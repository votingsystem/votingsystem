package org.votingsystem.serviceprovider.ejb;

import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/vote")
@Stateless
public class VoteEJB {

    private static final Logger log = Logger.getLogger(VoteEJB.class.getName());

    @GET @Path("/send")
    @Produces(MediaType.APPLICATION_JSON)
    public Response certs(@Context HttpServletRequest req) throws Exception {
        return Response.ok().entity("OK").build() ;
    }

}
