package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.ActorDto;
import org.votingsystem.model.Actor;
import org.votingsystem.util.JSON;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
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
    @Inject CMSBean cmsBean;
    @Inject TimeStampBean timeStampBean;

    @GET @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response serverInfo(@Context HttpServletRequest req) throws Exception {
        ActorDto actor = new ActorDto();
        actor.setServerType(Actor.Type.CURRENCY);
        actor.setName(config.getServerName());
        actor.setServerURL(config.getContextURL());
        actor.setWebSocketURL(config.getWebSocketURL());
        actor.setState(Actor.State.OK);
        actor.setDate(new Date());
        actor.setTimeStampCertPEM(new String(timeStampBean.getSigningCertPEMBytes()));
        actor.setTimeStampServerURL(config.getTimeStampServerURL());
        actor.setCertChainPEM(new String(cmsBean.getKeyStorePEMCerts()));
        //resp.setHeader("Access-Control-Allow-Origin", "*");
        //if (params.callback) render "${param.callback}(${serverInfo as JSON})"
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(actor)).build() ;
    }

    @GET @Path("/certChain")
    @Produces(MediaType.TEXT_PLAIN)
    public Response cert() throws Exception {
        return Response.ok().entity(timeStampBean.getSigningCertPEMBytes()).build() ;
    }

}