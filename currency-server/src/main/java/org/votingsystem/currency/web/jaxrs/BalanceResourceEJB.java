package org.votingsystem.currency.web.jaxrs;

import org.votingsystem.currency.web.ejb.BalancesEJB;
import org.votingsystem.currency.web.ejb.ConfigCurrencyServer;
import org.votingsystem.currency.web.ejb.CurrencyAccountEJB;
import org.votingsystem.currency.web.http.CurrencyPrincipal;
import org.votingsystem.currency.web.util.AuthRole;
import org.votingsystem.currency.web.util.ReportFiles;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.http.MediaType;
import org.votingsystem.model.User;
import org.votingsystem.util.*;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/balance")
public class BalanceResourceEJB {

    private static Logger log = Logger.getLogger(BalanceResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private CurrencyAccountEJB accountBean;
    @Inject private BalancesEJB balancesBean;

    @RolesAllowed(AuthRole.ADMIN)
    @GET @Path("/user/id/{userId}")
    public Response user(@PathParam("userId") long userId) throws Exception {
        User user = em.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                Messages.currentInstance().get("objectNotFoundMsg", userId)).build();
        return getUserBalancesDto(user, DateUtils.getWeekPeriod(LocalDateTime.now()));
    }

    @RolesAllowed(AuthRole.ADMIN)
    @GET @Path("/user/IBAN/{IBAN}")
    public Response userByIBAN(@PathParam("IBAN") String IBAN) throws Exception {
        List<User> userList = em.createNamedQuery(User.FIND_USER_BY_IBAN).setParameter("IBAN", IBAN).getResultList();
        if(userList.isEmpty()) return Response.status(Response.Status.NOT_FOUND).entity(
                Messages.currentInstance().get("itemNotFoundByIBANMsg", IBAN)).build();
        return getUserBalancesDto(userList.iterator().next(), DateUtils.getWeekPeriod(LocalDateTime.now()));
    }

    @RolesAllowed(AuthRole.USER)
    @GET @Path("/user")
    public Response user(@Context HttpServletRequest req) throws Exception {
        User user = ((CurrencyPrincipal)req.getUserPrincipal()).getUser();
        return getUserBalancesDto(user, DateUtils.getWeekPeriod(LocalDateTime.now()));
    }

    @RolesAllowed(AuthRole.ADMIN)
    @GET @Path("/user/nif/{userNIF}")
    public Response userByNIF(@PathParam("userNIF") String nif) throws Exception {
        nif = NifUtils.validate(nif);
        List<User> userList = em.createNamedQuery(User.FIND_USER_BY_NIF).setParameter("nif", nif).getResultList();
        if(userList.isEmpty()) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - userId: " + nif).build();
        return getUserBalancesDto(userList.iterator().next(), DateUtils.getWeekPeriod(LocalDateTime.now()));
    }

