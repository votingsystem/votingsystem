package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.currency.ejb.BalancesBean;
import org.votingsystem.web.currency.ejb.CurrencyAccountBean;
import org.votingsystem.web.currency.util.ReportFiles;
import org.votingsystem.web.ejb.DAOBean;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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

    private static Logger log = Logger.getLogger(BalanceResource.class.getSimpleName());

    @Inject
    DAOBean dao;
    @Inject ConfigVS config;
    @PersistenceContext private EntityManager em;
    @Inject
    CurrencyAccountBean accountBean;
    @Inject BalancesBean balancesBean;


    @GET
    @Path("/userVS/{userId}")
    public Object userVS(@PathParam("userId") long userId, @Context ServletContext context,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        return getUserVSBalanceMap(req, resp, context, userId, Calendar.getInstance());
    }

    @Path("/userVS/{userId}/{year}/{month}/{day}")
    @GET  @Produces(MediaType.APPLICATION_JSON)
    public Object userVSWithDate(@PathParam("userId") long userId, @PathParam("year") int year, @PathParam("month") int month,
            @PathParam("day") int day, @Context ServletContext context, @Context HttpServletRequest req,
            @Context HttpServletResponse resp) throws Exception {
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        return getUserVSBalanceMap(req, resp, context, userId, calendar);
    }

    private Response getUserVSBalanceMap(HttpServletRequest req, HttpServletResponse resp, ServletContext context,
               long userId, Calendar calendar) throws Exception {
        UserVS uservs = dao.find(UserVS.class, userId);
        if(uservs == null) {
            throw new NotFoundException("userId: " + userId);
        } else {
            TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar);
            BalancesDto balancesDto = balancesBean.genBalance(uservs, timePeriod);
            if(req.getContentType() != null && req.getContentType().contains("json")) {
                return Response.ok().entity(JSON.getMapper().writeValueAsBytes(balancesDto)).build();
            } else {
                req.setAttribute("balanceMap", JSON.getMapper().writeValueAsString(balancesDto));
                context.getRequestDispatcher("/balance/userVS.xhtml").forward(req, resp);
                return Response.ok().build();
            }
        }
    }

    @Path("/userVS/{userId}/db")
    @GET  @Produces(MediaType.APPLICATION_JSON)
    public Map userVSDB(@PathParam("userId") long userId, @Context ServletContext context, @Context HttpServletRequest req,
                @Context HttpServletResponse resp) {
        UserVS userVS = dao.find(UserVS.class, userId);
        if(userVS == null) {
            throw new NotFoundException("userId: " + userId);
        } else return accountBean.getAccountsBalanceMap(userVS);
    }

    @Path("/week/{year}/{month}/{day}") //old_url -> /balance/week/$year/$month/$day
    @GET  @Produces(MediaType.APPLICATION_JSON)
    public Object week(@PathParam("year") int year, @PathParam("month") int month, @PathParam("day") int day,
            @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar);
        ReportFiles reportFiles = new ReportFiles(timePeriod, config.getServerDir().getAbsolutePath(), null);
        if(reportFiles.getJsonFile() == null) {
            throw new NotFoundException("reportsForWeekNotFoundMsg - timePeriod: " + timePeriod.toString());
        } else {
            Map<String, Object> dataMap = new ObjectMapper().readValue(reportFiles.getJsonFile(),
                    new TypeReference<HashMap<String, Object>>() {});
            if(req.getContentType() != null && req.getContentType().contains("json")) {
                return dataMap;
            } else {
                req.setAttribute("balancesJSON", JSON.getMapper().writeValueAsString(dataMap));
                context.getRequestDispatcher("/balance/week.xhtml").forward(req, resp);
                return Response.ok().build();
            }
        }
    }


    @Path("/weekdb/{year}/{month}/{day}") // old_url -> /balance/weekdb/$year/$month/$day
    @GET  @Produces(MediaType.APPLICATION_JSON)
    public Object weekdb(@PathParam("year") int year, @PathParam("month") int month, @PathParam("day") int day,
                       @Context ServletContext context, @Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws IOException, ServletException {
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar);
        //TODO balancesBean.calculatePeriod(DateUtils.getWeekPeriod(calendar))
        return new HashMap<>();
    }

}
