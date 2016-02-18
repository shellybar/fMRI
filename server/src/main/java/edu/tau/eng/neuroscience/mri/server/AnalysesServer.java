package edu.tau.eng.neuroscience.mri.server;

import edu.tau.eng.neuroscience.mri.common.datatypes.*;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.dispatcher.*;

import java.util.List;
import java.util.Properties;

public class AnalysesServer {

    private static Logger logger = LoggerManager.getLogger(AnalysesServer.class);

    public static void main(String[] args) {

        logger.info("Start...");
        Properties dispatcherProps = new Properties();
        // TODO get props from args
        Dispatcher dispatcher = new DispatcherImpl(dispatcherProps);

        Task task = new TaskImpl();
        task.setId(57982);

        Machine machine = new Machine();
        machine.setAddress("132.67.140.170");

        task.setMachine(machine);

        Unit unit = new BaseUnit();
        unit.setId(1);
        unit.setInputPath("testUnitInputs");
        task.setUnit(unit);

        // TODO move execution proxy to the dispatcher - the AnalysesServer should only know the dispatcher
        ExecutionProxy executionProxy = ExecutionProxy.getInstance();
        logger.info("Executing task: " + task.getId());
        executionProxy.execute(task, null); // TODO notice you must have a QueueManager at this point
    }

    public void submitAnalysis(List<Integer> unitIds) {
        return;
    }

}
