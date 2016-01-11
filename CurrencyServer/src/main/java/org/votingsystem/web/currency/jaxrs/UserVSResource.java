package org.votingsystem.web.currency.jaxrs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.iban4j.Iban;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.BankVSInfo;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Interval;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.currency.ejb.*;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Path("/userVS")
public class UserVSResource {

    private static final Logger log = Logger.getLogger(UserVSResource.class.getSimpleName());

    @Inject TransactionVSBean transactionVSBean;
    @Inject GroupVSBean groupVSBean;
    @Inject BalancesBean balancesBean;
    @Inject UserVSBean userVSBean;
    @Inject BankVSBean bankVSBean;
    @Inject SignatureBean signatureBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;

    @Path("/") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response indexJSON(@DefaultValue("0") @QueryParam("offset") int offset,
                        @DefaultValue("100") @QueryParam("max") int max,
                        @QueryParam("searchText") String searchText,
                        @Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws Exception {
        UserVS.Type userType = UserVS.Type.USER;
        UserVS.State userState = UserVS.State.ACTIVE;
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        Date dateFrom = null;
        Date dateTo = null;
        try {userType = UserVS.Type.valueOf(req.getParameter("type"));} catch(Exception ex) {}
        try {userState = UserVS.State.valueOf(req.getParameter("state"));} catch(Exception ex) {}
        if(req.getParameter("searchFrom") != null) try {dateFrom = DateUtils.getDateFromString(
                req.getParameter("searchFrom"));} catch(Exception ex) { dateFrom = timePeriod.getDateFrom();}
        if(req.getParameter("searchTo") != null) try {dateTo = DateUtils.getDateFromString(
                req.getParameter("searchTo"));} catch(Exception ex) {dateTo = timePeriod.getDateTo();}
        Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(UserVS.class);
        criteria.add(Restrictions.eq("type", userType));
        criteria.add(Restrictions.eq("state", userState));
        if(searchText != null) {
            Criterion rest1= Restrictions.ilike("name", "%" + searchText + "%");
            Criterion rest2= Restrictions.ilike("firstName", "%" + searchText + "%");
            Criterion rest3= Restrictions.ilike("lastName", "%" + searchText + "%");
            Criterion rest4= Restrictions.ilike("nif", "%" + searchText + "%");
            Criterion rest5= Restrictions.ilike("IBAN", "%" + searchText + "%");
            criteria.add(Restrictions.or(rest1, rest2, rest3, rest4, rest5));
        }
        if(dateFrom != null && dateTo != null) {
            criteria.add(Restrictions.between("dateCreated", dateFrom, dateTo));
        }
        List<UserVS> userList = criteria.setFirstResult(offset).setMaxResults(max).list();
        List<UserVSDto> resultList = new ArrayList<>();
        for(UserVS userVS :  userList) {
            resultList.add(userVSBean.getUserVSDto(userVS, false));
        }
        long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaTypeVS.JSON).build();
    }


    @Path("/IBAN/{IBAN}")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response findByIBAN(@PathParam("IBAN") String IBAN, @Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", IBAN);
        UserVS userVS = dao.getSingleResult(UserVS.class, query);
        String msg = null;
        if(userVS == null) {//check if external bank client
            Iban iban = Iban.valueOf(IBAN);
            query = dao.getEM().createQuery("select bi from BankVSInfo bi where bi.bankCode =:bankCode")
                    .setParameter("bankCode", iban.getBankCode());
            BankVSInfo bankVSInfo = dao.getSingleResult(BankVSInfo.class, query);
            if(bankVSInfo != null) {
                userVS = bankVSInfo.getBankVS();
                msg = messages.get("ibanFromBankVSClientMsg", IBAN, bankVSInfo.getBankVS().getName());
            }
        }
        if(userVS == null) return Response.status(Response.Status.NOT_FOUND)
                .entity(messages.get("itemNotFoundByIBANMsg", IBAN)).build();
        return processUserVSResult(userVS, msg, req, resp, context);
    }

    @Path("/id/{id}")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response index(@PathParam("id") long id, @Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS userVS = dao.find(UserVS.class, id);
        if(userVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                messages.get("itemNotFoundMsg", Long.valueOf(id).toString())).build();
        else return processUserVSResult(userVS, null, req, resp, context);
    }

