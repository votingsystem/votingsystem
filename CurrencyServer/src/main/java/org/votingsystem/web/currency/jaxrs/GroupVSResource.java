package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.dto.currency.SubscriptionVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.currency.ejb.BalancesBean;
import org.votingsystem.web.currency.ejb.CurrencyAccountBean;
import org.votingsystem.web.currency.ejb.GroupVSBean;
import org.votingsystem.web.currency.ejb.UserVSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.SubscriptionVSBean;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/groupVS")
public class GroupVSResource {

    private static final Logger log = Logger.getLogger(GroupVSResource.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject GroupVSBean groupVSBean;
    @Inject UserVSBean userVSBean;
    @Inject BalancesBean balancesBean;
    @Inject SignatureBean signatureBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    private MessagesVS messages = MessagesVS.getCurrentInstance();

    @Path("/")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response index(@DefaultValue("0") @QueryParam("offset") int offset,
                        @DefaultValue("100") @QueryParam("max") int max,
                        @QueryParam("state") String stateStr,
                        @QueryParam("searchText") String searchText,
                        @Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        UserVS.State state = UserVS.State.ACTIVE;
        Date dateFrom = null;
        Date dateTo = null;
        try {state = UserVS.State.valueOf(stateStr);} catch(Exception ex) { }
        if(contentType.contains("json")) {
            Map<String, String> requestMap = null;
            try {
                requestMap = JSON.getMapper().readValue(req.getInputStream(),
                        new TypeReference<HashMap<String, String>>() {});
                try {dateFrom = DateUtils.getDateFromString(requestMap.get("searchFrom"));} catch(Exception ex) {}
                try {dateTo = DateUtils.getDateFromString(requestMap.get("searchTo"));} catch(Exception ex) {}
            } catch (Exception ex) { log.log(Level.FINE, "without json data");}
        }
        Query query = null;
        long totalCount = 0L;
        if(dateFrom != null && dateTo != null) {
            query = dao.getEM().createQuery("select g from GroupVS g where g.dateCreated between :dateFrom and :dateTo " +
                    "and (g.name =:searchText or g.state =:state or g.description =:description)")
                    .setParameter("searchText",  "%" + searchText + "%").setParameter("state", state)
                    .setParameter("dateTo", dateTo).setParameter("dateFrom", dateFrom)
                    .setFirstResult(offset).setMaxResults(max);
        } else query = dao.getEM().createQuery("select g from GroupVS g where g.name =:searchText or g.state =:state " +
                "or g.description =:searchText").setParameter("searchText", "%" + searchText + "%")
                .setParameter("state", state).setFirstResult(offset).setMaxResults(max);
        List<GroupVS> groupVSList = query.getResultList();
        List<GroupVSDto> resultList = new ArrayList<>();
        for(GroupVS groupVS : groupVSList) {
            resultList.add(groupVSBean.getGroupVSDto(groupVS));
        }
        if(dateFrom != null && dateTo != null) {
            query = dao.getEM().createQuery("select COUNT(g) from GroupVS g where g.dateCreated between :dateFrom and :dateTo " +
                    "and (g.name =:searchText or g.state =:state or g.description =:description)")
                    .setParameter("searchText",  "%" + searchText + "%").setParameter("state", state)
                    .setParameter("dateTo", dateTo).setParameter("dateFrom", dateFrom);
        } else query = dao.getEM().createQuery("select COUNT(g) from GroupVS g where g.name =:searchText or g.state =:state " +
                "or g.description =:searchText").setParameter("searchText", "%" + searchText + "%")
                .setParameter("state", state);
        totalCount = (long) query.getSingleResult();
        ResultListDto resultListDto = ResultListDto.GROUPVS(resultList, state, offset, max, totalCount);
        if(contentType.contains("json")) return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto))
                .type(MediaTypeVS.JSON).build();
        else {
            req.setAttribute("groupVSList", JSON.getMapper().writeValueAsString(resultListDto));
            context.getRequestDispatcher("/groupVS/index.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/id/{id}")
    @GET @Transactional @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") long id, @Context ServletContext context,
              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        GroupVS groupVS = dao.find(GroupVS.class, id);
        if(groupVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "GroupVS not found - groupId: " + id).build();
        GroupVSDto groupVSDto = groupVSBean.getGroupVSDto(groupVS);
        if(contentType.contains("json")) return Response.ok().entity(JSON.getMapper().writeValueAsBytes(groupVSDto)).type(MediaTypeVS.JSON).build();
        else {
            req.setAttribute("groupvsDto", JSON.getMapper().writeValueAsString(groupVSDto));
            context.getRequestDispatcher("/groupVS/groupVS.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/id/{id}/users")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response listUsers(@PathParam("id") long id,
            @DefaultValue("0") @QueryParam("offset") int offset,
            @DefaultValue("100") @QueryParam("max") int max,
            @DefaultValue("") @QueryParam("searchText") String searchText,
            @QueryParam("subscriptionState") String subscriptionStateStr,
            @QueryParam("userVSState") String userVSStateStr,
            @Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        GroupVS groupVS = dao.find(GroupVS.class, id);
        if(groupVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "GroupVS not found - groupId: " + id).build();

        SubscriptionVS.State subscriptionState = SubscriptionVS.State.ACTIVE;
        if(subscriptionStateStr != null) try {subscriptionState = SubscriptionVS.State.valueOf(
                subscriptionStateStr);} catch(Exception ex) {}
        UserVS.State userState = UserVS.State.ACTIVE;
        try {userState = UserVS.State.valueOf(userVSStateStr);} catch(Exception ex) {}
        long totalCount = 0L;
        if(contentType.contains("json")) {
            String queryListPrefix = "select s ";
            String querySufix = "from SubscriptionVS s where s.state =:subscriptionState " +
                    "and s.userVS.state =:userState and (s.userVS.name like :searchText " +
                    "or s.userVS.firstName like :searchText or s.userVS.lastName like :searchText " +
                    "or s.userVS.nif like :searchText)";
            String queryCountPrefix = "select COUNT(s) ";
            Query query = dao.getEM().createQuery(queryListPrefix + querySufix)
                    .setParameter("subscriptionState", subscriptionState)
                    .setParameter("userState", userState).setParameter("searchText", "%" + searchText + "%")
                    .setFirstResult(offset).setMaxResults(max);

            List<SubscriptionVS> userList = query.getResultList();
            List<SubscriptionVSDto> resultList = new ArrayList<>();
            for(SubscriptionVS subscriptionVS : userList) {
                resultList.add(SubscriptionVSDto.DETAILED(subscriptionVS, config.getRestURL()));
            }
            query = dao.getEM().createQuery(queryCountPrefix + querySufix)
                    .setParameter("subscriptionState", subscriptionState)
                    .setParameter("userState", userState).setParameter("searchText", "%" + searchText + "%");
            totalCount = (long) query.getSingleResult();
            ResultListDto resultListDto = new ResultListDto(resultList, offset, max, totalCount);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
        } else {
            req.setAttribute("groupVSId", groupVS.getId());
            req.setAttribute("groupVSName", groupVS.getName());
            context.getRequestDispatcher("/groupVS/listUsers.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/id/{groupId}/listUsers")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response users(@PathParam("groupId") long groupId, @Context ServletContext context,
                          @DefaultValue("0") @QueryParam("offset") int offset,
                          @DefaultValue("100") @QueryParam("max") int max,
                          @QueryParam("state") String stateStr,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        List<SubscriptionVS.State> states = Arrays.asList(SubscriptionVS.State.values());
        if(stateStr != null) try {
            states = Arrays.asList(SubscriptionVS.State.valueOf(stateStr));
        } catch(Exception ex) {}
        Query query = dao.getEM().createQuery("select s from SubscriptionVS s where s.groupVS.id =:groupId " +
                "and s.state in :states").setParameter("groupId", groupId).setParameter("states", states)
                .setFirstResult(offset).setMaxResults(max);
        List<SubscriptionVS> subscriptionVSList = query.getResultList();
        List<SubscriptionVSDto> resultList = new ArrayList<>();
        for(SubscriptionVS subscriptionVS : subscriptionVSList) {
            resultList.add(SubscriptionVSDto.DETAILED(subscriptionVS, config.getRestURL()));
        }
        query = dao.getEM().createQuery("select count(s) from SubscriptionVS s where s.groupVS.id =:groupId " +
                "and s.state in :states").setParameter("groupId", groupId).setParameter("states", states);
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, (long)query.getSingleResult());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }

    @Path("/id/{id}/balance") //old_url -> /groupVS/$id/balance"
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object balance(@PathParam("id") long id,
                            @DefaultValue("0") @QueryParam("offset") int offset,
                            @DefaultValue("100") @QueryParam("max") int max,
                            @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        GroupVS groupVS = dao.find(GroupVS.class, id);
        if(groupVS == null)  return Response.status(Response.Status.NOT_FOUND).entity(
                "GroupVS not found - groupId: " + id).build();
        BalancesDto dto = balancesBean.getBalancesDto(groupVS, DateUtils.getCurrentWeekPeriod());
        if(contentType.contains("json")) return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
        else {
            req.setAttribute("groupvsMap", JSON.getMapper().writeValueAsString(dto));
            context.getRequestDispatcher("/groupVS/groupVS.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/newGroup")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Object search(MessageSMIME messageSMIME,  @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        GroupVS groupVS = groupVSBean.saveGroup(messageSMIME);
        String URL = config.getRestURL() + "/groupVS/id/" + groupVS.getId();
        String message =  messages.get("newCurrencyGroupOKMsg", groupVS.getName());
        MessageDto messageDto = new MessageDto(ResponseVS.SC_OK, message, URL);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(messageDto)).type(MediaTypeVS.JSON).build();
    }

    @Path("/id/{id}/edit")//old_url -> /groupVS/edit/$id
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response edit(MessageSMIME messageSMIME, @PathParam("id") long id, @Context ServletContext context,
                       @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        GroupVS groupVS = dao.find(GroupVS.class, id);
        if(groupVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "GroupVS not found - groupId: " + id).build();
        groupVS = groupVSBean.editGroup(groupVS, messageSMIME);
        String URL = config.getRestURL() + "/groupVS/id/" + groupVS.getId();
        String message =  messages.get("currencyGroupEditedOKMsg", groupVS.getName());
        MessageDto messageDto = new MessageDto(ResponseVS.SC_OK, message, URL);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(messageDto)).type(MediaTypeVS.JSON).build();
    }

    @Path("/id/{id}/edit") @GET //old_url -> /groupVS/edit/$id
    public Object editForm(Map requestMap, @PathParam("id") long id, @Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        GroupVS groupVS = dao.find(GroupVS.class, id);
        if(groupVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "GroupVS not found - groupId: " + id).build();
        GroupVSDto groupVSDto = groupVSBean.getGroupVSDto(groupVS);
        req.setAttribute("groupvsMap", JSON.getMapper().writeValueAsString(groupVSDto));
        context.getRequestDispatcher("/groupVS/edit.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @Path("/id/{id}/cancel")
    @POST
    public Response cancel(MessageSMIME messageSMIME, @PathParam("id") long id, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        GroupVS groupVS = dao.find(GroupVS.class, id);
        if(groupVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "GroupVS not found - groupId: " + id).build();
        groupVS = groupVSBean.cancelGroup(groupVS, messageSMIME);
        String URL = config.getRestURL() + "/groupVS/id/" + groupVS.getId();
        String message =  messages.get("currencyGroupCancelledOKMsg", groupVS.getName());
        MessageDto messageDto = new MessageDto(ResponseVS.SC_OK, message, URL);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(messageDto)).type(MediaTypeVS.JSON).build();
    }

    @Path("/id/{id}/subscribe")
    @POST
    public Object subscribe(MessageSMIME messageSMIME, @PathParam("id") long id, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        GroupVS groupVS = dao.find(GroupVS.class, id);
        if(groupVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "GroupVS not found - groupId: " + id).build();
        UserVS signer = messageSMIME.getUserVS();
        SubscriptionVS subscriptionVS = groupVSBean.subscribe(messageSMIME);
        return Response.ok().entity(messages.get("groupvsSubscriptionOKMsg", signer.getNif(), signer.getName())).build();
    }

    @Path("/id/{groupId}/user/id/{userId}")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response user(@PathParam("groupId") long groupId, @PathParam("userId") long userId,
            @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp) 
            throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        Query query = dao.getEM().createQuery("select s from SubscriptionVS s where s.groupVS.id =:groupId " +
                "and s.userVS.id =:userId").setParameter("groupId", groupId).setParameter("userId", userId);
        SubscriptionVS subscriptionVS = dao.getSingleResult(SubscriptionVS.class, query);
        if(subscriptionVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "SubscriptionVS not found - groupId: " + groupId + " - userId: " + userId).build();
        SubscriptionVSDto dto = SubscriptionVSDto.DETAILED(subscriptionVS, config.getRestURL());
        if(contentType.contains("json")) return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto))
                .type(MediaTypeVS.JSON).build();
        else {
            req.setAttribute("subscriptionDto", JSON.getMapper().writeValueAsString(dto));
            context.getRequestDispatcher("/groupVS/user.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/activateUser")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Object activateUser(MessageSMIME messageSMIME, @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        SubscriptionVS subscriptionVS = subscriptionVSBean.activateUser(messageSMIME);
        currencyAccountBean.checkUserVSAccount(subscriptionVS.getUserVS());
        Map resultMap = new HashMap<>();
        resultMap.put("statusCode", ResponseVS.SC_OK);
        resultMap.put("message", messages.get("currencyGroupUserActivatedMsg", subscriptionVS.getUserVS().getNif(),
                subscriptionVS.getGroupVS().getName()));
        return resultMap;
    }

    @Path("/deActivateUser")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Object deActivateUser(MessageSMIME messageSMIME, @Context ServletContext context,
                               @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        SubscriptionVS subscriptionVS = subscriptionVSBean.deActivateUser(messageSMIME);
        Map resultMap = new HashMap<>();
        resultMap.put("statusCode", ResponseVS.SC_OK);
        resultMap.put("message", messages.get("currencyGroupUserdeActivatedMsg", subscriptionVS.getUserVS().getNif(),
                subscriptionVS.getGroupVS().getName()));
        return resultMap;
    }

}