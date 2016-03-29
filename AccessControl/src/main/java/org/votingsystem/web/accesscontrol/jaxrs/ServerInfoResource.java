package org.votingsystem.web.accesscontrol.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.voting.ControlCenter;
import org.votingsystem.util.JSON;
import org.votingsystem.web.ejb.CMSBean;
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
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/serverInfo")
public class ServerInfoResource {

    private static final Logger log = Logger.getLogger(ServerInfoResource.class.getName());

    @EJB ConfigVS config;
    @EJB CMSBean cmsBean;
    @EJB TimeStampBean timeStampBean;

    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response doGet(@Context HttpServletRequest req, @Context HttpServletResponse resp) throws JsonProcessingException {
        ActorDto actor = new ActorDto();
        actor.setServerType(Actor.Type.ACCESS_CONTROL);
        actor.setName(config.getServerName());
        actor.setServerURL(config.getContextURL());
        actor.setWebSocketURL(config.getWebSocketURL());
        actor.setState(Actor.State.OK);
        actor.setDate(new Date());
        actor.setTimeStampCertPEM(new String(timeStampBean.getSigningCertPEMBytes()));
        actor.setTimeStampServerURL(config.getTimeStampServerURL());
        actor.setCertChainPEM(new String(cmsBean.getKeyStoreCertificatesPEM()));
        ControlCenter controlCenter = config.getControlCenter();
        actor.setControlCenter(new ActorDto(controlCenter));
        //resp.setHeader("Access-Control-Allow-Origin", "*");
        //if (params.callback) render "${param.callback}(${serverInfo as JSON})"
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(actor)).build() ;
    }

}