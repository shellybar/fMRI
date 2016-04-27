package ubongo.execution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.TaskStatus;
import ubongo.execution.exceptions.MachinesManagementException;
import ubongo.execution.exceptions.QueueManagementException;
import ubongo.persistence.Persistence;
import ubongo.persistence.PersistenceException;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO instead of notifyFatal make the executionImpl listen on some object
public class QueueManager {

    // max time (in milliseconds) the producer thread awaits to be notified of new tasks.
    // After this amount of time, it will check if there are new tasks in the DB.
    private static final int MAX_PRODUCER_IDLE_TIME = 1000 * 60 * 30; // 30 minutes
    private static final int MAX_QUEUE_CAPACITY = 100;
    private static final int NUM_CONSUMER_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors());

    private static Logger logger = LogManager.getLogger(QueueManager.class);
    private final Object consumerLock = new Object();
    private final Object producerLock = new Object();
    private boolean producerMayWork = false; // lets the producer know whether they may work or not
    private boolean producerUpdatingDatabase = false; // lets the consumers know they need to wait

    private BlockingQueue<Task> queue = new ArrayBlockingQueue<>(MAX_QUEUE_CAPACITY, true);
    private Persistence persistence;
    private ExecutionProxy executionProxy;
    private MachinesManager machinesManager;
    private ExecutorService consumers;
    private ExecutorService producer;

    // dependency verification logic
    private Map<Set<Integer>, Set<Task>> dependencyMap;
    private Map<TaskKey, Set<Integer>> setLocatorMap;
    private final Object taskDependencyLock = new Object();
    private boolean updatingDependencies;

    QueueManager(Persistence persistence, MachinesManager machinesManager) {
        this.executionProxy = ExecutionProxy.getInstance();
        this.persistence = persistence;
        this.machinesManager = machinesManager;
        this.setLocatorMap = new HashMap<>();
        this.dependencyMap = new HashMap<>();
    }

    public void start() throws QueueManagementException {
        initQueue();
    }

    private void initQueue() {
        consumers = Executors.newFixedThreadPool(NUM_CONSUMER_THREADS);
        for (int i = 0; i < NUM_CONSUMER_THREADS; i++)
            consumers.execute(new Consumer(queue, persistence));
        logger.debug("Created " + NUM_CONSUMER_THREADS + " queue consumer thread"
                + ((NUM_CONSUMER_THREADS > 1)?"s":""));
        producer = Executors.newSingleThreadExecutor();
        producer.execute(new Producer(queue, persistence));
        logger.debug("Created queue producer thread");
    }

    public void startFlow(int flowId) throws PersistenceException {

        // start flow in DB and notify producer thread
        synchronized(producerLock) {
            producerMayWork = false;
        }
        persistence.startFlow(flowId);
        synchronized(producerLock) {
            producerMayWork = true;
            producerLock.notify();
        }
    }

    public void stop() {
        producer.shutdownNow();
        consumers.shutdownNow();
    }

    /**
     * called by the execution proxy
     */
    synchronized public void updateTaskAfterExecution(Task task) {
        try {
            if (task.getStatus() == TaskStatus.COMPLETED) {
                synchronized (taskDependencyLock) {
                    while (updatingDependencies) {
                        taskDependencyLock.wait();
                    }
                    updatingDependencies = true;
                    handleCompletedTask(task);
                    updatingDependencies = false;
                }
            }
            persistence.updateTaskStatus(task);
        } catch (PersistenceException | InterruptedException e) {
            logger.fatal("Failed to update task in DB");
            ExecutionImpl.notifyFatal(e); // TODO what now?
        }
    }

    synchronized private void handleCompletedTask(Task task) throws PersistenceException {
        TaskKey key = new TaskKey(task);
        Set<Integer> taskIdsMap = setLocatorMap.get(key);
        if (taskIdsMap != null) {
            taskIdsMap.remove(task.getId());
            if (taskIdsMap.isEmpty()) {
                Set<Task> waitingTasks = dependencyMap.get(taskIdsMap);
                // send waitingTasks back to DB as NEW so they will be retrieved by the queue
                // and next time it will be able to run them (this allows orderly dependency verification)
                waitingTasks.forEach(t -> t.setStatus(TaskStatus.NEW));
                persistence.updateTasksStatus(waitingTasks);
                dependencyMap.remove(taskIdsMap);
                setLocatorMap.remove(key);
            }
        }
    }

    private class Consumer extends Thread {

        BlockingQueue<Task> queue;
        Persistence persistence;

        public Consumer(BlockingQueue<Task> queue, Persistence persistence) {
            this.queue = queue;
            this.persistence = persistence;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    logger.debug("Waiting to take task from queue");
                    Task task = queue.take();
                    Task currTask = (Task) task.clone();
                    synchronized (consumerLock) {
                        while (producerUpdatingDatabase) {
                            consumerLock.wait();
                        }
                        logger.debug("Retrieved task from queue: taskId=" + currTask.getId()
                                + ", unitId=" + currTask.getUnit().getId());
                        if (!taskReadyForExecute(currTask)) {
                            continue;
                        }
                    }
                    Machine machine = machinesManager.getAvailableMachine();
                    currTask.setMachine(machine);
                    logger.debug("Allocated machine (machineId=" + machine.getId()
                            + ") for task (taskId=" + currTask.getId() + ")");
                    logger.debug("Updating task's status in DB to 'Processing' (taskId=" + currTask.getId() + ")");
                    currTask.setStatus(TaskStatus.PROCESSING);
                    persistence.updateTaskStatus(currTask);
                    logger.debug("Sending task for execution (taskId=" + currTask.getId() + ")");
                    executionProxy.execute(currTask, QueueManager.this);
                }
            } catch (InterruptedException e) {
                logger.debug("Queue consumer thread shutting down after interrupt");
            } catch (MachinesManagementException e) {
                logger.fatal("Queue consumer thread failed to find available machine to run task. Details: " + e.getMessage());
                logger.fatal(e.getMessage());
                ExecutionImpl.notifyFatal(e); // TODO what now?
            } catch (PersistenceException e) {
                logger.fatal("Failed to update task in DB");
                ExecutionImpl.notifyFatal(e); // TODO what now?
            } catch (CloneNotSupportedException e) {
                // not really possible because clone is supported for task
            }
        }

        synchronized private boolean taskReadyForExecute(Task task)
                throws InterruptedException, PersistenceException {
            boolean ready;
            int serial = task.getSerialNumber() - 1;
            if (serial < 0) {
                return true; // there are no predecessors for this task
            }
            synchronized (taskDependencyLock) {
                while (updatingDependencies) {
                    taskDependencyLock.wait();
                }
                updatingDependencies = true;
                // get a list of uncompleted tasks (may be failed/stopped...) with serial smaller by one
                Stream<Task> taskStream = persistence.getTasks(task.getFlowId()).stream()
                        .filter(t -> t.getSerialNumber() == serial &&
                                t.getStatus() != TaskStatus.COMPLETED);
                if (!(ready = taskStream.count() == 0)) { // there are some dependencies
                    storeDependencies(task, taskStream);
                    // after storing, we need to verify no dependent was completed in the meanwhile
                    // if it were, check if we can run now (return false anyway)
                    Stream<Task> completedTaskStream =
                            persistence.getTasks(task.getFlowId()).stream()
                            .filter(t -> t.getSerialNumber() == serial &&
                                    t.getStatus() == TaskStatus.COMPLETED);
                    completedTaskStream.forEach(QueueManager.this::handleCompletedTask); // TODO continue...
                }
                updatingDependencies = false;
            }
            return ready;
        }

        synchronized private void storeDependencies(Task task, Stream<Task> taskStream) {
            TaskKey key = new TaskKey(taskStream.findAny().get()); // representative of set
            Set<Integer> taskIds;
            boolean inserted = false;
            // do we already have tasks depending on retrieved tasks?
            if (setLocatorMap.containsKey(key)) {
                taskIds = setLocatorMap.get(key);
                if (taskIds == null) {
                    setLocatorMap.remove(key); // false alarm
                } else {
                    Set<Task> dependents = dependencyMap.get(taskIds);
                    if (dependents == null) { // false alarm
                        setLocatorMap.remove(key);
                        dependencyMap.remove(taskIds);
                    } else { // this is not the first task depending on retrieved tasks
                        dependents.add(task);
                        inserted = true;
                    }
                }
            }
            if (!inserted) {
                taskIds = taskStream.map(Task::getId).collect(Collectors.toSet());
                setLocatorMap.put(key, taskIds);
                Set<Task> taskSet = new HashSet<>();
                taskSet.add(task);
                dependencyMap.put(taskIds, taskSet);
            }
        }
    }

    private class Producer extends Thread {

        BlockingQueue<Task> queue;
        Persistence persistence;

        public Producer(BlockingQueue<Task> queue, Persistence persistence) {
            this.queue = queue;
            this.persistence = persistence;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    synchronized (producerLock) {
                        while (!producerMayWork) {
                            producerLock.wait(MAX_PRODUCER_IDLE_TIME);
                        }
                    }
                    List<Task> tasks;
                    try {
                        tasks = persistence.getNewTasks();
                    } catch (PersistenceException e) {
                        continue;
                    }
                    synchronized(producerLock) {
                        if (tasks == null || tasks.isEmpty()) {
                            producerMayWork = false;
                            continue;
                        }
                    }
                    for (Task task: tasks) {
                        Task currTask = (Task) task.clone();
                        logger.debug("Adding new task to queue (taskId=" + currTask.getId() + ")");
                        synchronized (consumerLock) {
                            producerUpdatingDatabase = true;
                        }
                        boolean taskInsertedToQueue = queue.offer(currTask);
                        if (!taskInsertedToQueue) {
                            synchronized (consumerLock) {
                                producerUpdatingDatabase = false;
                                consumerLock.notifyAll();
                            }
                            queue.put(currTask);
                            synchronized (consumerLock) {
                                producerUpdatingDatabase = true;
                            }
                        }
                        currTask.setStatus(TaskStatus.PENDING);
                        logger.debug("Updating task's status in DB to 'Pending' (taskId=" + currTask.getId() + ")");
                        persistence.updateTaskStatus(currTask);
                        synchronized (consumerLock) {
                            producerUpdatingDatabase = false;
                            consumerLock.notifyAll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Queue producer thread shutting down after interrupt");
            } catch (PersistenceException e) {
                // TODO handle PersistenceException in queue producer
            } catch (CloneNotSupportedException e) {
                // not really possible because clone is supported for task
            }
        }
    }

    // used to retrieve set of Tasks with Task (a sort of Union-Find data-type)
    private static final class TaskKey {

        private int flowId;
        private int serial;

        public TaskKey(Task task) {
            flowId = task.getFlowId();
            serial = task.getSerialNumber();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof TaskKey)) {
                return false;
            }
            TaskKey other = (TaskKey) o;
            return this.flowId != other.flowId || this.serial != other.serial;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = hash * 31 + flowId;
            hash = hash * 31 + serial;
            return hash;
        }

    }
}
