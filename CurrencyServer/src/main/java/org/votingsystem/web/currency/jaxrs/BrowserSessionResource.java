package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.web.currency.util.PrincipalVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/browserSession")
public class BrowserSessionResource {

    private static final Logger log = Logger.getLogger(BrowserSessionResource.class.getName());

    @Inject DAOBean dao;

    @Path("/init")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response init(CMSMessage cmsMessage, @Context HttpServletRequest req,
                         @Context HttpServletResponse res) throws Exception {
        HttpSession session = req.getSession(true);
        session.setAttribute(PrincipalVS.USER_KEY, cmsMessage.getUser());
        return Response.ok().build();
    }

    @Path("/close")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response close(@Context HttpServletRequest req,
                          @Context HttpServletResponse res) throws Exception {
        HttpSession session = req.getSession(true);
        session.removeAttribute(PrincipalVS.USER_KEY);
        return Response.ok().build();
    }


}