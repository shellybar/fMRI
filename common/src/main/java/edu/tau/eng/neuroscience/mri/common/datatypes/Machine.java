package edu.tau.eng.neuroscience.mri.common.datatypes;

import javax.xml.bind.annotation.*;

/**
 * Machine holds all required information regarding the physical/virtual machine
 * on which units are running.
 */
@XmlRootElement(name = "machine")
@XmlAccessorType(XmlAccessType.FIELD)
public class Machine {

    @XmlAttribute private int id;
    @XmlElement private String address;
    @XmlElement private int port;
    @XmlElement private MachineStatistics machineStatistics;

    public MachineStatistics getMachineStatistics() {
        return machineStatistics;
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public void setIds(int id) {
        this.id = id;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setStatistics(MachineStatistics machineStatistics) {
        this.machineStatistics = machineStatistics;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
