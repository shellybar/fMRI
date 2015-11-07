package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.Machine;

/**
 * Manages machines' performance, availability, units persistence
 * and all other machine management tasks.
 */
public interface MachinesManager {

    /**
     * @return available Machine to run analysis
     */
    Machine getMachine();

}
