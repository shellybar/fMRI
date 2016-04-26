package ubongo.common.datatypes;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Task implements Serializable {

    private int id;
    private int flowId;
    private int serialNumber;
    private Unit unit;
    private Machine machine;
    private TaskStatus status;
    private Context context;

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{(.*?)\\}");
    private static final String WILDCARD = ".*";

    // TODO no need to get id and flowId as they will be overridden by ids in DB
    public Task(int serialNumber, Unit unit, Context context) {
        this(0, 0, serialNumber, unit, null, context, TaskStatus.CREATED);
    }

    public Task(int id, int flowId, int serialNumber, Unit unit,
                Machine machine, Context context, TaskStatus status) {
        this.id = id;
        this.flowId = flowId;
        this.serialNumber = serialNumber;
        this.unit = unit;
        this.machine = machine;
        this.context = context;
        this.status = status;
    }

    public Task() {}

    public String getInputPath() {
        return unit == null ? null : unit.getInputPaths();
    }

    public String getOutputPath() {
        return unit == null ? null : unit.getOutputDir();
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

    public static List<Task> createTasks(Unit unit, Context context, int serialNumber) throws Exception {
        List<Task> tasks = new ArrayList<>();
        try {
            createTasks(tasks, unit, context, serialNumber);
        } catch (IllegalArgumentException e) {
            throw new Exception(e); // so it will not be a Runtime exception and will have to be handled
        }
        return tasks;
    }

    private static void createTasks(List<Task> tasks, Unit originalUnit, Context originalContext, int serialNumber)
            throws IllegalArgumentException, IOException, CloneNotSupportedException {

        Unit unit = (Unit) originalUnit.clone();
        Context context = (Context) originalContext.clone();

        String inputPattern = unit.getInputPaths();
        String outputPattern = unit.getOutputDir();
        Matcher inputMatcher = VAR_PATTERN.matcher(inputPattern);
        String inputVar = inputMatcher.find() ? inputMatcher.group(1) : null;
        if (inputVar == null) { // stop condition
            // inputPattern is a context-less path
            Matcher outputMatcher = VAR_PATTERN.matcher(outputPattern);
            if (outputMatcher.find()) {
                throw new IllegalArgumentException(
                        "Some variable in the output directory of unit " + unit.getId() +
                        " is redundant and inconsistent with the amount of variables in the input paths");
            }
            Task task = new Task(serialNumber, unit, context);
            tasks.add(task);
        } else {
            // next line may throw IllegalArgumentException
            ContextLevel contextLevel = ContextLevel.valueOf(inputVar.toLowerCase());
            String inputVarEnclosed = "\\{" + inputVar + "\\}";
            String[] inputPathParts = inputPattern.split(inputVarEnclosed);
            String inputPrefix = inputPathParts[0];
            String inputSuffix = inputPathParts.length > 1 ? inputPathParts[1] : "";
            String contextPart = contextLevel.getStringFromContext(context);

            if (contextPart == null) {
                throw new IllegalArgumentException("Variable {" + inputVar + "} is not a valid variable name");
            }
            if (!contextPart.equals(WILDCARD)) {
                unit.setInputPaths(inputPrefix + contextPart + inputSuffix);
                unit.setOutputDir(outputPattern.replaceAll(inputVarEnclosed, contextPart));
                updateUnitParameters(unit, inputVarEnclosed, contextPart);
                createTasks(tasks, unit, context, serialNumber);
            } else {
                Path dir = Paths.get(inputPrefix);
                List<Path> subDirs = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                    for (Path entry : stream) {
                        if (Files.isDirectory(entry)) {
                            subDirs.add(entry);
                        }
                    }
                }
                for (Path subDir : subDirs) {
                    unit.setInputPaths(subDir.toString().replaceAll("\\\\", "/") + inputSuffix);
                    String value = subDir.getFileName().toString();
                    unit.setOutputDir(outputPattern.replaceAll(inputVarEnclosed, value));
                    updateUnitParameters(unit, inputVarEnclosed, value);
                    List<Task> furtherTasks = new ArrayList<>();
                    contextLevel.updateContext(context, value);
                    createTasks(furtherTasks, unit, context, serialNumber);
                    tasks.addAll(furtherTasks);
                }
            }
        }
    }

    private static void updateUnitParameters(Unit unit, String inputVarEnclosed, String value) {
        List<UnitParameter> unitParameters = unit.getParameters();
        for (UnitParameter unitParameter : unitParameters) {
            unitParameter.setValue(unitParameter.getValue()
                    .replaceAll(inputVarEnclosed, value));
        }
    }

    private enum ContextLevel {

        study ("study"), subject ("subject"), run ("run");

        private String name;

        ContextLevel(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getStringFromContext(Context context) {
            switch (this) {
                case study: return context.getStudy();
                case subject: return context.getSubject();
                case run: return context.getRun();
            }
            return "";
        }

        public void updateContext(Context context, String value) {
            switch (this) {
                case study: context.setStudy(value); break;
                case subject: context.setSubject(value); break;
                case run: context.setRun(value);
            }
        }
    }

}
