package edu.tau.eng.neuroscience.mri.common.datatypes;

/**
 * Unit is the basic fMRI service that runs on a machine.
 * It contains all the information required to run an fMRI script/program.
 */
public interface Unit {

    /**
     * a unique identifier of the unit as saved in the configuration files or DB.
     * @return the unit's id.
     */
    int getId();

}
