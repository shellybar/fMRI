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

        long failed, pending, completed;
        boolean bad;
        int flowId = persistenceTest.createFlow(persistenceTest.prepareUnits());
        execution.runFlow(flowId); // kicks start queue into action

        // at this point tasks with serial number 0 should be 'Failed' and serial number 1 should be 'Pending'
        List<Task> tasks;
        int iterations = 0;
        do {
            Thread.sleep((++iterations) * 100);
            tasks = persistenceTest.persistence.getTasks(flowId);
            failed = getTaskCount(tasks, 0, TaskStatus.FAILED);
            pending = getTaskCount(tasks, 1, TaskStatus.PENDING);
        } while ((bad = (failed != 2 || pending != 4)) && iterations < 4);
        assert !bad;

        // now we set the failed tasks to completed and then assert the 'Pending' tasks are 'Failed'
        tasks = completeTaskAndGet(flowId, tasks);
        completeTaskAndGet(flowId, tasks);
        iterations = 0;
        do {
            Thread.sleep((++iterations) * 200);
            tasks = persistenceTest.persistence.getTasks(flowId);
            failed = getTaskCount(tasks, 0, TaskStatus.FAILED) + getTaskCount(tasks, 1, TaskStatus.FAILED);
            pending = getTaskCount(tasks, 1, TaskStatus.PENDING);
            completed = getTaskCount(tasks, 0, TaskStatus.COMPLETED);
        } while ((bad = (failed != 4 || pending != 0 || completed != 2)) && iterations < 4);
        assert !bad;
    }

    private static List<Task> completeTaskAndGet(int flowId, List<Task> tasks) throws InterruptedException, PersistenceException {
        Task task = tasks.stream().filter(t -> t.getSerialNumber() == 0
                && t.getStatus() == TaskStatus.FAILED).findFirst().get();
        task.setStatus(TaskStatus.COMPLETED);
        ((ExecutionImpl) execution).getQueueManager().updateTaskAfterExecution(task);
        tasks = persistenceTest.persistence.getTasks(flowId);
        return tasks;
    }

    private static long getTaskCount(List<Task> tasks, int serial, TaskStatus status) {
        return tasks.stream().filter(t -> t.getSerialNumber() == serial
                && t.getStatus() == status).count();
    }

}
