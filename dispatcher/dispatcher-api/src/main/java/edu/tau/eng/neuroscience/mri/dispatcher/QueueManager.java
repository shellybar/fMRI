package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.Task;
import edu.tau.eng.neuroscience.mri.common.Unit;
import java.util.List;

/**
 * Created by regevr on 11/1/2015.
 */
public interface QueueManager {

    void enqueue(List<Unit> units);
    void updateTaskStatus(Task task); //TODO add exection response object (with return-code and message)

}
