package edu.tau.eng.neuroscience.mri.dispatcher;

import com.jcraft.jsch.JSchException;
import edu.tau.eng.neuroscience.mri.common.datatypes.*;
import edu.tau.eng.neuroscience.mri.common.exceptions.ErrorCodes;
import edu.tau.eng.neuroscience.mri.common.exceptions.QueueManagementException;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class QueueManagerImpl implements QueueManager {

    // max time (in milliseconds) the producer thread awaits to be notified of new tasks.
    // After this amount of time, it will check if there are new tasks in the DB.
    private static final int MAX_PRODUCER_IDLE_TIME = 1000 * 60 * 30; // 30 minutes
    private static final int MAX_QUEUE_CAPACITY = 100;
    private static final int NUM_CONSUMER_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors());

    private static Logger logger = LoggerManager.getLogger(QueueManagerImpl.class);
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

    public QueueManagerImpl(DBProxy dbProxy) throws QueueManagementException {
        this.machinesManager = new MachinesManagerImpl();
        this.dbProxy = dbProxy;
        this.executionProxy = ExecutionProxyImpl.getInstance();
        try {
            dbProxy.connect();
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to connect to the database (url: %s; user: %s)",
                    dbProxy.getUrl(), dbProxy.getUser());
            logger.error(errorMsg +
                    "\nSQLException: " + e.getMessage() +
                    "\nSQLState: " + e.getSQLState() +
                    "\nVendorError: " + e.getErrorCode());
            throw new QueueManagementException(ErrorCodes.DB_CONNECTION_ERROR, errorMsg);
        } catch (JSchException e) {
            String errorMsg = "Failed to establish SSH connection to the database";
            throw new QueueManagementException(ErrorCodes.SSH_CONNECTION_ERROR, errorMsg);
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

    void shutdownNow() throws QueueManagementException {
        producer.shutdownNow();
        consumers.shutdownNow();
        try {
            dbProxy.disconnect();
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to disconnect from the database (url: %s; user: %s)",
                    dbProxy.getUrl(), dbProxy.getUser());
            logger.error(errorMsg +
                    "\nSQLException: " + e.getMessage() +
                    "\nSQLState: " + e.getSQLState() +
                    "\nVendorError: " + e.getErrorCode());
            throw new QueueManagementException(ErrorCodes.DB_CONNECTION_ERROR, errorMsg);
        }
    }

    @Override
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
        dbProxy.add(tasks);
        synchronized(producerLock) {
            producerMayWork = true;
            producerLock.notify();
        }
    }

    @Override
    public void updateTaskStatus(Task task) {
        dbProxy.update(task);
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
                    logger.debug("Updating task's status in DB to 'processing' (taskId=" + currTask.getId() + ")");
                    dbProxy.update(currTask);
                    logger.debug("Sending task for execution (taskId=" + currTask.getId() + ")");
                    executionProxy.execute(currTask);
                }
            } catch (InterruptedException e) {
                logger.debug("Queue consumer thread shutting down after interrupt");
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
                    List<Task> tasks = dbProxy.getNewTasks();
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
                        logger.debug("Updating task's status in DB to 'pending' (taskId=" + currTask.getId() + ")");
                        /* TODO If the status of the task in the DB is 'processing', do not change to pending
                         * (specifically in this case, since this is a new task).
                         * Implement this once the update functions are implemented */
                        dbProxy.update(currTask);
                        synchronized (consumerLock) {
                            producerUpdatingDatabase = false;
                            consumerLock.notifyAll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.debug("Queue producer thread shutting down after interrupt");
            }
        }
    }
}
