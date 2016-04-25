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

import java.util.List;
import java.util.concurrent.*;

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

    QueueManager(Persistence persistence, MachinesManager machinesManager) {
        this.executionProxy = ExecutionProxy.getInstance();
        this.persistence = persistence;
        this.machinesManager = machinesManager;
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

    public void stop() {
        producer.shutdownNow();
        consumers.shutdownNow();
    }

    /**
     * called by the execution proxy
     */
    public void updateTaskAfterExecution(Task task) {
        try {
            persistence.updateTaskStatus(task);
        } catch (PersistenceException e) {
            logger.fatal("Failed to update task in DB");
            ExecutionImpl.notifyFatal(e); // TODO what now?
        }
    }

    private synchronized static Task shallowCopyTask(Task task) {
        Task copied = new Task();
        copied.setId(task.getId());
        copied.setMachine(task.getMachine());
        copied.setStatus(task.getStatus());
        copied.setUnit(task.getUnit());
        return copied;
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
                    Task currTask = shallowCopyTask(task);
                    synchronized (consumerLock) {
                        while (producerUpdatingDatabase) {
                            consumerLock.wait();
                        }
                    }
                    logger.debug("Retrieved task from queue: taskId=" + currTask.getId()
                            + ", unitId=" + currTask.getUnit().getId());
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
                    List<Task> tasks = null;
                    try {
                        tasks = persistence.getNewTasks();
                    } catch (PersistenceException e) {
                        // TODO this is bad but not horrible because it just means no tasks will be processed -
                        // but they will also stay safe in the DB and we won't get an inconsistent situation
                    }
                    synchronized(producerLock) {
                        if (tasks == null || tasks.isEmpty()) {
                            producerMayWork = false;
                            continue;
                        }
                    }
                    for (Task task: tasks) {
                        Task currTask = shallowCopyTask(task);
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
                // TODO
            }
        }
    }
}
