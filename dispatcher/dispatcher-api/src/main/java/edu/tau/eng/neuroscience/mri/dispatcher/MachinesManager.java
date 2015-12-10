package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.datatypes.Machine;

import java.util.List;

/**
 * Manages machines' performance, availability, units persistence
 * and all other machine management tasks.
 */
public interface MachinesManager {

    /**
     * @return available Machine to run analysis
     */
    Machine getAvailableMachine();

    /**
     * returns a List of all the Machines where the MachineStatistics are up-to-date.
     * This can be used to reflect machines' status to the user, etc.
     * @return List of all Machines registered in the system
     */
    List<Machine> getAllMachines();

}
