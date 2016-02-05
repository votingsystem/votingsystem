package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.util.JSON;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/serverInfo")
public class ServerInfoResource {

    private static final Logger log = Logger.getLogger(ServerInfoResource.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject TimeStampBean timeStampBean;

    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response doGet() throws Exception {
        ActorVSDto actorVS = new ActorVSDto();
        actorVS.setServerType(ActorVS.Type.CURRENCY);
        actorVS.setName(config.getServerName());
        actorVS.setServerURL(config.getContextURL());
        actorVS.setWebSocketURL(config.getWebSocketURL());
        actorVS.setState(ActorVS.State.OK);
        actorVS.setEnvironmentMode(config.getMode());
        actorVS.setDate(new Date());
        actorVS.setTimeStampCertPEM(new String(timeStampBean.getSigningCertPEMBytes()));
        actorVS.setTimeStampServerURL(config.getTimeStampServerURL());
        actorVS.setCertChainPEM(new String(signatureBean.getKeyStorePEMCerts()));
        //resp.setHeader("Access-Control-Allow-Origin", "*");
        //if (params.callback) render "${param.callback}(${serverInfo as JSON})"
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(actorVS)).build() ;
    }

}