package org.votingsystem.web.timestamp.jaxrs.exception;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MainExceptionMapper implements ExceptionMapper<NotFoundException> {

    public Response toResponse(NotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND).entity(
                "ExceptionMapper: NOT_FOUND - " + exception.getMessage()).type(MediaType.TEXT_PLAIN).build();
    }

}