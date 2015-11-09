package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.EJB;
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

    private static final Logger log = Logger.getLogger(ServerInfoResource.class.getSimpleName());

    @EJB ConfigVS configVS;
    @EJB SignatureBean signatureBean;
    @EJB TimeStampBean timeStampBean;

    @GET @Produces(MediaType.APPLICATION_JSON)
    public Map doGet(@Context HttpServletRequest req, @Context HttpServletResponse resp) {
        HashMap serverInfo = new HashMap();
        serverInfo.put("serverType", ActorVS.Type.ACCESS_CONTROL);
        serverInfo.put("name", configVS.getServerName());
        serverInfo.put("serverURL", configVS.getContextURL());
        serverInfo.put("state",  ActorVS.State.OK);
        serverInfo.put("date", new Date());
        ControlCenterVS controlCenterVS = configVS.getControlCenter();
        if(controlCenterVS != null) serverInfo.put("controlCenter", new ActorVSDto(controlCenterVS));
        serverInfo.put("environmentMode", configVS.getMode());
        serverInfo.put("timeStampCertPEM", new String(timeStampBean.getSigningCertPEMBytes()));
        serverInfo.put("timeStampServerURL", configVS.getTimeStampServerURL());
        serverInfo.put("certChainPEM", new String(signatureBean.getKeyStorePEMCerts()));
        //resp.setHeader("Access-Control-Allow-Origin", "*");
        //if (params.callback) render "${param.callback}(${serverInfo as JSON})"
        return serverInfo;
    }

}