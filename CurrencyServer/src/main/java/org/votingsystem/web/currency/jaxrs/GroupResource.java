package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.GroupDto;
import org.votingsystem.dto.currency.SubscriptionDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Group;
import org.votingsystem.model.currency.Subscription;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.web.currency.ejb.BalancesBean;
import org.votingsystem.web.currency.ejb.CurrencyAccountBean;
import org.votingsystem.web.currency.ejb.GroupBean;
import org.votingsystem.web.currency.ejb.UserBean;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/group")
public class GroupResource {

    private static final Logger log = Logger.getLogger(GroupResource.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject CurrencyAccountBean currencyAccountBean;
    @Inject GroupBean groupBean;
    @Inject UserBean userBean;
    @Inject BalancesBean balancesBean;
    @Inject CMSBean cmsBean;
    @Inject SubscriptionBean subscriptionBean;

    @Path("/")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response index(@DefaultValue("0") @QueryParam("offset") int offset,
                        @DefaultValue("100") @QueryParam("max") int max,
                        @QueryParam("state") String stateStr,
                        @DefaultValue("") @QueryParam("searchText") String searchText,
                        @Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        User.State state = User.State.ACTIVE;
        Date dateFrom = null;
        Date dateTo = null;
        try {state = User.State.valueOf(stateStr);} catch(Exception ex) { }
        if(contentType.contains("json")) {
            Map<String, String> requestMap = null;
            try {
                requestMap = JSON.getMapper().readValue(req.getInputStream(),
                        new TypeReference<HashMap<String, String>>() {});
                try {dateFrom = DateUtils.getDateFromString(requestMap.get("searchFrom"));} catch(Exception ex) {}
                try {dateTo = DateUtils.getDateFromString(requestMap.get("searchTo"));} catch(Exception ex) {}
            } catch (Exception ex) { log.log(Level.FINE, "without json data");}
        }
        Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(Group.class);
        criteria.add(Restrictions.eq("state", state));
        if(searchText != null) {
            Criterion rest1= Restrictions.ilike("name", "%" + searchText + "%");
            Criterion rest2= Restrictions.ilike("description", "%" + searchText + "%");
            criteria.add(Restrictions.or(rest1, rest2));
        }
        if(dateFrom != null && dateTo != null) {
            criteria.add(Restrictions.between("dateCreated", dateFrom, dateTo));
        }
        List<Group> groupList = criteria.setFirstResult(offset).setMaxResults(max).list();
        List<GroupDto> resultList = new ArrayList<>();
        for(Group group : groupList) {
            resultList.add(groupBean.getGroupDto(group));
        }
        long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
        ResultListDto resultListDto = ResultListDto.GROUP(resultList, state, offset, max, totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto))
                .type(MediaType.JSON).build();
    }

