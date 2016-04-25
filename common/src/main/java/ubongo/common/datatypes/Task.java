package ubongo.common.datatypes;

import java.io.Serializable;

public class Task implements Serializable{

    private int id;
    private int flowId;
    private int serialNumber;
    private Unit unit;
    private Machine machine;
    private TaskStatus status;
    private String inputPath;
    private String outputPath;

    public Task(int id, int flowId, int serialNumber, Unit unit, Machine machine,
                TaskStatus status, String inputPath, String outputPath) {
        this.id = id;
        this.flowId = flowId;
        this.serialNumber = serialNumber;
        this.unit = unit;
        this.machine = machine;
        this.status = status;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public Task() {
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public int getFlowId() {
        return flowId;
    }

    public void setFlowId(int flowId) {
        this.flowId = flowId;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }
}
