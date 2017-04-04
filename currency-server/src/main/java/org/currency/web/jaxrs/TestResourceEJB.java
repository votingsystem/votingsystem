package org.currency.web.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.currency.web.ejb.AuditEJB;
import org.currency.web.ejb.BalancesEJB;
import org.currency.web.ejb.ConfigCurrencyServer;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.votingsystem.cms.MapUtils;
import org.currency.web.http.CurrencyPrincipal;
import org.currency.web.http.HttpSessionManager;
import org.currency.web.http.SignedAccessResource;
import org.currency.web.util.AuditLogger;
import org.currency.web.util.AuthRole;
import org.currency.web.websocket.SessionManager;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.CMSDocument;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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
import java.math.BigDecimal;
import java.security.Principal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

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


    @GET @Path("/sessionId")
    @Produces(MediaType.TEXT_PLAIN)
    public Response sessionId(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        HttpSession session = req.getSession();
        log.info("session.getId: " + session.getId() + " --- user uuid: " + req.getSession().getAttribute(Constants.USER_UUID));
        return Response.ok().entity("sessionId: " + session.getId() + " - user uuid: " +
                req.getSession().getAttribute(Constants.USER_UUID)).build() ;
    }

    @GET @Path("/invalidateSession")
    @Produces(MediaType.TEXT_PLAIN)
    public Response invalidateSession(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        HttpSession session = req.getSession();
        log.info("session id: " + session.getId() + " - userUUID: " +
                req.getSession().getAttribute(Constants.USER_UUID));
        session.invalidate();
        return Response.ok().entity("Invalidated session id: " + session.getId() + " - userUUID: " +
                req.getSession().getAttribute(Constants.USER_UUID)).build() ;
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
                JSON.getMapper().writeValueAsBytes(HttpSessionManager.getInstance().getUserUUIDSessionIdMap())).build() ;
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

    @GET @Path("/testQuery")
    public Response testQuery(@Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws JsonProcessingException, ValidationException {
        Query query = em.createQuery("select SUM(c.balance), tag, c.currencyCode from CurrencyAccount c JOIN c.tag tag where c.state =:state " +
                "group by tag, c.currencyCode").setParameter("state", CurrencyAccount.State.ACTIVE);
         List<Object[]> resultList = query.getResultList();
        for(Object[] result : resultList) {
            log.info("" + result[0] + ((Tag)result[1]).getName() + result[2]);
        }

        return Response.ok().entity("OK").build();
    }

    @POST @Path("/testPost")
    public Response testPost(@Context HttpServletRequest req, String postData, @Context HttpServletResponse resp)
            throws JsonProcessingException, ValidationException {
        return Response.ok().entity("POST-DATA - str: " + postData).build();
    }

    @POST @Path("/xml-signed-document")
    public Response testCMS(SignedDocument signedDocument, @Context HttpServletRequest req,
                            @Context HttpServletResponse res) throws JsonProcessingException, ValidationException {
        log.info("xml-signed-document: " + signedDocument);
        return Response.ok().entity(signedDocument.getBody()).build();
    }

    @POST @Path("/cms-document")
    public Response closeSession(SignedDocument signedDocument, @Context HttpServletRequest req,
                                 @Context HttpServletResponse res) throws Exception {
        log.info("cms-document: " + signedDocument);
        return Response.ok().entity(signedDocument.getBody()).build();
    }

    @POST @Path("/cms-document")
    public Response closeSession(CMSDocument cmsDocument, @Context HttpServletRequest req,
                                 @Context HttpServletResponse res) throws Exception {
        log.info("cmsDocument: " + cmsDocument);
        return Response.ok().entity(cmsDocument.getBody()).build();
    }

    @GET @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response mainMethod(@Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        Map result = new HashMap<>();
        result.put("UUID", UUID.randomUUID().toString());
        result.put("test", "test1, españa acentuación");
        AuditLogger.logReport(result);
        return Response.ok().entity(" - OK - ").build();
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

    @GET @Path("/logTransactions")
    public Response logTransactions(@Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws ServletException, IOException {
        LocalDateTime init = LocalDateTime.now();
        Random randomGenerator = new Random();
        Transaction.Type[] transactionTypes = Transaction.Type.values();
        int numTransactions = 1000;
        for (int idx = 1; idx <= numTransactions; ++idx){
            int randomInt = randomGenerator.nextInt(100);
            int transactionItemId = new Random().nextInt(transactionTypes.length);
            Transaction.Type transactionType = transactionTypes[transactionItemId];
            TransactionDto dto = new TransactionDto();
            dto.setId(Long.valueOf(idx));
            dto.setType(transactionType);
            dto.setFromUserName("fromUser" + randomInt);
            dto.setToUserName("toUser" + randomInt);
            dto.setCurrencyCode(CurrencyCode.EUR);
            dto.setAmount(new BigDecimal(randomInt));
            dto.setSubject("Subject - " + randomInt);
            dto.setDateCreated(ZonedDateTime.now());
            AuditLogger.logTransaction(dto);
        }
        long seconds = ChronoUnit.SECONDS.between(init, LocalDateTime.now());
        String msg = format("NumTransactions : {0} - duration: {1} seconds", numTransactions,
                new DecimalFormat("#,##0").format(seconds));
        log.info(msg);
        return Response.ok().entity(msg).build();
    }

    @GET @Path("/newWeek")
    public Response newWeek() throws Exception {
        auditBean.initWeekPeriod(LocalDateTime.now());
        /*Query query = dao.getEM().createQuery("select t from Transaction t where t.type  in :inList")
                .setParameter("inList", Arrays.asList(Transaction.Type.CURRENCY_PERIOD_INIT,
                Transaction.Type.CURRENCY_PERIOD_INIT_TIME_LIMITED));
        List<Transaction> resultList =  query.getResultList();
        for(Transaction transaction : resultList) {
            dao.getEM().remove(transaction);
        }*/
        return Response.ok().entity("OK").build();
    }

    @Path("/balance")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response balance() throws IOException {
        Map<CurrencyCode, Map<String, IncomesDto>> balancesTo = new HashMap<>();
        balancesTo.put(CurrencyCode.EUR, MapUtils.getTagMapForIncomes("HIDROGENO", new BigDecimal(880.5), new BigDecimal(700.5)));
        balancesTo.put(CurrencyCode.EUR, MapUtils.getTagMapForIncomes("NITROGENO", new BigDecimal(100), new BigDecimal(50.5)));
        balancesTo.put(CurrencyCode.USD, MapUtils.getTagMapForIncomes("WILDTAG", new BigDecimal(1454), new BigDecimal(400.5)));
        balancesTo.put(CurrencyCode.USD, MapUtils.getTagMapForIncomes("NITROGENO", new BigDecimal(100), new BigDecimal(50.5)));

        Map<CurrencyCode, Map<String, BigDecimal>> balancesFrom = new HashMap<>();
        balancesFrom.put(CurrencyCode.EUR, MapUtils.getTagMapForExpenses("HIDROGENO", new BigDecimal(1080.5)));
        balancesFrom.put(CurrencyCode.EUR, MapUtils.getTagMapForExpenses("OXIGENO", new BigDecimal(350)));
        balancesFrom.put(CurrencyCode.USD, MapUtils.getTagMapForExpenses("WILDTAG", new BigDecimal(6000)));
        balancesFrom.put(CurrencyCode.JPY, MapUtils.getTagMapForExpenses("WILDTAG", new BigDecimal(8000)));

        BalancesDto balancesDto = new BalancesDto();
        balancesDto.setBalancesTo(balancesTo);
        balancesDto.setBalancesFrom(balancesFrom);
        balancesDto.calculateCash();

        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(balancesDto)).build();
    }

}