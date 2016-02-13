package edu.tau.eng.neuroscience.mri.common.datatypes;

/**
 * MachineStatistics encapsulated all performance metrics of a machine
 * (e.g., CPU average consumption).
 */
public class MachineStatistics {

    int numOfRunningTasks;
    float CPUconsumption;

    /**
     * returns a single score between 0 and 1, measuring the availability of a machine
     */
    public float getAvailabilityScore() {
        return 1; // TODO should be calculated based on different metrics
    }

}
