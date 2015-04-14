package org.votingsystem.web.jaxrs.exception;

import org.votingsystem.dto.MessageDto;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ExceptionMapperVS implements ExceptionMapper<Exception> {

    private static final Logger log = Logger.getLogger(ExceptionMapperVS.class.getSimpleName());

    @Override
    public Response toResponse(Exception exception) {
        try {
            if(exception instanceof ExceptionVS) {
                log.log(Level.SEVERE, "--- ExceptionVS --- " + exception.getMessage());
                if(((ExceptionVS) exception).getMessageDto() != null) {
                    MessageDto messageDto = ((ExceptionVS) exception).getMessageDto();
                    return Response.status(messageDto.getStatusCode()).entity(
                            JSON.getMapper().writeValueAsBytes(messageDto)).type(MediaTypeVS.JSON).build();
                } else return Response.status(Response.Status.BAD_REQUEST).entity(
                        "ExceptionVS:" + exception.getMessage()).type(MediaType.TEXT_PLAIN).build();
            }  else if(exception instanceof NotFoundException) {
                log.log(Level.SEVERE, "--- NotFoundException --- " + exception.getMessage());
                return Response.status(Response.Status.NOT_FOUND).entity(
                        "NotFoundException: " + exception.getMessage()).type(MediaType.TEXT_PLAIN).build();
            } else if(exception instanceof WebApplicationException) {
                log.log(Level.SEVERE, "--- WebApplicationException --- " + exception.getMessage(), exception);
                return Response.status(Response.Status.BAD_REQUEST).entity(
                        "WebApplicationException: " + exception.getMessage()).type(MediaType.TEXT_PLAIN).build();
            } else {
                log.log(Level.SEVERE, exception.getMessage(), exception);
                return Response.status(Response.Status.BAD_REQUEST).entity(
                        "Exception: " + exception.getMessage()).type(MediaType.TEXT_PLAIN).build();
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, exception.getMessage(), exception);
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    "Exception: " + ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }
    }
}