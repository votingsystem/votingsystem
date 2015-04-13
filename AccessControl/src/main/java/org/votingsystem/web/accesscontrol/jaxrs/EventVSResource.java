package org.votingsystem.web.accesscontrol.jaxrs;

import org.votingsystem.model.EventVS;
import org.votingsystem.model.EventVSClaim;
import org.votingsystem.model.EventVSElection;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.accesscontrol.ejb.EventVSBean;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("/eventVS")
public class EventVSResource {

    private static final Logger log = Logger.getLogger(EventVSResource.class.getSimpleName());

    @Inject EventVSBean eventVSBean;
    @Inject DAOBean dao;

    @Path("/id/{id}") @GET
    public Response index(@PathParam("id") long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EventVS eventVS = dao.find(EventVS.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSClaim not found - " +
                "eventId: " + id).build();
        if(eventVS instanceof EventVSClaim) {
            context.getRequestDispatcher("/rest/eventVSClaim/id/" + eventVS.getId()).forward(req, resp);
            return Response.ok().build();
        } else if(eventVS instanceof EventVSElection) {
            context.getRequestDispatcher("/rest/eventVSElection/id/" + eventVS.getId()).forward(req, resp);
            return Response.ok().build();
        } else throw new ExceptionVS("unprocessed event typy: " + eventVS.getClass().getName());
    }

    @Path("/cancel") @POST
    public Response cancelled(MessageSMIME messageSMIME) throws Exception {
        MessageSMIME response = eventVSBean.cancelEvent(messageSMIME);
        return Response.ok().entity(response.getContent()).type(MediaTypeVS.JSON).build();
    }

    @Path("/id/{id}/checkDates") @GET
    public Response checkDates(@PathParam("id") long id) throws Exception {
        EventVS eventVS = dao.find(EventVS.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSClaim not found - " +
                "eventId: " + id).build();
        eventVSBean.checkEventVSDates(eventVS);
        return Response.ok().build();
    }
}
