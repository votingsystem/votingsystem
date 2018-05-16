package org.votingsystem.jaxrs;

import org.votingsystem.ejb.MetadataService;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.xml.XML;

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
    @Inject private TrustedServicesEJB trustedServicesEJB;

    @GET @Path("/")
    @Produces({"application/xml"})
    public Response getMetadata(@Context HttpServletRequest req) throws Exception {
        return Response.ok().entity(metadataService.getMetadataSigned()).build();
    }

    @GET @Path("/trusted")
    @Produces({"application/xml"})
    public Response trustedEntities(@Context HttpServletRequest req) throws Exception {
        return Response.ok().type(javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE)
                .entity(XML.getMapper().writeValueAsBytes(trustedServicesEJB.getTrustedEntities())).build();
    }

}
