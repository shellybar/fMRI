package ubongo.integration;

import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.TaskStatus;
import ubongo.execution.Execution;
import ubongo.execution.ExecutionImpl;
import ubongo.persistence.PersistenceException;

import java.util.List;

public class ExecutionTest {

    private static Execution execution;
    private static PersistenceTest persistenceTest;

    public static void main(String[] args)
            throws Exception {
        persistenceTest = new PersistenceTest();
        if (!persistenceTest.init()) {
            return;
        }
        init();
        testExecutionDependencies();
        close();
    }

    private static void init() {
        execution = new ExecutionImpl(persistenceTest.persistence, persistenceTest.configuration.getMachines());
        execution.start();
    }

    private static void close() throws PersistenceException {
        execution.stop();
        persistenceTest.close();
    }

    private static void testExecutionDependencies() throws Exception {

        long failed, pending;
        boolean bad;
        int flowId = persistenceTest.createFlow(persistenceTest.prepareUnits());
        execution.runFlow(flowId); // kicks start queue into action

        // at this point tasks with serial number 0 should be 'Failed' and serial number 1 should be 'Pending'
        List<Task> tasks;
        int iterations = 0;
        do {
            Thread.sleep((++iterations) * 200);
            tasks = persistenceTest.persistence.getTasks(flowId);
            failed = getTaskCount(tasks, 0, TaskStatus.FAILED);
            pending = getTaskCount(tasks, 1, TaskStatus.PENDING);
        } while ((bad = (failed != 2 || pending != 4)) && iterations < 5);
        assert !bad;
    }

    private static long getTaskCount(List<Task> tasks, int serial, TaskStatus status) {
        return tasks.stream().filter(t -> t.getSerialNumber() == serial
                && t.getStatus() == status).count();
    }

}
