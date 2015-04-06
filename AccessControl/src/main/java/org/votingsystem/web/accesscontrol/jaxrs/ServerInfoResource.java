package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.json.ActorVSJSON;
import org.votingsystem.model.ActorVS;
import org.votingsystem.web.accesscontrol.ejb.ControlCenterBean;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/serverInfo")
public class ServerInfoResource {

    //private static final Logger log = Logger.getLogger(ServerInfoResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject Logger log;
    @EJB SignatureBean signatureBean;
    @EJB TimeStampBean timeStampBean;
    @EJB ControlCenterBean controlCenterBean;

    @GET @Produces(MediaType.APPLICATION_JSON)
    public Map doGet(@Context HttpServletRequest req, @Context HttpServletResponse resp) {
        HashMap serverInfo = new HashMap();
        serverInfo.put("serverType", ActorVS.Type.ACCESS_CONTROL);
        serverInfo.put("name", config.getServerName());
        serverInfo.put("serverURL", config.getContextURL());
        serverInfo.put("state",  ActorVS.State.OK);
        serverInfo.put("date", new Date());
        serverInfo.put("controlCenter", new ActorVSJSON(controlCenterBean.getControlCenter()));
        serverInfo.put("environmentMode", config.getMode());
        serverInfo.put("timeStampCertPEM", new String(timeStampBean.getSigningCertPEMBytes()));
        serverInfo.put("timeStampServerURL", config.getTimeStampServerURL());
        serverInfo.put("certChainPEM", new String(signatureBean.getKeyStorePEMCerts()));

        //resp.setHeader("Access-Control-Allow-Origin", "*");
        //if (params.callback) render "${params.callback}(${serverInfo as JSON})"
        return serverInfo;
    }

}