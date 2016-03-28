package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.iban4j.Iban;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.Device;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.BankInfo;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Interval;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaType;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.currency.ejb.BalancesBean;
import org.votingsystem.web.currency.ejb.BankBean;
import org.votingsystem.web.currency.ejb.TransactionBean;
import org.votingsystem.web.currency.ejb.UserBean;
import org.votingsystem.web.currency.websocket.SessionManager;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
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
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Path("/user")
public class UserResource {

    private static final Logger log = Logger.getLogger(UserResource.class.getName());

    @Inject TransactionBean transactionBean;
    @Inject BalancesBean balancesBean;
    @Inject UserBean userBean;
    @Inject BankBean bankBean;
    @Inject CMSBean cmsBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;

    @Path("/") @GET
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response indexJSON(@DefaultValue("0") @QueryParam("offset") int offset,
                        @DefaultValue("100") @QueryParam("max") int max,
                        @QueryParam("searchText") String searchText,
                        @Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws Exception {
        User.Type userType = User.Type.USER;
        User.State userState = User.State.ACTIVE;
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        Date dateFrom = null;
        Date dateTo = null;
        try {userType = User.Type.valueOf(req.getParameter("type"));} catch(Exception ex) {}
        try {userState = User.State.valueOf(req.getParameter("state"));} catch(Exception ex) {}
        if(req.getParameter("searchFrom") != null) try {dateFrom = DateUtils.getDateFromString(
                req.getParameter("searchFrom"));} catch(Exception ex) { dateFrom = timePeriod.getDateFrom();}
        if(req.getParameter("searchTo") != null) try {dateTo = DateUtils.getDateFromString(
                req.getParameter("searchTo"));} catch(Exception ex) {dateTo = timePeriod.getDateTo();}
        Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(User.class);
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
        List<User> userList = criteria.setFirstResult(offset).setMaxResults(max).list();
        List<UserDto> resultList = new ArrayList<>();
        for(User user :  userList) {
            resultList.add(userBean.getUserDto(user, false));
        }
        criteria.setFirstResult(0); //reset offset for total count
        long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaType.JSON).build();
    }


