package org.votingsystem.web.timestamp.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.model.ActorVS;
import org.votingsystem.services.TimeStampService;
import org.votingsystem.util.JSON;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/serverInfo")
public class ServerInfoResource {

    private static final Logger log = Logger.getLogger(ServerInfoResource.class.getSimpleName());

    @Inject TimeStampService timeStampService;
    @Inject ConfigVS config;


    @GET @Produces(MediaType.APPLICATION_JSON)
    public String doGet(@Context Request req, @Context HttpServletResponse resp) throws URISyntaxException, JsonProcessingException {
        HashMap serverInfo = new HashMap();
        serverInfo.put("serverType", ActorVS.Type.TIMESTAMP_SERVER);
        serverInfo.put("certChainPEM", new String(timeStampService.getSigningCertChainPEMBytes()));
        serverInfo.put("serverURL", config.getContextURL());
        serverInfo.put("environmentMode", config.getMode());
        resp.setHeader("Access-Control-Allow-Origin", "*");
        return JSON.getMapper().writeValueAsString(serverInfo);
    }

}