package edu.tau.eng.neuroscience.mri.server;

import edu.tau.eng.neuroscience.mri.common.datatypes.*;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.dispatcher.*;

import java.util.List;

public class AnalysesServer {

    private static Logger logger = LoggerManager.getLogger(AnalysesServer.class);

    public static void main(String[] args) {

        logger.info("Start...");
//        DBProxy dbProxy = new DBProxy(""); // TODO add config file path
//        try {
//            QueueManager queueManager = new QueueManager(dbProxy);
//        } catch (QueueManagementException e) {
//            // TODO
//        }

        Task task = new TaskImpl();
        task.setId(57982);

        Machine machine = new Machine();
        machine.setAddress("132.67.140.170");

        task.setMachine(machine);

        Unit unit = new BaseUnit();
        unit.setId(1);
        unit.setInputPath("testUnitInputs");
        task.setUnit(unit);

        // TODO eventually, the server should "talk" to the dispatcher API and it will pass requests through the flow verificator, queue and all to the execution proxy
        ExecutionProxy executionProxy = ExecutionProxy.getInstance();
        logger.info("Executing task: " + task.getId());
        executionProxy.execute(task);
    }

    public void submitAnalysis(List<Integer> unitIds) {
        return;
    }

}
