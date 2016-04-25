package ubongo.execution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.constants.MachineConstants;
import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.MachineStatistics;
import ubongo.common.datatypes.RabbitData;
import ubongo.common.datatypes.Task;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

public enum ExecutionProxy {

    INSTANCE; // This is a singleton

    public static ExecutionProxy getInstance() {
        return INSTANCE;
    }

    private static Logger logger = LogManager.getLogger(ExecutionProxy.class);
    private String inputDirPath;
    private QueueManager queueManager;

    /**
     * @param task to execute
     * @param queueManager to send the task back after execution
     */
    public void execute(Task task, QueueManager queueManager) {
        this.queueManager = queueManager;
        int returnCode = executeTaskOnTheMachine(task);
        // TODO continue - check return code etc. update queue manager

        // TODO move this part of the code to the method called by the client after execution (it's here only to test the dispatcher)
        //task.setStatus(TaskStatus.COMPLETED);
        //queueManager.updateTaskAfterExecution(task);
    }

    public void killTask(Task task) {
        // TODO task.getMachine() and then send a command to the machine to kill the task
    }

    public MachineStatistics getStatistics(Machine machine) {
        return new MachineStatistics(null); // TODO
    }

    private int executeTaskOnTheMachine(Task task) {
        logger.info("Sending request to run unit on the machine. Task id = [" + task.getId() + "]");
        final String QUEUE_NAME =  SystemConstants.UBONGO_RABBIT_QUEUE;
        int returnCode = MachineConstants.BASE_UNIT_FAILURE;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(task.getMachine().getAddress());
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            RabbitData message = new RabbitData(task, MachineConstants.BASE_UNIT_REQUEST);
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            logger.debug(" [x] Sent '" + message.getMessage() + "'");
            channel.close();
            connection.close();
            returnCode = MachineConstants.BASE_UNIT_COMPLETED;
        } catch (Exception e){
            logger.error("Failed sending task to machine. Task id = [" + task.getId() + "] Machine = [" + task.getMachine().getAddress() + "] error: " + e.getMessage());
        }
        return returnCode;
    }

}