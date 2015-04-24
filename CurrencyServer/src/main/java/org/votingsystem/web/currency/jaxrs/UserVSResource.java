package org.votingsystem.web.currency.jaxrs;

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
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.currency.ejb.*;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.MessagesBean;
import org.votingsystem.web.ejb.SignatureBean;

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
    @Inject MessagesBean messages;

    @Path("/") @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response indexJSON(@DefaultValue("0") @QueryParam("offset") int offset,
                         @DefaultValue("100") @QueryParam("max") int max,
                         @Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws Exception {
        /*Map sortParamsMap = RequestUtils.getSortParamsMap(params);
        Map.Entry sortParam
        if(!sortParamsMap.isEmpty()) sortParam = sortParamsMap?.entrySet()?.iterator()?.next()*/
        List<UserVS> userList = null;
        Long totalCount = 0L;
        if(req.getParameter("searchText") != null || req.getParameter("searchFrom") != null ||
                req.getParameter("searchTo") != null || req.getParameter("type") != null ||
                req.getParameter("state") != null) {
            UserVS.Type userType = UserVS.Type.USER;
            UserVS.State userState = UserVS.State.ACTIVE;
            TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
            Date dateFrom = null;
            Date dateTo = null;
            try {userType = UserVS.Type.valueOf(req.getParameter("type"));} catch(Exception ex) {}
            try {userState = UserVS.State.valueOf(req.getParameter("state"));} catch(Exception ex) {}
            //searchFrom:2014/04/14 00:00:00, max:100, searchTo
            if(req.getParameter("searchFrom") != null) try {dateFrom = DateUtils.getDateFromString(
                    req.getParameter("searchFrom"));} catch(Exception ex) { dateFrom = timePeriod.getDateFrom();}
            if(req.getParameter("searchTo") != null) try {dateTo = DateUtils.getDateFromString(
                    req.getParameter("searchTo"));} catch(Exception ex) {dateTo = timePeriod.getDateTo();}
            String queryListPrefix = "select u from UserVS u";
            String querySufix = " where u.dateCreated between :dateFrom " +
                    "and :dateTo and u.type =:type and u.state =:state and (u.name like :searchText " +
                    "or u.firstName like :searchText or u.lastName like :searchText  or u.nif like :searchText " +
                    "or u.IBAN like :searchText)";
            Query query = dao.getEM().createQuery(queryListPrefix + querySufix)
                    .setParameter("searchText", "%" + req.getParameter("searchText") + "%")
                    .setParameter("state", userState).setParameter("type", userType).setParameter("dateFrom", dateFrom)
                    .setParameter("dateTo", dateTo);
            userList = query.setFirstResult(offset).setMaxResults(max).getResultList();
            String queryCountPrefix = "select COUNT(u) from UserVS u";
            query = dao.getEM().createQuery(queryCountPrefix + querySufix)
                    .setParameter("searchText", "%" + req.getParameter("searchText") + "%")
                    .setParameter("state", userState).setParameter("type", userType).setParameter("dateFrom", dateFrom)
                    .setParameter("dateTo", dateTo);
            totalCount = (long)query.getSingleResult();
        } else {
            String queryListPrefix = "select u ";
            String querySufix = "from UserVS u";
            String queryCountPrefix = "select COUNT(u) ";
            Query query = dao.getEM().createQuery(queryListPrefix + querySufix);
            userList = query.setMaxResults(max).setFirstResult(offset).getResultList();
            totalCount = (Long) dao.getEM().createQuery(queryCountPrefix + querySufix).getSingleResult();

        }
        List<UserVSDto> resultList = new ArrayList<>();
        for(UserVS userVS :  userList) {
            resultList.add(userVSBean.getUserVSDto(userVS, false));
        }
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaTypeVS.JSON).build();
    }


    @Path("/IBAN/{IBAN}")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Object findByIBAN(@PathParam("IBAN") String IBAN, @Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
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
        UserVS userVS = dao.find(UserVS.class, id);
        if(userVS == null) return Response.status(Response.Status.NOT_FOUND).entity(
                messages.get("itemNotFoundMsg", Long.valueOf(id).toString())).build();
        else return processUserVSResult(userVS, null, req, resp, context);
    }

    private Response processUserVSResult(UserVS userVS, String msg,
               HttpServletRequest req,  HttpServletResponse resp, ServletContext context) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        Object resultDto = null;
        String view = null;
        if(userVS instanceof GroupVS) {
            resultDto = groupVSBean.getGroupVSDto((GroupVS) userVS);
            req.setAttribute("groupvsDto", JSON.getMapper().writeValueAsString(resultDto));
            view = "/groupVS/groupVS.xhtml";
        }
        else if(userVS instanceof BankVS) {
            resultDto = UserVSDto.COMPLETE(userVS);
            req.setAttribute("uservsDto", JSON.getMapper().writeValueAsString(resultDto));
            req.setAttribute("messageToUser", msg);
            view = "/userVS/userVS.xhtml";
        } else {
            resultDto = UserVSDto.COMPLETE(userVS);
            req.setAttribute("uservsDto", JSON.getMapper().writeValueAsString(resultDto));
            view = "/userVS/userVS.xhtml";
        }
        if(contentType.contains("json")) {
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultDto)).build() ;
        } else {
            context.getRequestDispatcher(view).forward(req, resp);
            return Response.ok().build();
        }
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
        } else {
            context.getRequestDispatcher("/userVS/search.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/search")
    @POST @Produces(MediaType.APPLICATION_JSON)
    @Consumes({"application/json"})
    public Response search(Map requestMap, @Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws Exception {
        if(requestMap.containsKey("searchText")) {
            int offset = ((Number)requestMap.get("offset")).intValue();
            int max = ((Number)requestMap.get("max")).intValue();
            return processSearch((String) requestMap.get("searchText"), offset, max);
        } else return Response.status(Response.Status.BAD_REQUEST).entity("missing 'searchText'").build();
    }

    private Response processSearch(String searchText, int offset, int max) throws Exception {
        Query query = dao.getEM().createQuery("select u from UserVS u where u.state =:state and u.type =:type " +
                "and (u.name like :searchText or u.firstName like :searchText or u.lastName like :searchText " +
                "or u.nif like :searchText)").setParameter("type", UserVS.Type.USER)
                .setParameter("state", UserVS.State.ACTIVE)
                .setParameter("searchText", "%" + searchText + "%")
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
    public Response searchGroup(Map requestMap, @PathParam("groupId") long groupId, @Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        GroupVS groupVS = dao.find(GroupVS.class, groupId);
        if(groupVS == null) return Response.status(Response.Status.NOT_FOUND).entity("not found - groupId: " + groupId).build();
        if(requestMap.containsKey("searchText")) {
            int offset = ((Number)requestMap.get("offset")).intValue();
            int max = ((Number)requestMap.get("max")).intValue();
            SubscriptionVS.State state = SubscriptionVS.State.ACTIVE;
            try {state = SubscriptionVS.State.valueOf((String) requestMap.get("groupVSState"));} catch(Exception ex) {}
            String queryListPrefix = "select s from SubscriptionVS s ";
            String querySufix = "where s.groupVS =:groupVS " +
                    "and s.state =: state and s.userVS.state = :userState and (s.userVS.name like :searchText " +
                    "or s.userVS.firstName like :searchText or s.userVS.lastName like :searchText or s.userVS.nif like :searchText)";
            Query query = dao.getEM().createQuery(queryListPrefix + querySufix)
                    .setFirstResult(offset).setMaxResults(max).setParameter("param", "s").setParameter("state", state)
                    .setParameter("userState", UserVS.State.ACTIVE).setParameter("groupVS", groupVS)
                    .setParameter("searchText", "%" + requestMap.get("searchText") + "%");
            List<SubscriptionVS> subscriptionList = query.setFirstResult(offset).setMaxResults(max).getResultList();
            String queryCountPrefix = "select COUNT(s) from SubscriptionVS s ";
            query = dao.getEM().createQuery(queryCountPrefix + querySufix)
                    .setFirstResult(offset).setMaxResults(max).setParameter("param", "s").setParameter("state", state)
                    .setParameter("userState", UserVS.State.ACTIVE).setParameter("groupVS", groupVS)
                    .setParameter("searchText", "%" + requestMap.get("searchText") + "%");
            List<UserVSDto> resultList = new ArrayList<>();
            for(SubscriptionVS subscriptionVS : subscriptionList) {
                resultList.add(userVSBean.getUserVSDto(subscriptionVS.getUserVS(), false));
            }
            ResultListDto resultListDto = new ResultListDto(resultList, offset, max, (long)query.getSingleResult());
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
        TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar);
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
        TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
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
        String contentType = req.getContentType() != null ? req.getContentType():"";
        List<BankVS> bankVSList = dao.findAll(BankVS.class);
        List<UserVSDto> resultList = new ArrayList<>();
        for(BankVS bankVS :  bankVSList) {
            resultList.add(userVSBean.getUserVSDto(bankVS, false));
        }
        if(contentType.contains("json")) return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultList)).build();
        else {
            req.setAttribute("bankVSListDto", JSON.getMapper().writeValueAsString(resultList));
            context.getRequestDispatcher("/userVS/bankVSList.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/userInfoTest")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response userInfoTest(MessageSMIME messageSMIME, @Context HttpServletRequest req, @Context
        HttpServletResponse resp) throws Exception {
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        Map<String, Object> dataMap = JSON.getMapper().readValue(smimeMessage.getSignedContent(), Map.class);
        //TODO check operation
        TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        BalancesDto dto = balancesBean.getBalancesDto(messageSMIME.getUserVS(), timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Path("/save") @GET
    public Object saveForm(@Context ServletContext context, @Context HttpServletRequest req,
                           @Context HttpServletResponse resp) throws ServletException, IOException {
        context.getRequestDispatcher("/userVS/newUser.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @Path("/save")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response save(MessageSMIME messageSMIME, @Context HttpServletRequest req) throws Exception {
        UserVS newUser = userVSBean.saveUser(messageSMIME);
        MessageDto messageDto = MessageDto.OK(messages.get("certUserNewMsg", newUser.getNif()),
                config.getContextURL() + "/rest/userVS/id/" + newUser.getId());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(messageDto)).build();
    }

    @Path("/newBankVS")
    @POST @Produces(MediaType.APPLICATION_JSON)
    public Response newBankVS(MessageSMIME messageSMIME, @Context HttpServletRequest req) throws Exception {
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