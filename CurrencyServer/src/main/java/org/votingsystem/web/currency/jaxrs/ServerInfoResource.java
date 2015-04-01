package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.model.ActorVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/serverInfo")
public class ServerInfoResource {

    private static final Logger log = Logger.getLogger(ServerInfoResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;
    @Inject TimeStampBean timeStampBean;

    @GET @Produces(MediaType.APPLICATION_JSON)
    public Map doGet(@Context Request req, @Context HttpServletResponse resp) {
        HashMap serverInfo = new HashMap();
        serverInfo.put("serverType", ActorVS.Type.CURRENCY);
        serverInfo.put("name", config.getServerName());
        serverInfo.put("serverURL", config.getContextURL());
        serverInfo.put("webSocketURL", config.getWebSocketURL());
        serverInfo.put("state",  ActorVS.State.OK);
        serverInfo.put("environmentMode", config.getMode());
        serverInfo.put("date", new Date());
        serverInfo.put("timeStampCertPEM", new String(timeStampBean.getSigningCertPEMBytes()));
        serverInfo.put("urlTimeStampServer", config.getTimeStampServerURL());
        serverInfo.put("certChainPEM", new String(signatureBean.getKeyStorePEMCerts()));
        //resp.setHeader("Access-Control-Allow-Origin", "*");
        //if (params.callback) render "${params.callback}(${serverInfo as JSON})"
        return serverInfo;
    }

}