    private Response processUserVSResult(UserVS userVS, String msg,
               HttpServletRequest req,  HttpServletResponse resp, ServletContext context) throws Exception {
        Object resultDto = null;
        if(userVS instanceof GroupVS) {
            resultDto = groupVSBean.getGroupVSDto((GroupVS) userVS);
        } else if(userVS instanceof BankVS) {
            resultDto = UserVSDto.COMPLETE(userVS);
            ((UserVSDto)resultDto).setMessage(msg);
        } else resultDto = UserVSDto.COMPLETE(userVS);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultDto)).build() ;
    }

    @Path("/search")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response searchHTML(@DefaultValue("0") @QueryParam("offset") int offset,
             @DefaultValue("100") @QueryParam("max") int max, @QueryParam("searchText") String searchText,
            @Context ServletContext context, @Context HttpServletRequest req,
                         @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        if(contentType.contains("json")) {
            return processSearch(searchText, offset, max);
        } else return Response.temporaryRedirect(new URI("../userVS/search.xhtml")).build();
    }

    private Response processSearch(String searchText, int offset, int max) throws Exception {
        Query query = dao.getEM().createQuery("select u from UserVS u where u.state =:state and u.type =:type " +
                "and (lower(u.name) like :searchText or lower(u.firstName) like :searchText or lower(u.lastName) like :searchText " +
                "or lower(u.nif) like :searchText)").setParameter("type", UserVS.Type.USER)
                .setParameter("state", UserVS.State.ACTIVE)
                .setParameter("searchText", "%" + searchText.toLowerCase() + "%")
                .setFirstResult(offset).setMaxResults(max);
        List<UserVS> userVSList = query.getResultList();
        List<UserVSDto> resultList = new ArrayList<>();
        for(UserVS userVS : userVSList) {
            resultList.add(userVSBean.getUserVSDto(userVS, false));
        }
        query = dao.getEM().createQuery("SELECT COUNT(u) FROM UserVS u where u.state =:state and u.type =:type " +
                "and (u.name like :searchText or u.firstName like :searchText or u.lastName like :searchText " +
                "or u.nif like :searchText)").setParameter("type", UserVS.Type.USER)
                .setParameter("state", UserVS.State.ACTIVE)
                .setParameter("searchText", "%" + searchText + "%");
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, (long)query.getSingleResult());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaTypeVS.JSON).build();
    }


    @Path("/search/group/{groupId}")
    @POST @Produces(MediaType.APPLICATION_JSON)
    @Consumes({"application/json"})
    public Response searchGroup(@DefaultValue("0") @QueryParam("offset") int offset,
            @DefaultValue("100") @QueryParam("max") int max, @QueryParam("searchText") String searchText,
            @QueryParam("groupVSState") String groupVSState,
            @PathParam("groupId") long groupId, @Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        GroupVS groupVS = dao.find(GroupVS.class, groupId);
        if(groupVS == null) return Response.status(Response.Status.NOT_FOUND).entity("not found - groupId: " + groupId).build();
        if(searchText != null) {
            SubscriptionVS.State state = SubscriptionVS.State.ACTIVE;
            try {state = SubscriptionVS.State.valueOf(groupVSState);} catch(Exception ex) {}
            Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(SubscriptionVS.class).createAlias("userVS", "user");;
            criteria.add(Restrictions.eq("groupVS", groupVS));
            criteria.add(Restrictions.eq("state", state));
            criteria.add(Restrictions.eq("user.state", UserVS.State.ACTIVE));
            if(searchText != null) {
                Criterion rest1= Restrictions.ilike("user.name", "%" + searchText + "%");
                Criterion rest2= Restrictions.ilike("user.firstName", "%" + searchText + "%");
                Criterion rest3= Restrictions.ilike("user.lastName", "%" + searchText + "%");
                Criterion rest4= Restrictions.ilike("user.nif", "%" + searchText + "%");
                criteria.add(Restrictions.or(rest1, rest2, rest3, rest4));
            }
            List<SubscriptionVS> subscriptionList = criteria.setFirstResult(offset).setMaxResults(max).list();
            List<UserVSDto> resultList = new ArrayList<>();
            for(SubscriptionVS subscriptionVS : subscriptionList) {
                resultList.add(userVSBean.getUserVSDto(subscriptionVS.getUserVS(), false));
            }
            long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
            ResultListDto resultListDto = new ResultListDto(resultList, offset, max, totalCount);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
        } else return Response.status(Response.Status.BAD_REQUEST).entity("missing 'searchText'").build();
    }

    @Path("/nif/{nif}/{year}/{month}/{day}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response userInfo(@PathParam("nif") String nif,
                           @PathParam("year") int year,
                           @PathParam("month") int month,
                           @PathParam("day") int day,
                           @Context ServletContext context, @Context HttpServletRequest req,
                           @Context HttpServletResponse resp) throws Exception {
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", nif);
        UserVS userVS = dao.getSingleResult(UserVS.class, query);
        if(userVS == null) return Response.status(Response.Status.NOT_FOUND).entity("not found - nif: " + nif).build();
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        Interval timePeriod = DateUtils.getWeekPeriod(calendar);
        BalancesDto dto = balancesBean.getBalancesDto(userVS, timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Path("/nif/{nif}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response userInfoByNif(@PathParam("nif") String nif,
                           @Context ServletContext context, @Context HttpServletRequest req,
                           @Context HttpServletResponse resp) throws Exception {
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", nif);
        UserVS userVS = dao.getSingleResult(UserVS.class, query);
        if(userVS == null) return Response.status(Response.Status.NOT_FOUND).entity("not found - nif: " + nif).build();
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        BalancesDto dto = balancesBean.getBalancesDto(userVS, timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Path("/searchByDevice")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object searchByDevice(@QueryParam("phone") String phone, @QueryParam("email") String email,
                                 @Context HttpServletRequest req) throws Exception {
        if(phone == null && email == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("missing 'email' or 'phone'").build();
        }
        Query query = dao.getEM().createQuery("select d from DeviceVS d where d.email =:email or d.phone =:phone")
                .setParameter("email", email).setParameter("phone", phone);
        DeviceVS deviceVS = dao.getSingleResult(DeviceVS.class, query);
        if(deviceVS == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("device not found - email: " + email +
                    " - phone: " + phone).build();
        } else return userVSBean.getUserVSDto(deviceVS.getUserVS(), false);
    }

    @Path("/bankVSList")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response bankVSList(@Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        List<BankVS> bankVSList = dao.findAll(BankVS.class);
        List<UserVSDto> resultList = new ArrayList<>();
        for(BankVS bankVS :  bankVSList) {
            resultList.add(userVSBean.getUserVSDto(bankVS, false));
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultList)).build();
    }

    @Path("/userInfoTest")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response userInfoTest(MessageSMIME messageSMIME, @Context HttpServletRequest req, @Context
        HttpServletResponse resp) throws Exception {
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        Map<String, Object> dataMap = JSON.getMapper().readValue(smimeMessage.getSignedContent(), Map.class);
        //TODO check operation
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        BalancesDto dto = balancesBean.getBalancesDto(messageSMIME.getUserVS(), timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Path("/save")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response save(MessageSMIME messageSMIME, @Context HttpServletRequest req) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS newUser = userVSBean.saveUser(messageSMIME);
        MessageDto messageDto = MessageDto.OK(messages.get("certUserNewMsg", newUser.getNif()),
                config.getContextURL() + "/rest/userVS/id/" + newUser.getId());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(messageDto)).build();
    }

    @Path("/newBankVS")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response newBankVS(MessageSMIME messageSMIME, @Context HttpServletRequest req) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        BankVS newBankVS = bankVSBean.saveBankVS(messageSMIME);
        MessageDto messageDto = new MessageDto(ResponseVS.SC_OK,
                messages.get("newBankVSOKMsg", newBankVS.getCertificate().getSubjectDN().toString()),
                config.getContextURL() + "/rest/userVS/id/" + newBankVS.getId());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(messageDto)).type(MediaTypeVS.JSON).build();
    }

    @Path("/connected")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response connected(@Context HttpServletRequest req) throws Exception {
        //TODO
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(
                SessionVSManager.getInstance().getConnectedUsersDto())).build();
    }

}