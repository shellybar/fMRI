package ubongo.server;

import ubongo.server.errorhandling.UbongoHttpException;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("api")
public final class UbongoRestService {

    @GET
    @Path("machines")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllMachines() throws UbongoHttpException {
        return "{}";
    }

    @GET
    @Path("flows")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllFlows(@QueryParam("limit") int limit) {
        return "{}";
    }

    @POST
    @Path("flows")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createFlow(String requestBody) {
        return "{}";
    }

    @POST
    @Path("flows/{flowId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String performActionOnFlow(@PathParam("flowId") int flowId,
                                      @QueryParam("action") ResourceAction action) {
        return "{}";
    }

    @GET
    @Path("flows/all/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllTasks(@QueryParam("limit") int limit) {
        return "{}";
    }

    @GET
    @Path("flows/{flowId}/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTasks(@PathParam("flowId") int flowId) {
        return "{}";
    }

    @POST
    @Path("flows/{flowId}/tasks/{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String performActionOnTask(@PathParam("flowId") int flowId,
                                      @PathParam("taskId") int taskId,
                                      @QueryParam("action") ResourceAction action) {
        return "{}";
    }

    @GET
    @Path("flows/{flowId}/tasks/{taskId}/logs")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTaskLogs(@PathParam("flowId") int flowId,
                              @PathParam("taskId") int taskId) {
        return "{}";
    }

    @GET
    @Path("log")
    @Produces(MediaType.APPLICATION_JSON)
    public String getServerLog() {
        return "{}";
    }

    @GET
    @Path("units")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllUnits() {
        return "{}";
    }

}
