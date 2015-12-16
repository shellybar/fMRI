package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.datatypes.*;
import edu.tau.eng.neuroscience.mri.common.exceptions.ErrorCodes;
import edu.tau.eng.neuroscience.mri.common.exceptions.QueueManagementException;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

// TODO add unit tests and comments
public class QueueManagerImpl implements QueueManager {

    private static final int MAX_QUEUE_CAPACITY = 100;
    private static final int NUM_CONSUMER_THREADS = 1;
    private static final int NUM_PRODUCER_THREADS = 1;

    private static Logger logger = LoggerManager.getLogger(UnitFetcher.class);
    private final Object tasksToProcess = new Object();
    private boolean producerMayWork = false; // lets the producers know whether they may work or not

    private BlockingQueue<Task> queue = new ArrayBlockingQueue<>(MAX_QUEUE_CAPACITY, true);
    private DBProxy dbProxy;
    private ExecutionProxy executionProxy;
    private MachinesManager machinesManager;

    public QueueManagerImpl(DBProxy dbProxy) throws QueueManagementException {
        this.machinesManager = new MachinesManagerImpl();
        this.dbProxy = dbProxy;
        this.executionProxy = ExecutionProxyImpl.INSTANCE;
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
        }
        initQueue();
    }

    private void initQueue() {
        ExecutorService consumers = Executors.newFixedThreadPool(NUM_CONSUMER_THREADS);
        for (int i = 0; i < NUM_CONSUMER_THREADS; i++)
            consumers.execute(new Consumer(queue, dbProxy));
        ExecutorService producers = Executors.newFixedThreadPool(NUM_PRODUCER_THREADS);
        for (int i = 0; i < NUM_PRODUCER_THREADS; i++)
            producers.execute(new Producer(queue, dbProxy));
    }

    @Override
    public void enqueue(List<Unit> units) {
        // Convert list of units to list of tasks
        List<Task> tasks = units.stream()
                .map((Unit unit) -> {
                    TaskImpl task = new TaskImpl();
                    task.setUnit(unit);
                    task.setStatus(TaskStatus.NEW);
                    return task;
                }).collect(Collectors.<Task> toList());

        // add tasks to DB and notify a producer thread
        dbProxy.add(tasks);
        synchronized(tasksToProcess){
            producerMayWork = true;
            tasksToProcess.notify();
        }
    }

    @Override
    public void updateTaskStatus(Task task) {
        dbProxy.update(task);
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
                    Task task = queue.take();
                    Machine machine = machinesManager.getAvailableMachine();
                    task.setMachine(machine);
                    task.setStatus(TaskStatus.PROCESSING);
                    dbProxy.update(task);
                    executionProxy.execute(task);
                }
            } catch (InterruptedException e) {
                // Do nothing - consumer will simply terminate
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
                    synchronized (tasksToProcess) {
                        while (!QueueManagerImpl.this.producerMayWork) {
                            tasksToProcess.wait();
                        }
                        QueueManagerImpl.this.producerMayWork = false;
                    }
                    List<Task> tasks = dbProxy.getNewTasks();
                    for (Task task: tasks) {
                        queue.add(task);
                        task.setStatus(TaskStatus.PENDING);
                        dbProxy.update(task);
                    }
                }
            } catch (InterruptedException e) {
                // Do nothing - producer will simply terminate
            }
        }
    }
}
