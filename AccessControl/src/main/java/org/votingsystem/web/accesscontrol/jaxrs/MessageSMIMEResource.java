package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.model.MessageSMIME;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.RequestUtils;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/messageSMIME")
public class MessageSMIMEResource {

    private static final Logger log = Logger.getLogger(MessageSMIMEResource.class.getSimpleName());

    @Inject DAOBean dao;

    @Path("/id/{id}") @GET
    public Object index(@PathParam("id") long id, @Context ServletContext context,
                                @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        MessageSMIME messageSMIME = dao.find(MessageSMIME.class, id);
        if(messageSMIME == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "MessageSMIME not found - id: " + id).build();
        if(contentType.contains(ContentTypeVS.TEXT.getName())) {
            return Response.ok().entity(messageSMIME.getContent()).type(ContentTypeVS.TEXT_STREAM.getName()).build();
        } else return RequestUtils.processRequest(messageSMIME, context, req, resp);
    }

}
