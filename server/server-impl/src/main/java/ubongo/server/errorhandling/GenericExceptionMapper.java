package ubongo.server.errorhandling;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {

        ErrorMessage errorMessage = new ErrorMessage();
        setHttpStatus(ex, errorMessage);
        errorMessage.setMessage(ex.getMessage());

        return Response.status(errorMessage.getStatus())
                .entity(errorMessage.toJsonString())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private void setHttpStatus(Throwable ex, ErrorMessage errorMessage) {
        if (ex instanceof WebApplicationException) {
            Response.StatusType statusType = ((WebApplicationException) ex).getResponse().getStatusInfo();
            errorMessage.setStatus(statusType.getStatusCode());
        } else {
            errorMessage.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }
}