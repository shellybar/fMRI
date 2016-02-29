package ubongo.integration;

import ubongo.common.datatypes.*;
import ubongo.dispatcher.ExecutionProxy;
import ubongo.dispatcher.QueueManager;


/**
 * Created by Shelly on 20/02/2016.
 */
public class NetworkingTest {

    public static void main(String[] args) {
        ExecutionProxy executionProxy = ExecutionProxy.getInstance();
        QueueManager queueManager = new QueueManager();


        int id = 1;
        Machine machine = new Machine();
        machine.setPort(1234);
        String inputPath= "unit7Inputs";
        String outputPath = "unit7Outputs";
        Unit unit = new BaseUnit(7);


        Task taskToExec = new TaskImpl(id, unit, machine, TaskStatus.PROCESSING, inputPath, outputPath);
        executionProxy.execute(taskToExec,queueManager);

    }
}
