package org.votingsystem.web.accesscontrol.jaxrs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.EventVSStatsDto;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.accesscontrol.ejb.EventVSBean;
import org.votingsystem.web.accesscontrol.ejb.EventVSElectionBean;
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
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/eventVSElection")
public class EventVSElectionResource {

    private static Logger log = Logger.getLogger(EventVSElectionResource.class.getName());

    @Inject DAOBean dao;
    @Inject EventVSBean eventVSBean;
    @Inject EventVSElectionBean eventVSElectionBean;
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
        EventVSDto eventVSDto = new EventVSDto(eventVS, config.getServerName(), config.getContextURL());
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
        criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        List<EventVSElection> resultList = criteria.setFirstResult(offset).setMaxResults(max).list();
        long totalCount = ((Number)DAOUtils.cleanOrderings(criteria).setProjection(Projections.rowCount()).uniqueResult()).longValue();
        List<EventVSDto> eventVSListDto = new ArrayList<>();
        for(EventVSElection eventVSElection : resultList) {
            eventVSBean.checkEventVSDates(eventVSElection);
            eventVSListDto.add(new EventVSDto(eventVSElection, config.getServerName(), config.getContextURL()));
        }
        ResultListDto<EventVSDto> resultListDto = new ResultListDto<>(eventVSListDto, offset, max, totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto))
                .type(MediaTypeVS.JSON).build();
    }

    @Path("/") @POST
    public Response save(MessageCMS messageCMS, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EventVSElection response = eventVSElectionBean.saveEvent(messageCMS);
        resp.setHeader("eventURL", format("{0}/rest/eventVSElection/id/{1}", config.getContextURL(), response.getId()));
        return Response.ok().entity(response.getPublishRequestCMS().getContentPEM()).type(MediaTypeVS.JSON_SIGNED).build();
    }

    @Path("/cancel") @POST
    public Response cancelled(MessageCMS messageCMS) throws Exception {
        MessageCMS response = eventVSBean.cancelEvent(messageCMS);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(MessageDto.OK(null))).type(MediaTypeVS.JSON).build();
    }

    @Transactional
    @Path("/id/{id}/stats") @GET
    public Response stats(@PathParam("id") long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        EventVSElection eventVS = dao.find(EventVSElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - EventVSElection not found - eventId: " + id).build();
        EventVSStatsDto statsDto = eventVSElectionBean.getStats(eventVS);
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(statsDto)).type(MediaTypeVS.JSON).build();
        } else {
            req.getSession().setAttribute("statsDto", JSON.getMapper().writeValueAsString(statsDto));
            return Response.temporaryRedirect(new URI("../eventVSElection/stats.xhtml")).build();
        }
    }

    @Transactional
    @Path("/id/{id}/checkDates") @GET
    public Response checkDates(@PathParam("id") long id) throws Exception {
        EventVS eventVS = dao.find(EventVS.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSClaim not found - " +
                "eventId: " + id).build();
        eventVSBean.checkEventVSDates(eventVS);
        return Response.ok().build();
    }

    @Transactional
    @Path("/id/{id}/publishRequest") @GET
    public Response publishRequest(@PathParam("id") long id, @Context ServletContext context,
                                   @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EventVSElection eventVS = dao.find(EventVSElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventVSElection not found - " +
                "eventId: " + id).build();
        return Response.ok().entity(eventVS.getPublishRequestCMS().getContentPEM()).type(MediaTypeVS.JSON_SIGNED).build();
    }

}