    @Path("/id/{id}")
    @GET @Transactional @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") long id, @Context ServletContext context,
              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        Group group = dao.find(Group.class, id);
        if(group == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "Group not found - groupId: " + id).build();
        GroupDto groupDto = groupBean.getGroupDto(group);
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(groupDto)).type(MediaType.JSON).build();
        } else {
            return Response.temporaryRedirect(new URI("../spa.xhtml#!/CurrencyServer/rest/group/id/" + group.getId())).build();
        }
    }

    @Path("/id/{id}/searchUsers")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response searchUsers(@PathParam("id") long id,
            @DefaultValue("0") @QueryParam("offset") int offset,
            @DefaultValue("100") @QueryParam("max") int max,
            @QueryParam("searchText") String searchText,
            @QueryParam("subscriptionState") String subscriptionStateStr,
            @QueryParam("userState") String userStateStr,
            @Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        Group group = dao.find(Group.class, id);
        if(group == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "Group not found - groupId: " + id).build();

        Subscription.State subscriptionState = Subscription.State.ACTIVE;
        if(subscriptionStateStr != null) try {subscriptionState = Subscription.State.valueOf(
                subscriptionStateStr);} catch(Exception ex) {}
        User.State userState = User.State.ACTIVE;
        try {userState = User.State.valueOf(userStateStr);} catch(Exception ex) {}
        Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(Subscription.class)
                .createAlias("user", "user");
        criteria.add(Restrictions.eq("state", subscriptionState));
        criteria.add(Restrictions.eq("group", group));
        criteria.add(Restrictions.eq("user.state", userState));
        if(searchText != null) {
            Criterion rest1= Restrictions.ilike("user.name", "%" + searchText + "%");
            Criterion rest2= Restrictions.ilike("user.firstName", "%" + searchText + "%");
            Criterion rest3= Restrictions.ilike("user.lastName", "%" + searchText + "%");
            Criterion rest4= Restrictions.ilike("user.nif", "%" + searchText + "%");
            criteria.add(Restrictions.or(rest1, rest2, rest3, rest4));
        }
        List<Subscription> userList = criteria.setFirstResult(offset).setMaxResults(max).list();
        List<UserDto> resultList = new ArrayList<>();
        for(Subscription subscription : userList) {
            resultList.add(UserDto.COMPLETE(subscription.getUser()));
        }
        long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
        ResultListDto resultListDto = new ResultListDto(resultList, offset, resultList.size(), totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }

    @Path("/id/{groupId}/listUsers")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response listUsers(@PathParam("groupId") long groupId, @Context ServletContext context,
                          @DefaultValue("0") @QueryParam("offset") int offset,
                          @DefaultValue("100") @QueryParam("max") int max,
                          @QueryParam("state") String stateStr,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        List<Subscription.State> states = Arrays.asList(Subscription.State.values());
        if(stateStr != null) try {
            states = Arrays.asList(Subscription.State.valueOf(stateStr));
        } catch(Exception ex) {}
        Query query = dao.getEM().createQuery("select s from Subscription s where s.group.id =:groupId " +
                "and s.state in :states").setParameter("groupId", groupId).setParameter("states", states)
                .setFirstResult(offset).setMaxResults(max);
        List<Subscription> subscriptionList = query.getResultList();
        List<SubscriptionDto> resultList = new ArrayList<>();
        for(Subscription subscription : subscriptionList) {
            resultList.add(SubscriptionDto.DETAILED(subscription, config.getContextURL()));
        }
        query = dao.getEM().createQuery("select count(s) from Subscription s where s.group.id =:groupId " +
                "and s.state in :states").setParameter("groupId", groupId).setParameter("states", states);
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, (long)query.getSingleResult());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }

    @Path("/id/{id}/balance")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Object balance(@PathParam("id") long id,
                            @DefaultValue("0") @QueryParam("offset") int offset,
                            @DefaultValue("100") @QueryParam("max") int max,
                            @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        Group group = dao.find(Group.class, id);
        if(group == null)  return Response.status(Response.Status.NOT_FOUND).entity(
                "Group not found - groupId: " + id).build();
        BalancesDto dto = balancesBean.getBalancesDto(group, DateUtils.getCurrentWeekPeriod());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Transactional
    @Path("/saveGroup")
    @POST @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Object saveGroup(CMSMessage cmsMessage, @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        Group group = groupBean.saveGroup(cmsMessage);
        GroupDto dto = GroupDto.DETAILS(group, null);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).type(MediaType.JSON).build();
    }


    @Path("/id/{id}/cancel")
    @POST @Transactional
    public Response cancel(CMSMessage cmsMessage, @PathParam("id") long id, @Context ServletContext context,
                           @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Group group = dao.find(Group.class, id);
        if(group == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "Group not found - groupId: " + id).build();
        group = groupBean.cancelGroup(group, cmsMessage);
        String URL = config.getContextURL() + "/rest/group/id/" + group.getId();
        String message =  messages.get("currencyGroupCancelledOKMsg", group.getName());
        MessageDto messageDto = new MessageDto(ResponseVS.SC_OK, message, URL);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(messageDto)).type(MediaType.JSON).build();
    }

    @Path("/id/{id}/subscribe")
    @POST
    public Object subscribe(CMSMessage cmsMessage, @PathParam("id") long id, @Context ServletContext context,
                            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Group group = dao.find(Group.class, id);
        if(group == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "Group not found - groupId: " + id).build();
        User signer = cmsMessage.getUser();
        Subscription subscription = groupBean.subscribe(cmsMessage);
        return Response.ok().entity(messages.get("groupSubscriptionOKMsg", signer.getNif(), signer.getName())).build();
    }

    @Path("/id/{groupId}/user/id/{userId}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response user(@PathParam("groupId") long groupId, @PathParam("userId") long userId,
            @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp) 
            throws Exception {
        Query query = dao.getEM().createQuery("select s from Subscription s where s.group.id =:groupId " +
                "and s.user.id =:userId").setParameter("groupId", groupId).setParameter("userId", userId);
        Subscription subscription = dao.getSingleResult(Subscription.class, query);
        if(subscription == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "Subscription not found - groupId: " + groupId + " - userId: " + userId).build();
        SubscriptionDto dto = SubscriptionDto.DETAILED(subscription, config.getContextURL());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto))
                .type(MediaType.JSON).build();
    }

    @Path("/activateUser")
    @POST @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response activateUser(CMSMessage cmsMessage, @Context ServletContext context,
                                 @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Subscription subscription = subscriptionBean.activateUser(cmsMessage);
        currencyAccountBean.checkUserAccount(subscription.getUser());
        MessageDto dto = MessageDto.OK(messages.get("currencyGroupUserActivatedMsg", subscription.getUser().getNif(),
                subscription.getGroup().getName()), null);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).type(MediaType.JSON).build();
    }

    @Path("/deActivateUser")
    @POST @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response deActivateUser(CMSMessage cmsMessage, @Context ServletContext context,
                                   @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Subscription subscription = subscriptionBean.deActivateUser(cmsMessage);
        MessageDto dto = MessageDto.OK(messages.get("currencyGroupUserdeActivatedMsg", subscription.getUser().getNif(),
                subscription.getGroup().getName()), null);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).type(MediaType.JSON).build();
    }

}