package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.model.User;
import org.votingsystem.util.*;
import org.votingsystem.web.currency.ejb.BalancesBean;
import org.votingsystem.web.currency.ejb.CurrencyAccountBean;
import org.votingsystem.web.currency.filter.PrincipalVS;
import org.votingsystem.web.currency.util.ReportFiles;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/balance")
public class BalanceResource {

    private static Logger log = Logger.getLogger(BalanceResource.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CurrencyAccountBean accountBean;
    @Inject BalancesBean balancesBean;

    @RolesAllowed("ADMIN")
    @GET @Path("/user/id/{userId}")
    public Response user(@PathParam("userId") long userId, @Context ServletContext context,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        User user = dao.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                messages.get("objectNotFoundMsg", userId)).build();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getWeekPeriod(Calendar.getInstance()));
    }

    @RolesAllowed("ADMIN")
    @GET @Path("/user/IBAN/{IBAN}")
    public Response userByIBAN(@PathParam("IBAN") String IBAN, @Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", IBAN);
        User user = dao.getSingleResult(User.class, query);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                messages.get("itemNotFoundByIBANMsg", IBAN)).build();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getWeekPeriod(Calendar.getInstance()));
    }

    @RolesAllowed("USER")
    @GET @Path("/user")
    public Response user(ServletContext context, @Context HttpServletRequest req,
                     @Context HttpServletResponse resp) throws Exception {
        User user = ((PrincipalVS)req.getUserPrincipal()).getUser();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getWeekPeriod(Calendar.getInstance()));
    }

    @RolesAllowed("ADMIN")
    @GET @Path("/user/nif/{userNIF}")
    public Response userByNIF(@PathParam("userNIF") String nif, @Context ServletContext context,
                           @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        nif = NifUtils.validate(nif);
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", nif);;
        User user = dao.getSingleResult(User.class, query);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - userId: " + nif).build();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getWeekPeriod(Calendar.getInstance()));
    }

    @RolesAllowed("ADMIN")
    @Path("/user/id/{userId}/{timePeriod}")
    @GET @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response user(@PathParam("userId") long userId, @PathParam("timePeriod") String lapseStr,
                         @Context ServletContext context, @Context HttpServletRequest req,
                         @Context HttpServletResponse resp) throws Exception {
        User user = dao.find(User.class, userId);
        if(user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("not found - userId: " + userId).build();
        }
        Interval.Lapse lapse =  Interval.Lapse.valueOf(lapseStr.toUpperCase());
        Interval timePeriod = DateUtils.getLapsePeriod(Calendar.getInstance(req.getLocale()).getTime(), lapse);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(balancesBean.getBalancesDto(user, timePeriod))).build();
    }

    @RolesAllowed("ADMIN")
    @Path("/user/id/{userId}/{year}/{month}/{day}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response userWeek(@PathParam("userId") long userId, @PathParam("year") int year, @PathParam("month") int month,
                                 @PathParam("day") int day, @Context ServletContext context, @Context HttpServletRequest req,
                                 @Context HttpServletResponse resp) throws Exception {
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        User user = dao.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - userId: " + userId).build();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getWeekPeriod(calendar));
    }

    @RolesAllowed("ADMIN")
    @Path("/user/id/{userId}/{year}/{month}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response userMonth(@PathParam("userId") long userId, @PathParam("year") int year, @PathParam("month") int month,
                             @Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        Calendar calendar = DateUtils.getCalendar(year, month, 1);
        User user = dao.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - userId: " + userId).build();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getMonthPeriod(calendar));
    }

    @RolesAllowed("ADMIN")
    @Path("/user/id/{userId}/{year}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response userYear(@PathParam("userId") long userId, @PathParam("year") int year,
                              @Context ServletContext context, @Context HttpServletRequest req,
                              @Context HttpServletResponse resp) throws Exception {
        Calendar calendar = DateUtils.getCalendar(year, 1, 1);
        User user = dao.find(User.class, userId);
        if(user == null) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - User not found - userId: " + userId).build();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getYearPeriod(calendar));
    }


    @RolesAllowed("USER")
    @Path("/user/{year}/{month}/{day}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response authUserWeek(@PathParam("year") int year, @PathParam("month") int month,
                             @PathParam("day") int day, @Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws Exception {
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        User user = ((PrincipalVS)req.getUserPrincipal()).getUser();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getWeekPeriod(calendar));
    }

    @RolesAllowed("USER")
    @Path("/user/{year}/{month}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response authUserMonth(@PathParam("year") int year, @PathParam("month") int month,
                              @Context ServletContext context, @Context HttpServletRequest req,
                              @Context HttpServletResponse resp) throws Exception {
        Calendar calendar = DateUtils.getCalendar(year, month, 1);
        User user = ((PrincipalVS)req.getUserPrincipal()).getUser();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getMonthPeriod(calendar));
    }

    @RolesAllowed("USER")
    @Path("/user/{year}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response authUserYear(@PathParam("year") int year, @Context ServletContext context,
                 @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        Calendar calendar = DateUtils.getCalendar(year, 1, 1);
        User user = ((PrincipalVS)req.getUserPrincipal()).getUser();
        return getUserBalancesDto(req, resp, context, user, DateUtils.getYearPeriod(calendar));
    }

    private Response getUserBalancesDto(HttpServletRequest req, HttpServletResponse resp, ServletContext context,
                                        User user, Interval timePeriod) throws Exception {
        BalancesDto balancesDto = balancesBean.getBalancesDto(user, timePeriod);
        return Response.ok().type(MediaType.JSON).entity(
                JSON.getMapper().writeValueAsBytes(balancesDto)).build();
    }

    @RolesAllowed("ADMIN")
    @Path("/user/id/{userId}/db")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Map userDB(@PathParam("userId") long userId, @Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) {
        User user = dao.find(User.class, userId);
        if(user == null) {
            throw new NotFoundException("userId: " + userId);
        } else return accountBean.getAccountsBalanceMap(user);
    }

    @RolesAllowed("ADMIN")
    //data for <balance-weekreport>
    @Path("/week/{year}/{month}/{day}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Object week(@PathParam("year") int year, @PathParam("month") int month, @PathParam("day") int day,
            @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        Interval timePeriod = DateUtils.getWeekPeriod(calendar);
        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getServerDir().getAbsolutePath(), null);
        if(reportFiles.getJsonFile() == null) {
            throw new NotFoundException("reportsForWeekNotFoundMsg - timePeriod: " + timePeriod.toString());
        } else {
            return Response.ok().type(MediaType.JSON).entity(FileUtils.getBytesFromFile(reportFiles.getJsonFile())).build();
        }
    }

    @RolesAllowed("ADMIN")
    @Path("/weekdb/{year}/{month}/{day}")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Object weekdb(@PathParam("year") int year, @PathParam("month") int month, @PathParam("day") int day,
                       @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        Interval timePeriod = DateUtils.getWeekPeriod(calendar);
        //TODO balancesBean.calculatePeriod(DateUtils.getWeekPeriod(calendar))
        return new HashMap<>();
    }

}
