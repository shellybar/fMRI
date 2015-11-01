package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.Machine;

/**
 * Created by regevr on 11/1/2015.
 */
public interface MachinesManager {

    /**
     * @return Available Machine to run analysis
     */
    Machine getMachine();

}
