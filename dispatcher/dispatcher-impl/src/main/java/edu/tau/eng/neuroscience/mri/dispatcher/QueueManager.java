package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.datatypes.*;
import edu.tau.eng.neuroscience.mri.common.exceptions.ErrorCodes;
import edu.tau.eng.neuroscience.mri.common.exceptions.MachinesManagementException;
import edu.tau.eng.neuroscience.mri.common.exceptions.QueueManagementException;
import edu.tau.eng.neuroscience.mri.common.exceptions.UnitFetcherException;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxy;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxyException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class QueueManager {

    // max time (in milliseconds) the producer thread awaits to be notified of new tasks.
    // After this amount of time, it will check if there are new tasks in the DB.
    private static final int MAX_PRODUCER_IDLE_TIME = 1000 * 60 * 30; // 30 minutes
    private static final int MAX_QUEUE_CAPACITY = 100;
    private static final int NUM_CONSUMER_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors());

    private static Logger logger = LoggerManager.getLogger(QueueManager.class);
    private final Object consumerLock = new Object();
    private final Object producerLock = new Object();
    private boolean producerMayWork = false; // lets the producer know whether they may work or not
    private boolean producerUpdatingDatabase = false; // lets the consumers know they need to wait

    private BlockingQueue<Task> queue = new ArrayBlockingQueue<>(MAX_QUEUE_CAPACITY, true);
    private DBProxy dbProxy;
    private ExecutionProxy executionProxy;
    private MachinesManager machinesManager;
    private ExecutorService consumers;
    private ExecutorService producer;

    public QueueManager(DBProxy dbProxy) throws QueueManagementException, MachinesManagementException {
        this.executionProxy = ExecutionProxy.getInstance();
        this.dbProxy = dbProxy;
        try {
            this.machinesManager = new MachinesManager(executionProxy);
            dbProxy.connect();
        } catch (DBProxyException e) {
            throw new QueueManagementException(ErrorCodes.QUEUE_MANAGEMENT_CONNECTION_EXCEPTION, e.getMessage());
        }
        initQueue();
    }

    private void initQueue() {
        consumers = Executors.newFixedThreadPool(NUM_CONSUMER_THREADS);
        for (int i = 0; i < NUM_CONSUMER_THREADS; i++)
            consumers.execute(new Consumer(queue, dbProxy));
        logger.debug("Created " + NUM_CONSUMER_THREADS + " queue consumer thread"
                + ((NUM_CONSUMER_THREADS > 1)?"s":""));
        producer = Executors.newSingleThreadExecutor();
        producer.execute(new Producer(queue, dbProxy));
        logger.debug("Created queue producer thread");
    }

    public void shutdownNow() throws QueueManagementException {
        producer.shutdownNow();
        consumers.shutdownNow();
        try {
            dbProxy.disconnect();
        } catch (DBProxyException e) {
            throw new QueueManagementException(ErrorCodes.QUEUE_MANAGEMENT_CONNECTION_EXCEPTION, e.getMessage());
        }
    }

    public void enqueue(Unit unit) {
        List<Unit> units = new ArrayList<>();
        units.add(unit);
        enqueue(units);
    }

    public void enqueue(List<Unit> units) {
        // Convert list of units to list of tasks
        List<Task> tasks = units.stream()
                .map((unit) -> {
                    TaskImpl task = new TaskImpl();
                    task.setUnit(unit);
                    task.setStatus(TaskStatus.NEW);
                    return task;
                }).collect(Collectors.<Task> toList());

        // add tasks to DB and notify producer thread
        synchronized(producerLock) {
            producerMayWork = false;
        }
        try {
            dbProxy.add(tasks);
        } catch (DBProxyException e) {
            logger.fatal("Failed to add tasks to DB");
            DispatcherImpl.notifyFatal(e); // TODO what now?
        }
        synchronized(producerLock) {
            producerMayWork = true;
            producerLock.notify();
        }
    }

    private synchronized static Task shallowCopyTask(Task task) {
        Task copied = new TaskImpl();
        copied.setId(task.getId());
        copied.setMachine(task.getMachine());
        copied.setStatus(task.getStatus());
        copied.setUnit(task.getUnit());
        return copied;
    }

    private class Consumer extends Thread {

        BlockingQueue<Task> queue;
        DBProxy dbProxy;

        public Consumer(BlockingQueue<Task> queue, DBProxy dbProxy) {
            this.queue = queue;
            this.dbProxy = dbProxy;
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
                    dbProxy.updateStatus(currTask);
                    logger.debug("Sending task for execution (taskId=" + currTask.getId() + ")");
                    executionProxy.execute(currTask);
                }
            } catch (InterruptedException e) {
                logger.debug("Queue consumer thread shutting down after interrupt");
            } catch (MachinesManagementException e) {
                logger.fatal("Queue consumer thread failed to find available machine to run task. Details: " + e.getMessage());
                logger.fatal(e.getMessage());
                DispatcherImpl.notifyFatal(e); // TODO what now?
            } catch (DBProxyException e) {
                logger.fatal("Failed to updateStatus task in DB");
                DispatcherImpl.notifyFatal(e); // TODO what now?
            }
        }
    }

    private class Producer extends Thread {

        BlockingQueue<Task> queue;
        DBProxy dbProxy;

        public Producer(BlockingQueue<Task> queue, DBProxy dbProxy) {
            this.queue = queue;
            this.dbProxy = dbProxy;
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
                        tasks = dbProxy.getNewTasks();
                    } catch (UnitFetcherException e) {
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
                        dbProxy.updateStatus(currTask);
                        synchronized (consumerLock) {
                            producerUpdatingDatabase = false;
                            consumerLock.notifyAll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Queue producer thread shutting down after interrupt");
            } catch (DBProxyException e) {
                // TODO
            }
        }
    }
}
