package ubongo.common.datatypes;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TaskImpl implements Task, Serializable{

    private int id;
    private Unit unit;
    private Machine machine;
    private TaskStatus status;
    private String inputPath;
    private String outputPath;

    public TaskImpl(int id, Unit unit, Machine machine, TaskStatus status, String inputPath, String outputPath) {
        this.id = id;
        this.unit = unit;
        this.machine = machine;
        this.status = status;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public TaskImpl() {
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    @Override
    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    @Override
    public TaskStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    @Override
    public int getNetworkPort(){
        String port = String.valueOf(this.getMachine().getPort()) +  String.valueOf(this.getId());
        return Integer.parseInt(port);
    }

}
