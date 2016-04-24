package ubongo.common.datatypes;

import java.io.Serializable;

public class Task implements Serializable{

    private long id;
    private long flowId; // TODO add to constructor + add getter/setter
    private Unit unit;
    private Machine machine;
    private TaskStatus status;
    private String inputPath;
    private String outputPath;

    public Task(long id, Unit unit, Machine machine, TaskStatus status, String inputPath, String outputPath) {
        this.id = id;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

}
