package org.votingsystem.web.accesscontrol.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.json.EventVSJSON;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.EventVSElection;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.accesscontrol.ejb.EventVSBean;
import org.votingsystem.web.accesscontrol.ejb.EventVSElectionBean;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/eventVSElection")
public class EventVSElectionResource {

    private static Logger log = Logger.getLogger(EventVSElectionResource.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject EventVSBean eventVSBean;
    @Inject EventVSElectionBean eventVSElectionBean;
    @Inject ConfigVS config;

    @Path("/id/{id}") @GET
    @Transactional
    public Object getById (@PathParam("id") long id, @Context ServletContext context, @Context HttpServletRequest req,
                           @Context HttpServletResponse resp) throws ValidationExceptionVS, IOException, ServletException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        List<EventVS.State> inList = Arrays.asList(EventVS.State.ACTIVE, EventVS.State.PENDING, EventVS.State.CANCELED,
                EventVS.State.TERMINATED);
        Query query = dao.getEM().createQuery("select e from EventVSElection e where e.state in :inList and " +
                "e.id =:id").setParameter("inList", inList).setParameter("id", id);
        EventVSElection eventVS =  dao.getSingleResult(EventVSElection.class, query);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSElection not found - " +
                "eventId: " + id).build();
        eventVSBean.checkEventVSDates(eventVS);
        EventVSJSON eventVSJSON = new EventVSJSON(eventVS, config.getServerName(), config.getContextURL());
        if(contentType.contains("json")) {
            return Response.ok().entity(new ObjectMapper().writeValueAsBytes(eventVSJSON))
                    .type(ContentTypeVS.JSON.getName()).build();
        } else {
            req.setAttribute("eventMap", JSON.getInstance().writeValueAsString(eventVSJSON));
            context.getRequestDispatcher("/jsf/eventVSElection/eventVSElection.jsp").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/") @GET
    @Transactional
    public Object index (@QueryParam("eventVSState") String eventVSStateReq,
                         @DefaultValue("0") @QueryParam("offset") int offset,
                         @DefaultValue("100") @QueryParam("max") int max, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws ValidationExceptionVS,
            IOException, ServletException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        List<EventVS.State> inList = Arrays.asList(EventVS.State.ACTIVE, EventVS.State.PENDING,
                EventVS.State.TERMINATED, EventVS.State.CANCELED);
        if(eventVSStateReq != null) {
            try {
                EventVS.State eventVSState = EventVS.State.valueOf(eventVSStateReq);
                if(eventVSState == EventVS.State.TERMINATED) {
                    inList = Arrays.asList(EventVS.State.TERMINATED,EventVS.State.CANCELED);
                } else if(eventVSState != EventVS.State.DELETED_FROM_SYSTEM) inList = Arrays.asList(eventVSState);
            } catch(Exception ex) {}
        }
        Query query = dao.getEM().createQuery("select e from EventVSElection e where e.state in :inList")
                .setParameter("inList", inList).setFirstResult(offset).setMaxResults(max);
        List<EventVSElection> resultList = query.getResultList();
        List<EventVSJSON> resultListJSON = new ArrayList<>();
        for(EventVSElection eventVSElection : resultList) {
            eventVSBean.checkEventVSDates(eventVSElection);
            resultListJSON.add(new EventVSJSON(eventVSElection, config.getServerName(), config.getContextURL()));
        }
        Map eventsVSMap = new HashMap();
        eventsVSMap.put("eventVS", resultListJSON);
        eventsVSMap.put("offset", offset);
        eventsVSMap.put("max", max);
        eventsVSMap.put("totalCount", resultListJSON.size()); //TODO
        if(contentType.contains("json")){
            return Response.ok().entity(new ObjectMapper().writeValueAsBytes(eventsVSMap))
                    .type(ContentTypeVS.JSON.getName()).build();
        } else {
            req.setAttribute("eventsVSMap", JSON.getInstance().writeValueAsString(eventsVSMap));
            context.getRequestDispatcher("/jsf/eventVSElection/index.jsp").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/") @POST
    public Response save(MessageSMIME messageSMIME, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessageSMIME response = eventVSElectionBean.saveEvent(messageSMIME);
        resp.setHeader("eventURL", format("{0}/eventVSElection/id/{1}", config.getRestURL(), response.getEventVS().getId()));
        return Response.ok().entity(response.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }

    @Path("/id/{id}/stats") @GET
    public Response stats(@PathParam("id") long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        EventVSElection eventVS = dao.find(EventVSElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSElection not found - " +
                "eventId: " + id).build();
        Map statsMap = eventVSElectionBean.getStatsMap(eventVS);
        if(contentType.contains("json")) {
            return Response.ok().entity(new ObjectMapper().writeValueAsBytes(statsMap))
                    .type(ContentTypeVS.JSON.getName()).build();
        } else {
            req.setAttribute("statsJSON", JSON.getInstance().writeValueAsString(statsMap));
            context.getRequestDispatcher("/jsf/eventVSElection/stats.jsp").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/id/{id}/publishRequest") @GET
    public Response publishRequest(@PathParam("id") long id, @Context ServletContext context,
                                   @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        //contextURL + "/eventVSElection/id/" + eventVS.getId() + "/publishRequest"
        EventVSElection eventVS = dao.find(EventVSElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSElection not found - " +
                "eventId: " + id).build();
        Query query = dao.getEM().createQuery("select m from MessageSMIME m where m.eventVS =:eventVS " +
                "and m.type =:type").setParameter("eventVS", eventVS).setParameter("type", TypeVS.VOTING_EVENT);
        MessageSMIME messageSMIME = dao.getSingleResult(MessageSMIME.class, query);
        if(messageSMIME == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSElection without " +
                "publishRequest - eventId: " + id).build();
        return Response.ok().entity(messageSMIME.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }
}
