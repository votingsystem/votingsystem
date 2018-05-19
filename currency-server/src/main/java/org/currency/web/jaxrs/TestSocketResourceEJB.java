package org.currency.web.jaxrs;

import org.currency.web.jsf.SocketPushEvent;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/testSocket")
public class TestSocketResourceEJB {

    private static final Logger log = Logger.getLogger(TestSocketResourceEJB.class.getName());

    @Inject private BeanManager beanManager;

    @GET @Path("/singleUser")
    @Produces(MediaType.TEXT_PLAIN)
    public Response singleUser(@Context HttpServletRequest req, @Context HttpServletResponse res,
                               @QueryParam("userUUID") String userUUID) throws Exception {
        ResponseDto response = new ResponseDto().setMessage("singleUser TO_USER").setType(CurrencyOperation.GET_SESSION_CERTIFICATION);
        SocketPushEvent pushEvent = new SocketPushEvent(new JSON().getMapper().writeValueAsString(response), userUUID,
                SocketPushEvent.Type.TO_USER);
        beanManager.fireEvent(pushEvent);
        HttpSession session = req.getSession();
        log.info("session.getId: " + session.getId());
        return Response.ok().entity("session id: " + session.getId()).build() ;
    }

    @GET @Path("/broadcast")
    @Produces(MediaType.TEXT_PLAIN)
    public Response broadcast(@Context HttpServletRequest req,
                               @Context HttpServletResponse res) throws Exception {
        SocketPushEvent pushEvent = new SocketPushEvent("ALL_USERS message", SocketPushEvent.Type.ALL_USERS);
        beanManager.fireEvent(pushEvent);
        HttpSession session = req.getSession();
        log.info("session.getId: " + session.getId());
        return Response.ok().entity("session id: " + session.getId()).build() ;
    }

    @GET @Path("/group")
    @Produces(MediaType.TEXT_PLAIN)
    public Response group(@Context HttpServletRequest req, @Context HttpServletResponse res,
                          @QueryParam("userUUIDList") String userUUIDList) throws Exception {
        List<String> userList = Arrays.asList(userUUIDList.split(","));
        SocketPushEvent pushEvent = new SocketPushEvent("TO_GROUP message", SocketPushEvent.Type.TO_GROUP).setUserSet(new HashSet<>(userList));
        beanManager.fireEvent(pushEvent);
        HttpSession session = req.getSession();
        log.info("session.getId: " + session.getId());
        return Response.ok().entity("session id: " + session.getId()).build() ;
    }
}