    @RolesAllowed(AuthRole.ADMIN)
    @Path("/user/id/{userId}/{timePeriod}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response user(@PathParam("userId") long userId, @PathParam("timePeriod") String lapseStr,
                         @Context HttpServletRequest req) throws Exception {
        User user = em.find(User.class, userId);
        if(user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("not found - userId: " + userId).build();
        }
        Interval.Lapse lapse =  Interval.Lapse.valueOf(lapseStr.toUpperCase());
        Interval timePeriod = DateUtils.getLapsePeriod(LocalDateTime.now(), lapse);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(
                balancesBean.getBalancesDto(user, timePeriod))).build();
    }

    @RolesAllowed(AuthRole.ADMIN)
    @Path("/user/id/{userId}/{year}/{month}/{day}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response userWeek(@PathParam("userId") long userId, @PathParam("year") int year, @PathParam("month") int month,
                                 @PathParam("day") int day, @Context HttpServletRequest req) throws Exception {
        User user = em.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - userId: " + userId).build();
        return getUserBalancesDto(user, DateUtils.getWeekPeriod(LocalDateTime.of(year, month, day, 0 ,0)));
    }

    @RolesAllowed(AuthRole.ADMIN)
    @Path("/user/id/{userId}/{year}/{month}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response userMonth(@PathParam("userId") long userId, @PathParam("year") int year,
                              @PathParam("month") int month) throws Exception {
        User user = em.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - userId: " + userId).build();
        return getUserBalancesDto(user, DateUtils.getMonthPeriod(LocalDateTime.of(year, month, 1, 0 ,0)));
    }

    @RolesAllowed(AuthRole.ADMIN)
    @Path("/user/id/{userId}/{year}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response userYear(@PathParam("userId") long userId, @PathParam("year") int year) throws Exception {
        User user = em.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - userId: " + userId).build();
        return getUserBalancesDto(user, DateUtils.getYearPeriod(LocalDateTime.of(year, 1, 1, 0 ,0)));
    }


    @RolesAllowed(AuthRole.USER)
    @Path("/user/{year}/{month}/{day}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response authUserWeek(@PathParam("year") int year, @PathParam("month") int month,
                             @PathParam("day") int day, @Context HttpServletRequest req) throws Exception {
        User user = ((CurrencyPrincipal)req.getUserPrincipal()).getUser();
        return getUserBalancesDto(user, DateUtils.getWeekPeriod(LocalDateTime.of(year, month, day, 0 ,0)));
    }

    @RolesAllowed(AuthRole.USER)
    @Path("/user/{year}/{month}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response authUserMonth(@PathParam("year") int year, @PathParam("month") int month,
                              @Context HttpServletRequest req) throws Exception {
        User user = ((CurrencyPrincipal)req.getUserPrincipal()).getUser();
        return getUserBalancesDto(user, DateUtils.getMonthPeriod(LocalDateTime.of(year, month, 1, 0 ,0)));
    }

    @RolesAllowed(AuthRole.USER)
    @Path("/user/{year}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response authUserYear(@PathParam("year") int year, @Context HttpServletRequest req) throws Exception {
        User user = ((CurrencyPrincipal)req.getUserPrincipal()).getUser();
        return getUserBalancesDto(user, DateUtils.getYearPeriod(LocalDateTime.of(year, 1, 1, 0 ,0)));
    }

    private Response getUserBalancesDto(User user, Interval timePeriod) throws Exception {
        BalancesDto balancesDto = balancesBean.getBalancesDto(user, timePeriod);
        return Response.ok().type(MediaType.JSON).entity(
                JSON.getMapper().writeValueAsBytes(balancesDto)).build();
    }

    @RolesAllowed(AuthRole.ADMIN)
    @Path("/user/id/{userId}/db")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Map userDB(@PathParam("userId") long userId) {
        User user = em.find(User.class, userId);
        if(user == null) {
            throw new NotFoundException("userId: " + userId);
        } else return accountBean.getAccountsBalanceMap(user);
    }

    @RolesAllowed(AuthRole.ADMIN)
    //data for <balance-weekreport>
    @Path("/week/{year}/{month}/{day}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response week(@PathParam("year") int year, @PathParam("month") int month, @PathParam("day") int day)
            throws IOException, ServletException {
        Interval timePeriod = DateUtils.getWeekPeriod(LocalDateTime.of(year, month, day, 0 ,0));
        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getApplicationDataPath(), null);
        if(reportFiles.getReportFile() == null) {
            throw new NotFoundException("reportsForWeekNotFoundMsg - timePeriod: " + timePeriod.toString());
        } else {
            return Response.ok().type(MediaType.JSON).entity(FileUtils.getBytesFromFile(reportFiles.getReportFile())).build();
        }
    }

    @RolesAllowed(AuthRole.ADMIN)
    @Path("/weekdb/{year}/{month}/{day}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Object weekdb(@PathParam("year") int year, @PathParam("month") int month, @PathParam("day") int day)
            throws IOException, ServletException {
        Interval timePeriod = DateUtils.getWeekPeriod(LocalDateTime.of(year, month, day, 0 ,0));
        //TODO balancesBean.calculatePeriod(DateUtils.getWeekPeriod(LocalDateTime.of(year, month, day, 0 ,0)))
        return new HashMap<>();
    }

}
