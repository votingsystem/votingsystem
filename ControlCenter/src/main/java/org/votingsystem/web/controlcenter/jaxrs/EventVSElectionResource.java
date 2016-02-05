package org.votingsystem.web.controlcenter.jaxrs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.EventVSStatsDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.controlcenter.ejb.EventVSElectionBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.DAOUtils;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/eventVSElection")
public class EventVSElectionResource {

    private static Logger log = Logger.getLogger(EventVSElectionResource.class.getName());

    @Inject DAOBean dao;
    @Inject EventVSElectionBean eventVSBean;
    @Inject ConfigVS config;

    @Transactional
    @Path("/id/{id}") @GET
    public Response getById (@PathParam("id") long id, @Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws ValidationExceptionVS, IOException, ServletException, URISyntaxException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        List<EventVS.State> inList = Arrays.asList(EventVS.State.ACTIVE, EventVS.State.PENDING, EventVS.State.CANCELED,
                EventVS.State.TERMINATED);
        Query query = dao.getEM().createQuery("select e from EventVSElection e where e.state in :inList and " +
                "e.id =:id").setParameter("inList", inList).setParameter("id", id);
        EventVSElection eventVS =  dao.getSingleResult(EventVSElection.class, query);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSElection not found - " +
                "eventId: " + id).build();
        eventVSBean.checkEventVSDates(eventVS);
        EventVSDto eventVSDto = new EventVSDto(eventVS);
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(eventVSDto)).type(MediaTypeVS.JSON).build();
        } else {
            req.getSession().setAttribute("eventDto", JSON.getMapper().writeValueAsString(eventVSDto));
            return Response.temporaryRedirect(new URI("../eventVSElection/eventVSElection.xhtml")).build();
        }
    }

    @Transactional
    @Path("/") @GET
    public Response index (@QueryParam("eventVSState") String eventVSStateReq,
                   @DefaultValue("0") @QueryParam("offset") int offset,
                   @DefaultValue("50") @QueryParam("max") int max, @Context ServletContext context,
                   @Context HttpServletRequest req, @Context HttpServletResponse resp) throws ValidationExceptionVS,
            IOException, ServletException {
        List<EventVS.State> inList = Arrays.asList(EventVS.State.ACTIVE);
        if(eventVSStateReq != null) {
            try {
                EventVS.State eventVSState = EventVS.State.valueOf(eventVSStateReq);
                if(eventVSState == EventVS.State.TERMINATED) {
                    inList = Arrays.asList(EventVS.State.TERMINATED,EventVS.State.CANCELED);
                } else if(eventVSState != EventVS.State.DELETED_FROM_SYSTEM) inList = Arrays.asList(eventVSState);
            } catch(Exception ex) {}
        }
        Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(EventVSElection.class);
        criteria.add(Restrictions.in("state", inList));
        criteria.addOrder(Order.desc("dateBegin"));
        List<EventVSElection> resultList = criteria.setFirstResult(offset).setMaxResults(max).list();
        long totalCount = ((Number) DAOUtils.cleanOrderings(criteria).setProjection(Projections.rowCount()).uniqueResult()).longValue();
        List<EventVSDto> eventVSListDto = new ArrayList<>();
        for(EventVSElection eventVSElection : resultList) {
            eventVSBean.checkEventVSDates(eventVSElection);
            eventVSListDto.add(new EventVSDto(eventVSElection));
        }
        ResultListDto<EventVSDto> resultListDto = new ResultListDto<>(eventVSListDto, offset, max, totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto))
                .type(MediaTypeVS.JSON).build();
    }

    @Path("/") @POST
    public Response save(MessageSMIME messageSMIME, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EventVSElection eventVSElection = eventVSBean.saveEvent(messageSMIME);
        return Response.ok().entity(eventVSElection.getId()).type(MediaType.TEXT_PLAIN).build();
    }

    @Path("/cancel") @POST
    public Response cancel(MessageSMIME messageSMIME, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessageSMIME responseMessage = eventVSBean.cancelEvent(messageSMIME);
        return Response.ok().entity(responseMessage.getContent()).type(MediaType.TEXT_PLAIN).build();
    }

    @Transactional
    @Path("/id/{id}/stats") @GET
    public Response stats(@PathParam("id") long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        EventVSElection eventVS = dao.find(EventVSElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - EventVSElection not found - eventId: " + id).build();
        EventVSStatsDto statsDto = eventVSBean.getStats(eventVS);
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(statsDto)).type(MediaTypeVS.JSON).build();
        } else {
            req.getSession().setAttribute("statsDto", JSON.getMapper().writeValueAsString(statsDto));
            return Response.temporaryRedirect(new URI("../eventVSElection/stats.xhtml")).build();
        }
    }

    @Path("/id/{id}/publishRequest") @GET
    public Response publishRequest(@PathParam("id") long id, @Context ServletContext context,
                           @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EventVSElection eventVS = dao.find(EventVSElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSElection not found - " +
                "eventId: " + id).build();
        return Response.ok().entity(eventVS.getPublishRequestSMIME().getContent()).type(MediaTypeVS.JSON_SIGNED).build();
    }

}
