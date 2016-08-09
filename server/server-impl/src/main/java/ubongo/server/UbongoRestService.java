package ubongo.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ubongo.common.datatypes.FlowData;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;
import ubongo.persistence.PersistenceException;
import ubongo.server.errorhandling.UbongoHttpException;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.UnmarshalException;
import java.io.IOException;
import java.util.List;

@Path("api")
public final class UbongoRestService {

    // TODO add logger and log messages

    private static final boolean DEBUG = true; // TODO move to configuration
    private static final int DEFAULT_QUERY_LIMIT = 1000; // TODO move to configuration

    private AnalysesServer analysesServer;
    private static final String FAILURE_MSG =
            "The AnalysesServer failed to start. Please check the server's configuration and restart it.";

    @GET
    @Path("machines")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllMachines() throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        ObjectMapper mapper = new ObjectMapper();
        String response;
        try {
            List<Machine> machines = analysesServer.getAllMachines();
            if (machines == null) {
                throw new UbongoHttpException(500, "Failed to retrieve machines.");
            }
            response = mapper.writeValueAsString(machines);
        } catch (JsonProcessingException e) {
            throw new UbongoHttpException(500, "Failed to serialize machines to JSON.");
        }
        return response;
    }

    @GET
    @Path("flows")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllFlows(@QueryParam("limit") int limit) throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        ObjectMapper mapper = new ObjectMapper();
        String response;
        try {
            List<FlowData> flows = analysesServer.getAllFlows(limit > 0 ? limit : DEFAULT_QUERY_LIMIT);
            if (flows == null) {
                throw new UbongoHttpException(500, "Failed to retrieve flows from DB.");
            }
            response = mapper.writeValueAsString(flows);
        } catch (JsonProcessingException e) {
            throw new UbongoHttpException(500, "Failed to serialize flows to JSON.");
        } catch (PersistenceException e) {
            throw new UbongoHttpException(500, "Failed to read flows from DB. Details: " + e.getMessage());
        }
        return response;
    }

    @POST
    @Path("flows")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createFlow(String requestBody) throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        int flow;
        String studyName;
        List<Task> tasks;
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(requestBody);
            if (jsonNode.hasNonNull("studyName")) {
                studyName = jsonNode.get("studyName").asText();
            } else {
                throw new UbongoHttpException(400, "Study name for flow cannot be empty nor null.");
            }
            if (jsonNode.hasNonNull("tasks")) {
                tasks = mapper.readValue(jsonNode.get("tasks").toString(),
                        new TypeReference<List<Task>>(){});
            } else {
                throw new UbongoHttpException(400, "Flow must contain at least one task.");
            }
            flow = analysesServer.createFlow(studyName, tasks);
        } catch (IOException e) {
            throw new UbongoHttpException(500, "Failed to deserialize JSON to FlowData.");
        } catch (PersistenceException e) {
            throw new UbongoHttpException(500, "Failed to create flow in DB. Details: " + e.getMessage());
        }
        return "{\"flowId\": \"" + flow + "\"}";
    }

    @POST
    @Path("flows/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    public void performActionOnFlow(@PathParam("flowId") int flowId,
                                    @NotNull @QueryParam("action") ResourceAction action)
            throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        switch (action) {
            case CANCEL:
                try {
                    analysesServer.cancelFlow(flowId);
                } catch (PersistenceException e) {
                    throw new UbongoHttpException(500, "Failed to cancel flow. Details: " + e.getMessage());
                }
                break;
            case RUN:
                try {
                    analysesServer.runFlow(flowId);
                } catch (PersistenceException e) {
                    throw new UbongoHttpException(500, "Failed to run flow. Details: " + e.getMessage());
                }
                break;
            default:
                throw new UbongoHttpException(405, "Unsupported action on flow (" + action.name() + ").");
        }
    }

    @GET
    @Path("flows/all/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllTasks(@QueryParam("limit") int limit) throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        ObjectMapper mapper = new ObjectMapper();
        String response;
        try {
            List<Task> tasks = analysesServer.getAllTasks(limit > 0 ? limit : DEFAULT_QUERY_LIMIT);
            if (tasks == null) {
                throw new UbongoHttpException(500, "Failed to retrieve tasks from DB.");
            }
            response = mapper.writeValueAsString(tasks);
        } catch (PersistenceException e) {
            throw new UbongoHttpException(500, "Failed to read tasks from DB. Details: " + e.getMessage());
        } catch (JsonProcessingException e) {
            throw new UbongoHttpException(500, "Failed to serialize tasks to JSON.");
        }
        return response;
    }

    @GET
    @Path("flows/{flowId}/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTasks(@DefaultValue("-1") @PathParam("flowId") int flowId) throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        if (flowId < 0) {
            throw new UbongoHttpException(400, "Flow ID must be a positive integer.");
        }
        ObjectMapper mapper = new ObjectMapper();
        String response;
        try {
            List<Task> tasks = analysesServer.getTasks(flowId);
            if (tasks == null) {
                throw new UbongoHttpException(500, "Failed to retrieve tasks.");
            }
            response = mapper.writeValueAsString(tasks);
        }
        catch (JsonProcessingException e) {
            throw new UbongoHttpException(500, "Failed to serialize tasks to JSON.");
        } catch (PersistenceException e) {
            throw new UbongoHttpException(500, "Failed to read tasks from DB. Details: " + e.getMessage());
        }
        return response;
    }

    @POST
    @Path("flows/{flowId}/tasks/{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    public void performActionOnTask(@PathParam("flowId") int flowId,
                                      @PathParam("taskId") int taskId,
                                      @QueryParam("action") ResourceAction action)
            throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        Task task;
        try {
            task = analysesServer.getTask(taskId);
        } catch (PersistenceException e) {
            throw new UbongoHttpException(500, "Failed to retrieve task from DB. Details: " + e.getMessage());
        }
        if (task.getFlowId() != flowId) {
            throw new UbongoHttpException(400, "The taskId does not match the flowId.");
        }
        switch (action) {
            case CANCEL:
                try {
                    analysesServer.cancelTask(task);
                } catch (PersistenceException e) {
                    throw new UbongoHttpException(500, "Failed to cancel tasks. Details: " + e.getMessage());
                }
                break;
            case RESUME:
                try {
                    analysesServer.resumeTask(task);
                } catch (PersistenceException e) {
                    throw new UbongoHttpException(500, "Failed to resume task. Details: " + e.getMessage());
                }
                break;
            case STOP:
                analysesServer.killTask(task);
                break;
            default:
                throw new UbongoHttpException(405, "Unsupported action on flow (" + action.name() + ").");
        }
    }

    @GET
    @Path("flows/{flowId}/tasks/{taskId}/logs")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTaskLogs(@PathParam("flowId") int flowId,
                              @PathParam("taskId") int taskId) throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        ObjectMapper mapper = new ObjectMapper();
        String response;
        try {
            List<String> logs = analysesServer.showTaskLogs(taskId);
            if (logs == null) {
                throw new UbongoHttpException(500, "Failed to retrieve task logs.");
            }
            response = mapper.writeValueAsString(logs);
        }
        catch (JsonProcessingException e) {
            throw new UbongoHttpException(500, "Failed to serialize logs to JSON.");
        }
        return response;
    }

    @GET
    @Path("log")
    @Produces(MediaType.APPLICATION_JSON)
    public String getServerLog() throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        ObjectMapper mapper = new ObjectMapper();
        String response;
        try {
            List<String> logs = analysesServer.showServerLog();
            if (logs == null) {
                throw new UbongoHttpException(500, "Failed to retrieve server log.");
            }
            response = mapper.writeValueAsString(logs);
        }
        catch (JsonProcessingException e) {
            throw new UbongoHttpException(500, "Failed to serialize server log to JSON.");
        }
        return response;
    }

    @GET
    @Path("units")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllUnits() throws UbongoHttpException {
        if (analysesServer == null) {
            throw new UbongoHttpException(500, FAILURE_MSG);
        }
        ObjectMapper mapper = new ObjectMapper();
        String response;
        try {
            List<Unit> units = analysesServer.getAllUnits();
            if (units == null) {
                throw new UbongoHttpException(500, "Failed to retrieve units.");
            }
            response = mapper.writeValueAsString(units);
        } catch (PersistenceException e) {
            throw new UbongoHttpException(500, "One or more units could not be read. Details: " + e.getMessage());
        } catch (JsonProcessingException e) {
            throw new UbongoHttpException(500, "Failed to serialize units to JSON.");
        }
        return response;
    }

    /* ---------------------------------------------------------------------------------- */

    private static final String CONFIG_PATH = "config";
    private static final String UNITS_DIR_PATH = "units_path";

    public UbongoRestService() {
        // initialize analysis server
        String configPath = System.getProperty(CONFIG_PATH);
        String unitsDirPath = System.getProperty(UNITS_DIR_PATH);
        if (validateSystemVariables(configPath, unitsDirPath)) return;
        Configuration configuration;
        try {
            configuration = Configuration.loadConfiguration(configPath);
            analysesServer = new AnalysesServerImpl(configuration, unitsDirPath, DEBUG);
        } catch (UnmarshalException e) {
            analysesServer = null;
        }
    }

    private static boolean validateSystemVariables(String configPath, String unitsDirPath) {
        if (configPath == null || unitsDirPath == null) {
            String pattern = "Please supply %1$s path as run parameter: -%2$s=<path>\n";
            if (configPath == null) {
                System.out.format(pattern, "configuration", CONFIG_PATH);
            }
            if (unitsDirPath == null) {
                System.out.format(pattern, "units directory", UNITS_DIR_PATH);
            }
            return true;
        }
        return false;
    }

}
