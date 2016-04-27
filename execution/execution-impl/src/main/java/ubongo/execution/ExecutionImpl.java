package ubongo.execution;

import com.rabbitmq.client.*;
import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.RabbitData;
import ubongo.common.datatypes.Task;
import ubongo.execution.exceptions.QueueManagementException;
import ubongo.persistence.Persistence;
import ubongo.persistence.PersistenceException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ExecutionImpl implements Execution {

    private Persistence persistence;
    private MachinesManager machinesManager;
    private QueueManager queueManager;
    private ExecutionProxy executionProxy;
    private boolean debug; // TODO currently not used!

    public ExecutionImpl(Persistence persistence, List<Machine> machines) {
        this(persistence, machines, false);
    }

    public ExecutionImpl(Persistence persistence, List<Machine> machines, boolean debug) {
        this.debug = debug;
        this.persistence = persistence;
        executionProxy = ExecutionProxy.getInstance();
        machinesManager = new MachinesManager(machines, executionProxy);
        queueManager = new QueueManager(persistence, machinesManager);
        try {
            tasksStatusListener();
        } catch (Exception e) {
            notifyFatal(e);
        }
    }

    @Override
    public void start() {
        try {
            machinesManager.start();
            queueManager.start();
        } catch (QueueManagementException e) {
            // TODO handle QueueManagementException on dispatcher start
        }
    }

    @Override
    public void stop() {
        queueManager.stop();
        machinesManager.stop();
    }

    @Override
    public void runFlow(int flowId) {
        try {
            queueManager.startFlow(flowId);
        } catch (PersistenceException e) {
            // TODO handle PersistenceException in runFlow
        }
    }

    @Override
    public void killTask(Task task) {
        executionProxy.killTask(task);
    }

    @Override
    public List<Machine> getAllMachines() {
        return machinesManager.getAllMachines();
    }

    /**
     * A call to this function notifies the Execution that a fatal error has occurred
     */
    protected static void notifyFatal(Throwable e) {
        // TODO notify UI, try to solve based on type of error...
    }

    private void tasksStatusListener() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queue = SystemConstants.UBONGO_SERVER_TASKS_STATUS_QUEUE;
        channel.queueDeclare(queue, false, false, false, null);
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try {
                    RabbitData message = RabbitData.fromBytes(body);
                    System.out.println(" [!] Received '" + message.getMessage() + "'");
                    Task task = message.getTask();
                    queueManager.updateTaskAfterExecution(task);
                } catch (Exception e){
                    throw new IOException(e);
                }
            }
        };
        channel.basicConsume(queue, true, consumer);
    }

    // used for testing but not part of API
    public QueueManager getQueueManager() {
        return queueManager;
    }

    public void notifyQueueBeforeCancel(Task task) {
        queueManager.aboutToCancel(task);
    }

    public void notifyQueueAfterCancel(Task task) {
        queueManager.cancelCompleted(task);
    }
}
