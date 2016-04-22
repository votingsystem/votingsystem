package org.votingsystem.web.timestamp.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.model.Actor;
import org.votingsystem.service.TimeStampService;
import org.votingsystem.util.JSON;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/serverInfo")
public class ServerInfoResource {

    private static final Logger log = Logger.getLogger(ServerInfoResource.class.getName());

    @Inject TimeStampService timeStampService;
    @Inject ConfigVS config;

    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response doGet(@Context HttpServletResponse resp) throws JsonProcessingException {
        HashMap dataMap = new HashMap();
        dataMap.put("serverType", Actor.Type.TIMESTAMP_SERVER);
        dataMap.put("certChainPEM", new String(timeStampService.getSigningCertChainPEMBytes()));
        dataMap.put("serverURL", config.getContextURL());
        resp.setHeader("Access-Control-Allow-Origin", "*");
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dataMap)).build() ;
    }

}