package ubongo.server.errorhandling;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UbongoExceptionMapper implements ExceptionMapper<UbongoHttpException> {

    @Override
    public Response toResponse(UbongoHttpException ex) {
        return Response.status(ex.getStatus())
                .entity(new ErrorMessage(ex).toJsonString())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
