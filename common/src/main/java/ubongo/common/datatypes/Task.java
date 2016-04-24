package ubongo.common.datatypes;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Task implements Serializable{

    private int id;
    private Unit unit;
    private Machine machine;
    private TaskStatus status;
    private String inputPath;
    private String outputPath;

    public Task(int id, Unit unit, Machine machine, TaskStatus status, String inputPath, String outputPath) {
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

}
