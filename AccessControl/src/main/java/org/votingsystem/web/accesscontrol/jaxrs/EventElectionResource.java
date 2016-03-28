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
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.web.accesscontrol.ejb.EventElectionBean;
import org.votingsystem.web.accesscontrol.ejb.EventVSBean;
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
@Path("/eventElection")
public class EventElectionResource {

    private static Logger log = Logger.getLogger(EventElectionResource.class.getName());

    @Inject DAOBean dao;
    @Inject EventVSBean eventVSBean;
    @Inject EventElectionBean eventElectionBean;
    @Inject ConfigVS config;

    @Transactional
    @Path("/id/{id}") @GET
    public Response getById (@PathParam("id") long id, @Context ServletContext context, @Context HttpServletRequest req,
                       @Context HttpServletResponse resp) throws ValidationException, IOException, ServletException, URISyntaxException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        List<EventVS.State> inList = Arrays.asList(EventVS.State.ACTIVE, EventVS.State.PENDING, EventVS.State.CANCELED,
                EventVS.State.TERMINATED);
        Query query = dao.getEM().createQuery("select e from EventElection e where e.state in :inList and " +
                "e.id =:id").setParameter("inList", inList).setParameter("id", id);
        EventElection eventVS =  dao.getSingleResult(EventElection.class, query);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventElection not found - " +
                "eventId: " + id).build();
        eventVSBean.checkEventVSDates(eventVS);
        EventVSDto eventVSDto = new EventVSDto(eventVS, config.getServerName(), config.getContextURL());
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(eventVSDto)).type(MediaType.JSON).build();
        } else {
            req.getSession().setAttribute("eventDto", JSON.getMapper().writeValueAsString(eventVSDto));
            return Response.temporaryRedirect(new URI("../eventElection/eventElection.xhtml")).build();
        }
    }

    @Transactional
    @Path("/") @GET
    public Response index (@QueryParam("eventVSState") String eventVSStateReq,
                         @DefaultValue("0") @QueryParam("offset") int offset,
                         @DefaultValue("50") @QueryParam("max") int max, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws ValidationException,
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
        Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(EventElection.class);
        criteria.add(Restrictions.in("state", inList));
        criteria.addOrder(Order.desc("dateBegin"));
        criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        List<EventElection> resultList = criteria.setFirstResult(offset).setMaxResults(max).list();
        criteria.setFirstResult(0); //reset offset for total count
        long totalCount = ((Number)DAOUtils.cleanOrderings(criteria).setProjection(Projections.rowCount()).uniqueResult()).longValue();
        List<EventVSDto> eventVSListDto = new ArrayList<>();
        for(EventElection eventElection : resultList) {
            eventVSBean.checkEventVSDates(eventElection);
            eventVSListDto.add(new EventVSDto(eventElection, config.getServerName(), config.getContextURL()));
        }
        ResultListDto<EventVSDto> resultListDto = new ResultListDto<>(eventVSListDto, offset, max, totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto))
                .type(MediaType.JSON).build();
    }

    @Path("/") @POST
    public Response save(CMSMessage cmsMessage, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        EventElection response = eventElectionBean.saveEvent(cmsMessage);
        resp.setHeader("eventURL", format("{0}/rest/eventElection/id/{1}", config.getContextURL(), response.getId()));
        return Response.ok().entity(response.getCmsMessage().getContentPEM()).type(MediaType.JSON_SIGNED).build();
    }

    @Path("/cancel") @POST
    public Response cancelled(CMSMessage cmsMessage) throws Exception {
        CMSMessage response = eventVSBean.cancelEvent(cmsMessage);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(MessageDto.OK(null))).type(MediaType.JSON).build();
    }

    @Transactional
    @Path("/id/{id}/stats") @GET
    public Response stats(@PathParam("id") long id, @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        EventElection eventVS = dao.find(EventElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - EventElection not found - eventId: " + id).build();
        EventVSStatsDto statsDto = eventElectionBean.getStats(eventVS);
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(statsDto)).type(MediaType.JSON).build();
        } else {
            req.getSession().setAttribute("statsDto", JSON.getMapper().writeValueAsString(statsDto));
            return Response.temporaryRedirect(new URI("../eventElection/stats.xhtml")).build();
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
        EventElection eventVS = dao.find(EventElection.class, id);
        if(eventVS == null) return Response.status(Response.Status.NOT_FOUND).entity("ERROR - EventElection not found - " +
                "eventId: " + id).build();
        return Response.ok().entity(eventVS.getCmsMessage().getContentPEM()).type(MediaType.JSON_SIGNED).build();
    }

}
