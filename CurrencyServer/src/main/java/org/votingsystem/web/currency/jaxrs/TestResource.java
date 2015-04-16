package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.service.EventBusService;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MapUtils;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.currency.ejb.AuditingBean;
import org.votingsystem.web.currency.ejb.BalancesBean;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.DAOBean;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test")
public class TestResource {

    private static final Logger log = Logger.getLogger(TestResource.class.getSimpleName());

    @Inject AuditingBean auditingBean;
    @Inject BalancesBean balanceBean;
    @Inject DAOBean dao;


    private static ExecutorService executorService;

    static {
        executorService = Executors.newFixedThreadPool(5);
    }


    class EventBusListener {
        @Subscribe public void newUserVS(UserVS userVS) {
            log.info("newUserVS: " + userVS.getNif());
        }
    }


    @GET @Path("/eventBus")
    public Response eventBus(@Context ServletContext context, @Context HttpServletRequest req,
                          @Context HttpServletResponse resp) {
        EventBusService.getInstance().register(new EventBusListener());
        UserVS userVS = new UserVS();
        userVS.setNif("111111D");
        EventBusService.getInstance().post(userVS);
        return Response.ok().entity("").build();
    }

    @GET @Path("/")
    public Response index(@Context ServletContext context, @Context HttpServletRequest req,
                          @Context HttpServletResponse resp) {
        return Response.ok().entity("").build();
    }

    @GET @Path("/checkLocale")
    public Response checkLocale(@Context HttpHeaders requestHeaders, @Context ServletContext context, @Context HttpServletRequest req,
                          @Context HttpServletResponse resp) {
        Locale locale = requestHeaders.getLanguage();
        return Response.ok().entity(locale.getCountry()).build();
    }


    @GET @Path("/asyncResource")
    public void asyncResource(@Suspended AsyncResponse response) {
        CompletableFuture.supplyAsync(this::getMessage, executorService).thenAccept(response::resume);
    }

    public String getMessage() {
        try {
            Thread.sleep(5000);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return "Hello from async resource";
    }

    @GET @Path("/IBAN")
    public Response IBAN(@Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws ServletException, IOException {
        String accountNumberStr = String.format("%010d", 12345L);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode("7777").branchCode( "7777")
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return Response.ok().entity(iban.toString()).build();
    }

    @GET @Path("/broadcast")
    public Response broadcast(@Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws ServletException, IOException {
        Map dataMap = new HashMap<>();
        dataMap.put("status", 200);
        dataMap.put("message", "Hello");
        dataMap.put("coreSignal", "transactionvs-new");
        SessionVSManager.getInstance().broadcast(new ObjectMapper().writeValueAsString(dataMap));
        return Response.ok().entity("OK").build();
    }

    @GET @Path("/checkCooin")
    public Response checkCooin(@Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws ServletException, IOException {
        TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        auditingBean.checkCurrencyRequest(timePeriod);
        return Response.ok().entity("OK").build();
    }

    @GET @Path("/logTransactions")
    public Response logTransactions(@Context ServletContext context, @Context HttpServletRequest req,
                               @Context HttpServletResponse resp) throws ServletException, IOException {
        Long init = System.currentTimeMillis();
        Random randomGenerator = new Random();
        TransactionVS.Type[] transactionTypes = TransactionVS.Type.values();
        int numTransactions = 1000;
        for (int idx = 1; idx <= numTransactions; ++idx){
            int randomInt = randomGenerator.nextInt(100);
            int transactionvsItemId = new Random().nextInt(transactionTypes.length);
            TransactionVS.Type transactionType = transactionTypes[transactionvsItemId];
            LoggerVS.logTransactionVS(Long.valueOf(idx), "TRANSACTION_STATE", transactionType.toString(),
                    "fromUser" + randomInt, "toUser" + randomInt, Currency.getInstance("EUR").getCurrencyCode(),
                    new BigDecimal(randomInt), null, Calendar.getInstance().getTime(), null, "Subject - " + randomInt, true);
        }
        Long finish = System.currentTimeMillis();
        Long duration = finish - init;
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        String msg = format(" --- Done numTransactions : {0} - duration in millis: {1} - duration: {2}",
                numTransactions, duration, durationStr);
        log.info(msg);
        return Response.ok().entity(msg).build();
    }

    @GET @Path("/newWeek")
    public Response newWeek() throws IOException {
        Calendar nextWeek = Calendar.getInstance();
        nextWeek.set(Calendar.WEEK_OF_YEAR, (nextWeek.get(Calendar.WEEK_OF_YEAR) + 1));
        balanceBean.initWeekPeriod(nextWeek);
        /*List transactionList
        TransactionVS.withTransaction {
            //transactionList = TransactionVS.findAllWhere(type:[TransactionVS.Type.CURRENCY_INIT_PERIOD,
            //       TransactionVS.Type.CURRENCY_INIT_PERIOD_TIME_LIMITED])
            transactionList = TransactionVS.createCriteria().list(offset: 0) {
                inList("type", [TransactionVS.Type.CURRENCY_INIT_PERIOD,
                                TransactionVS.Type.CURRENCY_INIT_PERIOD_TIME_LIMITED] )
            }

            for(TransactionVS transaction : transactionList) {
                transaction.delete()
            }
        }*/
        return Response.ok().entity("OK").build();
    }

    @Path("/balance")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response balance() throws IOException {
        Map balanceTo = new HashMap<>();
        balanceTo.put("EUR", MapUtils.getTagMapForIncomes(new MapUtils.TagData("HIDROGENO", new BigDecimal(880.5), new BigDecimal(700.5)),
                new MapUtils.TagData("NITROGENO", new BigDecimal(100), new BigDecimal(50.5))));
        balanceTo.put("DOLLAR", MapUtils.getTagMapForIncomes(new MapUtils.TagData("WILDTAG", new BigDecimal(1454), new BigDecimal(400.5)),
                new MapUtils.TagData("NITROGENO", new BigDecimal(100), new BigDecimal(50.5))));
        Map balanceFrom = new HashMap<>();
        balanceFrom.put("EUR", MapUtils.getTagMapForExpenses(new MapUtils.TagData("HIDROGENO", new BigDecimal(1080.5)),
                new MapUtils.TagData("OXIGENO", new BigDecimal(350))));
        balanceFrom.put("DOLLAR", MapUtils.getTagMapForExpenses(new MapUtils.TagData("WILDTAG", new BigDecimal(6000))));
        balanceFrom.put("YEN", MapUtils.getTagMapForExpenses(new MapUtils.TagData("WILDTAG", new BigDecimal(8000))));

        BalancesDto balancesDto = new BalancesDto();
        balancesDto.setBalancesTo(balanceTo);
        balancesDto.setBalancesFrom(balanceFrom);
        balancesDto.calculateCash();

        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(balancesDto)).build();
    }

    @Path("/initWeek")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object initWeek() throws Exception {
        UserVS userVS = dao.find(UserVS.class, 2L);
        TimePeriod timePeriod = DateUtils.getWeekPeriod(DateUtils.getDayFromPreviousWeek(Calendar.getInstance()));
        balanceBean.initUserVSWeekPeriod(userVS, timePeriod, "TestingController");
        return Response.ok().entity("OK").build();
    }

    @Path("/initWeekPeriod")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object initWeekPeriod() throws Exception {
        balanceBean.initWeekPeriod(Calendar.getInstance());
        return Response.ok().entity("OK").build();
    }

}
