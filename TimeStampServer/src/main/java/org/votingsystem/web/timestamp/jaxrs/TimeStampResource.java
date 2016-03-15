package org.votingsystem.web.timestamp.jaxrs;

import org.votingsystem.model.TimeStamp;
import org.votingsystem.util.ContentType;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/timestamp")
public class TimeStampResource {

    @Inject DAOBean dao;

    @GET
    @Path("/{id}")
    public Response lookupTimeStampById(@PathParam("id") long timeStampId) {
        TimeStamp timeStamp = (TimeStamp) dao.find(TimeStamp.class, timeStampId);
        if(timeStamp != null) {
            return Response.ok(timeStamp.getTokenBytes()).type(ContentType.TIMESTAMP_RESPONSE.getName()).build();

        } else throw new NotFoundException("TimeStamp id '" + timeStampId + "'");
    }

}