package org.votingsystem.web.controlcenter.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.json.EventVSJSON;
import org.votingsystem.model.EventVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.controlcenter.ejb.EventVSElectionBean;
import org.votingsystem.web.ejb.DAOBean;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("/search")
public class SearchResource {

    private static final Logger log = Logger.getLogger(SearchResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject EventVSElectionBean eventVSElectionBean;

    @Path("/eventvsByTag") @GET
    public Response eventvsByTag (@QueryParam("tag") String tag) throws JsonProcessingException {
        if(tag == null) return Response.status(Response.Status.BAD_REQUEST).entity("ERROR - missing param 'tag'").build();
        Query query = dao.getEM().createQuery(
                "select e from EventVS e inner join e.tagVSSet tag where tag.name =:tag").setParameter("tag", tag);
        List<EventVS> eventVSList = query.getResultList();
        List<Map> resultList = query.getResultList();
        for(EventVS eventVS :eventVSList) {
            Map eventMap = new HashMap<>();
            eventMap.put("id", eventVS.getId());
            eventMap.put("subject", eventVS.getSubject());
            eventMap.put("content", eventVS.getContent());
            eventMap.put("URL", config.getRestURL() + "/eventVS/id/" + eventVS.getId());
            resultList.add(eventMap);
        }
        Map resultMap = new HashMap<>();
        resultMap.put("eventsVS", resultList);
        return Response.ok().entity(new ObjectMapper().writeValueAsBytes(resultMap)).type(ContentTypeVS.JSON.getName()).build();
    }

    @Path("/eventVS") @GET
    public Response eventVS(@QueryParam("searchText") String searchText, @QueryParam("eventVSState") String eventVSStateReq,
                          @DefaultValue("0") @QueryParam("offset") int offset,
                          @DefaultValue("100") @QueryParam("max") int max,
                          @Context Request req, @Context HttpServletResponse resp) throws JsonProcessingException {
        List<EventVS.State> inList = Arrays.asList(EventVS.State.ACTIVE, EventVS.State.PENDING, EventVS.State.CANCELED,
                EventVS.State.TERMINATED);
        if(eventVSStateReq != null) {
            try {
                EventVS.State eventVSState = EventVS.State.valueOf(eventVSStateReq);
                if(eventVSState == EventVS.State.TERMINATED) {
                    inList = Arrays.asList(EventVS.State.TERMINATED,EventVS.State.CANCELED);
                } else if(eventVSState != EventVS.State.DELETED_FROM_SYSTEM) inList = Arrays.asList(eventVSState);
            } catch(Exception ex) {}
        }
        /*Date dateBeginFrom = null;
        Date dateBeginTo = null;
        if(params.dateBeginFrom) try {dateBeginFrom = DateUtils.getDateFromString(params.dateBeginFrom)} catch(Exception ex) {}
        if(params.dateBeginTo) try {dateBeginTo = DateUtils.getDateFromString(params.dateBeginTo)} catch(Exception ex) {}*/
        Query query = dao.getEM().createQuery("select e from EventVS e where e.state in :inList and " +
                "(e.subject like :searchText or e.content like :searchText)").setParameter("inList", inList)
                .setParameter("searchText", "%" + searchText + "%");
        List<EventVS> eventvsList = query.getResultList();
        List resultList = null;
        for(EventVS eventVS : eventvsList) {
            resultList.add(new EventVSJSON(eventVS, config.getServerName(), config.getContextURL()));
        }
        Map resultMap = new HashMap<>();
        resultMap.put("eventVS", resultList);
        resultMap.put("totalCount", resultList.size());
        resultMap.put("offset", offset);
        resultMap.put("max", max);
        return Response.ok().entity(new ObjectMapper().writeValueAsBytes(resultMap)).type(ContentTypeVS.JSON.getName()).build();
    }


}
