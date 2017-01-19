package org.votingsystem.currency.web.jaxrs;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.iban4j.Iban;
import org.votingsystem.currency.web.ejb.BankEJB;
import org.votingsystem.currency.web.ejb.ConfigCurrencyServer;
import org.votingsystem.currency.web.ejb.TransactionEJB;
import org.votingsystem.currency.web.ejb.UserEJB;
import org.votingsystem.currency.web.util.AuthRole;
import org.votingsystem.currency.web.websocket.SessionManager;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.UserDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.Device;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.BankInfo;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Interval;
import org.votingsystem.util.JSON;
import org.votingsystem.util.Messages;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Stateless
@Path("/user")
public class UserResourceEJB {

    private static final Logger log = Logger.getLogger(UserResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private TransactionEJB transactionBean;
    @Inject private UserEJB userBean;
    @Inject private BankEJB bankBean;
    @Inject private SignatureService signatureService;
    @Inject private ConfigCurrencyServer config;

    @Path("/") @GET
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response indexJSON(@DefaultValue("0") @QueryParam("offset") int offset,
            @DefaultValue("100") @QueryParam("max") int max, @QueryParam("searchText") String searchText,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        User.Type userType = User.Type.USER;
        User.State userState = User.State.ACTIVE;
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        LocalDateTime dateFrom = null;
        LocalDateTime dateTo = null;
        try {userType = User.Type.valueOf(req.getParameter("type"));} catch(Exception ex) {}
        try {userState = User.State.valueOf(req.getParameter("state"));} catch(Exception ex) {}
        if(req.getParameter("searchFrom") != null) try {dateFrom = DateUtils.getDate(
                req.getParameter("searchFrom"));} catch(Exception ex) { dateFrom = timePeriod.getDateFrom().toLocalDateTime();}
        if(req.getParameter("searchTo") != null) try {dateTo = DateUtils.getDate(
                req.getParameter("searchTo"));} catch(Exception ex) {dateTo = timePeriod.getDateTo().toLocalDateTime();}
        Criteria criteria = em.unwrap(Session.class).createCriteria(User.class);
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
    public Response findByIBAN(@PathParam("IBAN") String IBAN, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        List<User> userList = em.createNamedQuery(User.FIND_USER_BY_IBAN).setParameter("IBAN", IBAN).getResultList();
        String msg = null;
        User user;
        if(userList.isEmpty()) {//check if external bank client
            Iban iban = Iban.valueOf(IBAN);
            List<BankInfo> bankInfos = em.createQuery("select bi from BankInfo bi where bi.bankCode =:bankCode")
                    .setParameter("bankCode", iban.getBankCode()).getResultList();
            if(!bankInfos.isEmpty()) {
                user = bankInfos.iterator().next().getBank();
                msg = Messages.currentInstance().get("ibanFromBankClientMsg", IBAN, user.getName());
            }
        }
        user = userList.iterator().next();
        if(user == null) return Response.status(Response.Status.NOT_FOUND)
                .entity(Messages.currentInstance().get("itemNotFoundByIBANMsg", IBAN)).build();
        return processUserResult(user, msg);
    }

    @Path("/id/{id}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response index(@PathParam("id") long id,
                @DefaultValue("false") @QueryParam("connectedDevices") Boolean connectedDevices,
                @Context ServletContext context, @Context HttpServletRequest req,
                @Context HttpServletResponse resp) throws Exception {
        User user = em.find(User.class, id);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                Messages.currentInstance().get("objectNotFoundMsg", Long.valueOf(id).toString())).build();
        if(connectedDevices) {
            UserDto resultDto = userBean.getUserDto(user, false);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultDto)).build() ;
        } else return processUserResult(user, null);
    }

    private Response processUserResult(User user, String msg) throws Exception {
        UserDto resultDto = UserDto.COMPLETE(user);
        if(user instanceof Bank)
            resultDto.setDetails(msg);
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
        Query query = em.createQuery("select u from User u where u.state =:state and u.type =:type " +
                "and (lower(u.name) like :searchText or lower(u.name) like :searchText or lower(u.surname) like :searchText " +
                "or lower(u.numId) like :searchText)").setParameter("type", User.Type.USER)
                .setParameter("state", User.State.ACTIVE)
                .setParameter("searchText", "%" + searchText.toLowerCase() + "%")
                .setFirstResult(offset).setMaxResults(max);
        List<User> userList = query.getResultList();
        List<UserDto> resultList = new ArrayList<>();
        for(User user : userList) {
            resultList.add(userBean.getUserDto(user, false));
        }
        query = em.createQuery("SELECT COUNT(u) FROM User u where u.state =:state and u.type =:type " +
                "and (u.name like :searchText or u.name like :searchText or u.surname like :searchText " +
                "or u.numId like :searchText)").setParameter("type", User.Type.USER)
                .setParameter("state", User.State.ACTIVE)
                .setParameter("searchText", "%" + searchText + "%");
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, (long)query.getSingleResult());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).type(MediaType.JSON).build();
    }

    @Transactional
    @Path("/searchByDevice")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Object searchByDevice(@QueryParam("phone") String phone, @QueryParam("email") String email,
                                 @Context HttpServletRequest req) throws Exception {
        if(phone == null && email == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("missing 'email' or 'phone'").build();
        }
        List<Device> deviceList = em.createQuery("select d from Device d where d.email =:email or d.phone =:phone")
                .setParameter("email", email).setParameter("phone", phone).getResultList();
        if(deviceList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("device not found - email: " + email +
                    " - phone: " + phone).build();
        } else {
            Device device = deviceList.iterator().next();
            UserDto dto = userBean.getUserDto(device.getUser(), false);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
        }
    }

    @Path("/bankList")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response bankList(@Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        List<Bank> bankList = em.createQuery("select b from Bank b").getResultList();
        List<UserDto> resultList = new ArrayList<>();
        for(Bank bank : bankList) {
            resultList.add(userBean.getUserDto(bank, false));
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultList)).build();
    }


    @Path("/new-bank")
    @POST @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response newBank(SignedDocument signedDocument, @Context HttpServletRequest req) throws Exception {
        Bank newBank = bankBean.saveBank(signedDocument);
        ResponseDto responseDto = new ResponseDto(ResponseDto.SC_OK,
                Messages.currentInstance().get("newBankOKMsg", newBank.getX509Certificate().getSubjectDN().toString()),
                config.getEntityId() + "/api/user/id/" + newBank.getId());
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(responseDto)).type(MediaType.JSON).build();
    }

    @RolesAllowed(AuthRole.ADMIN)
    @Path("/connected")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response connected(@Context HttpServletRequest req) throws Exception {
        //TODO
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(
                SessionManager.getInstance().getConnectedUsersDto())).build();
    }

}