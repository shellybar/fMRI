package ubongo.common.datatypes;

import java.io.Serializable;

public class Task implements Serializable{

    private int id;
    private int flowId;
    private int serialNumber;
    private Unit unit;
    private Machine machine;
    private TaskStatus status;
    private Context context;
    private String inputPath;
    private String outputPath;

    public Task(int id, int flowId, int serialNumber, Unit unit, Machine machine,
                TaskStatus status, Context context) {
        this.id = id;
        this.flowId = flowId;
        this.serialNumber = serialNumber;
        this.unit = unit;
        this.machine = machine;
        this.status = status;
        this.setContext(context);
    }

    public Task() {}

    public String getInputPath() {
        if (inputPath == null) {
            // TODO compute from context params (subject, run...)
        }
        return inputPath;
    }

    public String getOutputPath() {
        if (outputPath == null) {
            // TODO compute from context params (subject, run...)
        }
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

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
