package edu.tau.eng.neuroscience.mri.common.datatypes;

/**
 * Machine holds all required information regarding the physical/virtual machine
 * on which units are running.
 */
public interface Machine {

    MachineStatistics getMachineStatistics(int id);

    int getId();

}
