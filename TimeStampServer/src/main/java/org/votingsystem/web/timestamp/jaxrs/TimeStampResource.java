package org.votingsystem.web.timestamp.jaxrs;

import org.votingsystem.model.TimeStampVS;
import org.votingsystem.util.ContentTypeVS;
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
        TimeStampVS timeStampVS = (TimeStampVS) dao.find(TimeStampVS.class, timeStampId);
        if(timeStampVS != null) {
            return Response.ok(timeStampVS.getTokenBytes()).type(ContentTypeVS.TIMESTAMP_RESPONSE.getName()).build();

        } else throw new NotFoundException("TimeStampVS id '" + timeStampId + "'");
    }

}