package org.votingsystem.web.accesscontrol.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.json.EventVSJSON;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.EventVSClaim;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.accesscontrol.ejb.EventVSBean;
import org.votingsystem.web.accesscontrol.ejb.EventVSClaimBean;
import org.votingsystem.web.accesscontrol.ejb.EventVSClaimCollectorBean;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
@Path("/eventVSClaim")
public class EventVSClaimResource {

    private static Logger log = Logger.getLogger(EventVSClaimResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject EventVSClaimBean eventVSClaimBean;
    @Inject EventVSBean eventVSBean;
    @Inject EventVSClaimCollectorBean eventVSClaimCollectorBean;

    @Path("/") @GET
    public Object index (@QueryParam("id") Long id,
                         @QueryParam("eventVSState") String eventVSStateReq,
                         @DefaultValue("0") @QueryParam("offset") int offset,
                         @DefaultValue("100") @QueryParam("max") int max, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws ValidationExceptionVS,
            IOException, ServletException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        if (id != null) {
            List<EventVS.State> inList = Arrays.asList(EventVS.State.ACTIVE, EventVS.State.PENDING, EventVS.State.CANCELED,
                    EventVS.State.TERMINATED);
            Query query = dao.getEM().createQuery("select e from EventVSClaim e where e.state in :inList and " +
                    "e.id =:id").setParameter("inList", inList).setParameter("id", id);
            EventVSClaim eventVS =  dao.getSingleResult(EventVSClaim.class, query);
            if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSClaim not found - " +
                    "eventId: " + id).build();
            eventVSBean.checkEventVSDates(eventVS);
            EventVSJSON eventVSJSON = new EventVSJSON(eventVS, config.getServerName(), config.getRestURL());
            query = dao.getEM().createQuery("select count(m) from MessageSMIME m where m.eventVS =:eventVS and " +
                    "m.type =:type").setParameter("eventVS", eventVS).setParameter("type", TypeVS.CLAIM_EVENT_SIGN);
            eventVSJSON.setNumSignatures((long) query.getSingleResult());
            if(contentType.contains("json")) {
                return Response.ok().entity(new ObjectMapper().writeValueAsBytes(eventVSJSON))
                        .type(ContentTypeVS.JSON.getName()).build();
            } else {
                req.setAttribute("eventMap", JSON.getEscapingMapper().writeValueAsString(eventVSJSON));
                context.getRequestDispatcher("/jsf/eventVSClaim/eventVSClaim.jsp").forward(req, resp);
                return Response.ok().build();
            }
        }
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
        Query query = dao.getEM().createQuery("select e from EventVSClaim e where e.state in :inList")
                .setParameter("inList", inList).setFirstResult(offset).setMaxResults(max);
        List<EventVSClaim> resultList = query.getResultList();
        List<EventVSJSON> resultListJSON = new ArrayList<>();
        for(EventVSClaim eventVSClaim : resultList) {
            eventVSBean.checkEventVSDates(eventVSClaim);
            resultListJSON.add(new EventVSJSON(eventVSClaim, config.getServerName(), config.getContextURL()));
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
            req.setAttribute("eventsVSMap", JSON.getEscapingMapper().writeValueAsString(eventsVSMap));
            context.getRequestDispatcher("/jsf/eventVSClaim/index.jsp").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/editor") @GET
    public Response editor(@Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        context.getRequestDispatcher("/jsf/eventVSClaim/editor.jsp").forward(req, resp);
        return Response.ok().build();
    }

    @Path("/id/{id}/publishRequest") @GET
    public Response publishRequest(@PathParam("id") long id, @Context ServletContext context,
                   @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        //contextURL + "/eventVSElection/id/" + eventVS.getId() + "/publishRequest"
        EventVSClaim eventVS = dao.find(EventVSClaim.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSClaim not found - " +
                "eventId: " + id).build();
        Query query = dao.getEM().createQuery("select m from MessageSMIME m where m.eventVS =:eventVS " +
                "and m.type =:type").setParameter("eventVS", eventVS).setParameter("type", TypeVS.CLAIM_EVENT);
        MessageSMIME messageSMIME = dao.getSingleResult(MessageSMIME.class, query);
        if(messageSMIME == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSClaim without " +
                "publishRequest - eventId: " + id).build();
        return Response.ok().entity(messageSMIME.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }

    @Path("/collect") @POST
    public Response collect(MessageSMIME messageSMIME, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessageSMIME response = eventVSClaimCollectorBean.save(messageSMIME);
        return Response.ok().entity(response.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }
    
    @Path("/") @POST
    public Response save(MessageSMIME messageSMIME, @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessageSMIME response = eventVSClaimBean.saveEvent(messageSMIME);
        resp.setHeader("eventURL", format("{0}/eventVSClaim/{1}", config.getRestURL(), response.getEventVS().getId()));
        return Response.ok().entity(response.getContent()).type(ContentTypeVS.JSON_SIGNED.getName()).build();
    }

    @Path("/id/{id}/signaturesInfo") @GET
    public Response signaturesInfo(@PathParam("id") long id, @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EventVSClaim eventVS = dao.find(EventVSClaim.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSClaim not found - " +
                "eventId: " + id).build();
        Map resultMap = new HashMap();
        Query query = dao.getEM().createQuery("select m from MessageSMIME m where m.type =:type and m.eventVS =:eventVS")
                .setParameter("type", TypeVS.CLAIM_EVENT_SIGN).setParameter("eventVS", eventVS);
        List<MessageSMIME> messageSMIMEList = query.getResultList();
        resultMap.put("numSignatures", messageSMIMEList.size());
        resultMap.put("eventVSSubject", eventVS.getSubject());
        resultMap.put("eventURL", config.getRestURL() + "/eventVSClaim/id/" + eventVS.getId());
        List<String> signatureList = new ArrayList<>();
        for(MessageSMIME messageSMIME : messageSMIMEList) {
            signatureList.add(format("{o}/messageSMIME/id/{1}", config.getRestURL(), messageSMIME.getId()));
        }
        resultMap.put("signatures", signatureList);
        return Response.ok().entity(new ObjectMapper().writeValueAsBytes(resultMap)).type(
                ContentTypeVS.JSON.getName()).build();
    }
}
