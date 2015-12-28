package edu.tau.eng.neuroscience.mri.common.datatypes;


public class MachineImpl implements Machine {
    private String ip; /* TODO maybe create class for IP */

    @Override
    public MachineStatistics getMachineStatistics(int id) {
        return null;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
