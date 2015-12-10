package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.datatypes.Task;
import edu.tau.eng.neuroscience.mri.common.datatypes.Unit;

import java.util.List;

/**
 * Manages the queue of tasks to run. This module receives units to run,
 * couples them with machines and creates tasks to be executed.
 * When a unit is ready to be executed, it is assigned a machine and
 * the QueueManager sends this task to the ExecutionProxy for execution.
 */
public interface QueueManager {

    /**
     * Receives a verified execution flow consisted of units to run and enqueues them
     * for future execution. Some may be dequeued and run immediately after being added
     * to the queue, while others may be persisted in the DB and await their turn
     * (when all prerequisites are met and there is a available machine to run on.
     * @param units to run
     */
    void enqueue(List<Unit> units);

    /**
     * Called by the ExecutionProxy when a task has been completed (successfully or unsuccessfully).
     * The QueueManager may try to send another execution request if the execution
     * was not completed successfully. In any case, this method updates the DB with the
     * new task details.
     * @param task to update status in the DB after it was executed.
     */
    void updateTaskStatus(Task task); //TODO add execution response object (with return-code and message)

}
