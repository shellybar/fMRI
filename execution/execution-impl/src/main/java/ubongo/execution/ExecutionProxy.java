package ubongo.execution;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.constants.MachineConstants;
import ubongo.common.constants.SystemConstants;
import ubongo.common.datatypes.*;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

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
        sendRequestToMachine(task, SystemConstants.UBONGO_RABBIT_TASKS_QUEUE ,MachineConstants.BASE_UNIT_REQUEST );
    }

    public void killTask(Task task) {
        sendRequestToMachine(task, SystemConstants.UBONGO_RABBIT_KILL_TASKS_QUEUE ,MachineConstants.KILL_TASK_REQUEST );
    }

    public MachineStatistics getStatistics(Machine machine) {
        return new MachineStatistics(null); // TODO
    }

    private void sendRequestToMachine(Task task, String queue, String request) {
        logger.info("Sending request to run unit on the machine. Task id = [" + task.getId() + "]");
        final String QUEUE_NAME =  queue;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(task.getMachine().getAddress());
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            RabbitData message = new RabbitData(task, request);
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            logger.debug(" [x] Sent '" + message.getMessage() + "'");
            channel.close();
            connection.close();
        } catch (Exception e){
            logger.error("Failed sending task to machine. Task id = [" + task.getId() + "] Machine = [" + task.getMachine().getAddress() + "] error: " + e.getMessage());
            task.setStatus(TaskStatus.FAILED);
            queueManager.updateTaskAfterExecution(task);
        }
    }

}