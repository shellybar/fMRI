package edu.tau.eng.neuroscience.mri.server;

import edu.tau.eng.neuroscience.mri.common.datatypes.*;
import edu.tau.eng.neuroscience.mri.common.log.Logger;
import edu.tau.eng.neuroscience.mri.common.log.LoggerManager;
import edu.tau.eng.neuroscience.mri.dispatcher.ExecutionProxy;
import edu.tau.eng.neuroscience.mri.dispatcher.ExecutionProxyImpl;

import java.util.List;

public class AnalysesServerImpl implements AnalysesServer {

    private static Logger logger = LoggerManager.getLogger(AnalysesServerImpl.class);

    public static void main(String[] args) {

        logger.info("Start...");

        Task task = new TaskImpl();
        task.setId(57982);

        Machine machine = new MachineImpl();
        machine.setIp("132.67.140.170");

        task.setMachine(machine);

        Unit unit = new UnitImpl();
        unit.setId(1);
        unit.setInputPath("testUnitInputs");
        task.setUnit(unit);

        ExecutionProxy executionProxy = ExecutionProxyImpl.getInstance();
        logger.info("Executing task: " + task.getId());
        executionProxy.execute(task);
    }

    @Override
    public void submitAnalysis(List<Integer> unitIds) {
        return;
    }
}