    @Path("/IBAN/{IBAN}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response findByIBAN(@PathParam("IBAN") String IBAN, @Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", IBAN);
        User user = dao.getSingleResult(User.class, query);
        String msg = null;
        if(user == null) {//check if external bank client
            Iban iban = Iban.valueOf(IBAN);
            query = dao.getEM().createQuery("select bi from BankInfo bi where bi.bankCode =:bankCode")
                    .setParameter("bankCode", iban.getBankCode());
            BankInfo bankInfo = dao.getSingleResult(BankInfo.class, query);
            if(bankInfo != null) {
                user = bankInfo.getBank();
                msg = messages.get("ibanFromBankClientMsg", IBAN, bankInfo.getBank().getName());
            }
        }
        if(user == null) return Response.status(Response.Status.NOT_FOUND)
                .entity(messages.get("itemNotFoundByIBANMsg", IBAN)).build();
        return processUserResult(user, msg);
    }

    @Path("/id/{id}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response index(@PathParam("id") long id,
                @DefaultValue("false") @QueryParam("connectedDevices") Boolean connectedDevices,
                @Context ServletContext context, @Context HttpServletRequest req,
                @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        User user = dao.find(User.class, id);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                messages.get("objectNotFoundMsg", Long.valueOf(id).toString())).build();
        if(connectedDevices) {
            UserDto resultDto = userBean.getUserDto(user, false);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultDto)).build() ;
        } else return processUserResult(user, null);
    }

    private Response processUserResult(User user, String msg) throws Exception {
        Object resultDto = null;
        if(user instanceof Bank) {
            resultDto = UserDto.COMPLETE(user);
            ((UserDto)resultDto).setMessage(msg);
        } else resultDto = UserDto.COMPLETE(user);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultDto)).build() ;
    }

    @Path("/search")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response searchHTML(@DefaultValue("0") @QueryParam("offset") int offset,
             @DefaultValue("100") @QueryParam("max") int max, @QueryParam("searchText") String searchText,
            @Context ServletContext context, @Context HttpServletRequest req,
                         @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        if(contentType.contains("json")) {
            return processSearch(searchText, offset, max);
        } else return Response.temporaryRedirect(new URI("../user/search.xhtml")).build();
    }

    private Response processSearch(String searchText, int offset, int max) throws Exception {
        Query query = dao.getEM().createQuery("select u from User u where u.state =:state and u.type =:type " +
                "and (lower(u.name) like :searchText or lower(u.firstName) like :searchText or lower(u.lastName) like :searchText " +
                "or lower(u.nif) like :searchText)").setParameter("type", User.Type.USER)
                .setParameter("state", User.State.ACTIVE)
                .setParameter("searchText", "%" + searchText.toLowerCase() + "%")
                .setFirstResult(offset).setMaxResults(max);
        List<User> userList = query.getResultList();
        List<UserDto> resultList = new ArrayList<>();
        for(User user : userList) {
            resultList.add(userBean.getUserDto(user, false));
        }
        query = dao.getEM().createQuery("SELECT COUNT(u) FROM User u where u.state =:state and u.type =:type " +
                "and (u.name like :searchText or u.firstName like :searchText or u.lastName like :searchText " +
                "or u.nif like :searchText)").setParameter("type", User.Type.USER)
                .setParameter("state", User.State.ACTIVE)
                .setParameter("searchText", "%" + searchText + "%");
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, (long)query.getSingleResult());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaType.JSON).build();
    }

    @Path("/nif/{nif}/{year}/{month}/{day}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response userInfo(@PathParam("nif") String nif,
                           @PathParam("year") int year,
                           @PathParam("month") int month,
                           @PathParam("day") int day,
                           @Context ServletContext context, @Context HttpServletRequest req,
                           @Context HttpServletResponse resp) throws Exception {
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", nif);
        User user = dao.getSingleResult(User.class, query);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity("not found - nif: " + nif).build();
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        Interval timePeriod = DateUtils.getWeekPeriod(calendar);
        BalancesDto dto = balancesBean.getBalancesDto(user, timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Path("/nif/{nif}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response userInfoByNif(@PathParam("nif") String nif,
                           @Context ServletContext context, @Context HttpServletRequest req,
                           @Context HttpServletResponse resp) throws Exception {
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", nif);
        User user = dao.getSingleResult(User.class, query);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity("not found - nif: " + nif).build();
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        BalancesDto dto = balancesBean.getBalancesDto(user, timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Transactional
    @Path("/searchByDevice")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Object searchByDevice(@QueryParam("phone") String phone, @QueryParam("email") String email,
                                 @Context HttpServletRequest req) throws Exception {
        if(phone == null && email == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("missing 'email' or 'phone'").build();
        }
        Query query = dao.getEM().createQuery("select d from Device d where d.email =:email or d.phone =:phone")
                .setParameter("email", email).setParameter("phone", phone);
        Device device = dao.getSingleResult(Device.class, query);
        if(device == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("device not found - email: " + email +
                    " - phone: " + phone).build();
        } else {
            UserDto dto = userBean.getUserDto(device.getUser(), false);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
        }
    }

    @Path("/bankList")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response bankList(@Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        List<Bank> bankList = dao.findAll(Bank.class);
        List<UserDto> resultList = new ArrayList<>();
        for(Bank bank : bankList) {
            resultList.add(userBean.getUserDto(bank, false));
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultList)).build();
    }

    @Path("/userInfoTest")
    @POST @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response userInfoTest(CMSMessage cmsMessage, @Context HttpServletRequest req, @Context
        HttpServletResponse resp) throws Exception {
        CMSSignedMessage cmsSignedMessage = cmsMessage.getCMS();
        Map<String, String> dataMap = cmsSignedMessage.getSignedContent(new TypeReference<Map<String, String>>() {});
        //TODO check operation
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        BalancesDto dto = balancesBean.getBalancesDto(cmsMessage.getUser(), timePeriod);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Path("/save")
    @POST @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response save(CMSMessage cmsMessage, @Context HttpServletRequest req) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        User newUser = userBean.saveUser(cmsMessage);
        MessageDto messageDto = MessageDto.OK(messages.get("certUserNewMsg", newUser.getNif()),
                config.getContextURL() + "/rest/user/id/" + newUser.getId());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(messageDto)).build();
    }

    @Path("/csrSignedWithIDCard")
    @POST @Produces("text/plain")
    public Response csrSignedWithIDCard(CMSMessage cmsMessage) throws Exception {
        X509Certificate issuedCert = cmsBean.signCSRSignedWithIDCard(cmsMessage);
        return Response.ok().entity(PEMUtils.getPEMEncoded(issuedCert)).build();
    }

    @Path("/newBank")
    @POST @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response newBank(CMSMessage cmsMessage, @Context HttpServletRequest req) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Bank newBank = bankBean.saveBank(cmsMessage);
        MessageDto messageDto = new MessageDto(ResponseVS.SC_OK,
                messages.get("newBankOKMsg", newBank.getX509Certificate().getSubjectDN().toString()),
                config.getContextURL() + "/rest/user/id/" + newBank.getId());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(messageDto)).type(MediaType.JSON).build();
    }

    @Path("/connected")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response connected(@Context HttpServletRequest req) throws Exception {
        //TODO
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(
                SessionManager.getInstance().getConnectedUsersDto())).build();
    }

}