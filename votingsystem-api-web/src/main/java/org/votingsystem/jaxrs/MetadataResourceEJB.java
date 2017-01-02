package org.votingsystem.jaxrs;

import org.votingsystem.ejb.MetadataService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/metadata")
@Stateless
public class MetadataResourceEJB {

    private static final Logger log = Logger.getLogger(MetadataResourceEJB.class.getName());

    @Inject private MetadataService metadataService;

    @GET @Path("/")
    @Produces({"application/xml"})
    public Response getMetadata(@Context HttpServletRequest req) throws Exception {
        return Response.ok().entity(metadataService.getMetadataSigned()).build();
    }

}
