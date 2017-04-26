package org.currency.web.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.currency.web.ejb.AuditEJB;
import org.currency.web.ejb.BalancesEJB;
import org.currency.web.ejb.ConfigCurrencyServer;
import org.currency.web.http.CurrencyPrincipal;
import org.currency.web.http.HttpSessionManager;
import org.currency.web.http.SignedAccessResource;
import org.currency.web.util.AuditLogger;
import org.currency.web.util.AuthRole;
import org.currency.web.websocket.SessionManager;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.votingsystem.model.CMSDocument;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Interval;
import org.votingsystem.util.JSON;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test")
public class TestResourceEJB {

    private static final Logger log = Logger.getLogger(TestResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private AuditEJB auditBean;
    @Inject private BalancesEJB balanceBean;
    @Inject private ConfigCurrencyServer config;

    @GET @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response rootPath(@Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        Map result = new HashMap<>();
        result.put("UUID", UUID.randomUUID().toString());
        result.put("test", "test1, españa acentuación");
        AuditLogger.logReport(result);
        return Response.ok().entity(" - OK - ").build();
    }

    @GET @Path("/sessionId")
    @Produces(MediaType.TEXT_PLAIN)
    public Response sessionId(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        HttpSession session = req.getSession();
        log.info("session.getId: " + session.getId() + " --- user uuid: " + req.getSession().getAttribute(Constants.SESSION_UUID));
        return Response.ok().entity("sessionId: " + session.getId() + " - user uuid: " +
                req.getSession().getAttribute(Constants.SESSION_UUID)).build() ;
    }

    @GET @Path("/invalidateSession")
    @Produces(MediaType.TEXT_PLAIN)
    public Response invalidateSession(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        HttpSession session = req.getSession();
        String msg = "Invalidated session id: " + session.getId() + " - userUUID: " +
                req.getSession().getAttribute(Constants.SESSION_UUID);
        session.invalidate();
        return Response.ok().entity(msg).build() ;
    }

    @GET @Path("/sessions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSessions(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        return Response.ok().entity(
                JSON.getMapper().writeValueAsBytes(HttpSessionManager.getInstance().getSessionUUIDSet())).build() ;
    }

    @GET @Path("/sessionsMap")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSessionsMap(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        return Response.ok().entity(
                JSON.getMapper().writeValueAsBytes(HttpSessionManager.getInstance().getSessionUUIDSessionIdMap())).build() ;
    }

    @GET @Path("/addToRealm")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addToRealm(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        HttpSession session = req.getSession(true);
        log.info("session: " + session.getId());
        session.setAttribute(Constants.USER_KEY, new User(User.Type.USER, "John", "Doe", "08888888D", null));
        //req.authenticate(res);
        log.info("HAS ADMIN " + req.isUserInRole("ADMIN"));
        log.info("HAS USER " + req.isUserInRole("USER"));
        return Response.ok().entity("Authentication OK!!!").build() ;
    }

    @RolesAllowed(AuthRole.USER)
    @GET @Path("/privateUser")
    @Produces(MediaType.TEXT_PLAIN)
    public Response testUser(@Context HttpServletRequest req, @Context SecurityContext securityContext) throws Exception {
        Principal principal = securityContext.getUserPrincipal();
        log.info("testUser - principal: " + principal.getName());
        log.info("HAS USER " + securityContext.isUserInRole("USER"));
        log.info("HAS ADMIN " + securityContext.isUserInRole("ADMIN"));
        return Response.ok().entity("Authentication OK!!!").build() ;
    }

    @RolesAllowed(AuthRole.ADMIN)
    @GET @Path("/privateAdmin")
    @Produces(MediaType.TEXT_PLAIN)
    public Response testAdmin(@Context HttpServletRequest req, @Context SecurityContext securityContext) throws Exception {
        Principal principal = securityContext.getUserPrincipal();
        log.info("testAdmin - principal: " + principal.getName());
        log.info("HAS USER " + securityContext.isUserInRole("USER"));
        log.info("HAS ADMIN " + securityContext.isUserInRole("ADMIN"));
        return Response.ok().entity("Authentication OK!!!").build() ;
    }

    @SignedAccessResource
    @POST @Path("/privatePost")
    @Produces(MediaType.TEXT_PLAIN)
    public Response postTest(@Context SecurityContext securityContext, @Context HttpServletRequest req) throws Exception {
        CurrencyPrincipal principal = (CurrencyPrincipal) securityContext.getUserPrincipal();
        //User user = ((CurrencyPrincipal)req.getUserPrincipal()).getUser();
        log.info("SignedDocument: " + principal.getSignedDocument());
        return Response.ok().entity("Authentication OK!!!").build() ;
    }

    @GET @Path("/test")
    public Response test(@Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        log.info("--- entity id: " + config.getEntityId());
        Interval timePeriod = DateUtils.getCurrentWeekPeriod();
        //SELECT b FROM Bank b WHERE b.numId =:numId
        List<User> userList = em.createQuery("select u from User u where u.id=:id").setParameter("id", 2L).getResultList();
        User user = userList.iterator().next();
        return Response.ok().entity("OK").build();
    }

    @POST @Path("/testPost")
    public Response testPost(@Context HttpServletRequest req, String postData, @Context HttpServletResponse resp)
            throws JsonProcessingException, ValidationException {
        return Response.ok().entity("POST-DATA - str: " + postData).build();
    }

    @POST @Path("/xml-signed-document")
    public Response xmlSigned(SignedDocument signedDocument, @Context HttpServletRequest req,
                            @Context HttpServletResponse res) throws JsonProcessingException, ValidationException {
        log.info("xml-signed-document: " + signedDocument);
        return Response.ok().entity(signedDocument.getBody()).build();
    }

    @POST @Path("/signed-doc")
    public Response signedDoc(SignedDocument signedDocument, @Context HttpServletRequest req,
                                 @Context HttpServletResponse res) throws Exception {
        log.info("currency-document: " + signedDocument);
        return Response.ok().entity(signedDocument.getBody()).build();
    }

    @POST @Path("/currency-doc")
    public Response cmsDoc(CMSDocument cmsDocument, @Context HttpServletRequest req,
                                 @Context HttpServletResponse res) throws Exception {
        log.info("cmsDocument: " + cmsDocument);
        return Response.ok().entity(cmsDocument.getBody()).build();
    }

    @GET @Path("/checkLocale")
    public Response checkLocale(@Context HttpHeaders requestHeaders, @Context HttpServletRequest req,
                          @Context HttpServletResponse resp) {
        Locale locale = requestHeaders.getLanguage();
        return Response.ok().entity(locale.getCountry()).build();
    }

    @GET @Path("/IBAN")
    public Response IBAN(@Context HttpServletRequest req, @Context HttpServletResponse res) throws ServletException,
            IOException {
        String accountNumberStr = String.format("%010d", 12345L);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode("7777").branchCode( "7777")
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return Response.ok().entity(iban.toString()).build();
    }

    @GET @Path("/broadcast")
    public Response broadcast(@Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws ServletException, IOException {
        Map dataMap = new HashMap<>();
        dataMap.put("status", 200);
        dataMap.put("message", "Hello");
        dataMap.put("coreSignal", "transaction-new");
        SessionManager.getInstance().broadcast(JSON.getMapper().writeValueAsString(dataMap));
        return Response.ok().entity("OK").build();
    }

    @GET @Path("/newWeek")
    public Response newWeek() throws Exception {
        auditBean.initWeekPeriod(LocalDateTime.now());
        return Response.ok().entity("OK").build();
    }

}