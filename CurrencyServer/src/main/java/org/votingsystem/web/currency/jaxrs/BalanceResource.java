package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.currency.ejb.BalancesBean;
import org.votingsystem.web.currency.ejb.CurrencyAccountBean;
import org.votingsystem.web.currency.util.ReportFiles;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
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

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CurrencyAccountBean accountBean;
    @Inject BalancesBean balancesBean;

    @GET
    @Path("/userVS/id/{userId}")
    public Response userVS(@PathParam("userId") long userId, @Context ServletContext context,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        return getUserVSBalancesDto(req, resp, context, userId, DateUtils.getWeekPeriod(Calendar.getInstance()));
    }

    @Path("/userVS/id/{userId}/{timePeriod}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object userVS(@PathParam("userId") long userId, @PathParam("timePeriod") String lapseStr,
                         @Context ServletContext context, @Context HttpServletRequest req,
                         @Context HttpServletResponse resp) throws Exception {
        UserVS userVS = dao.find(UserVS.class, userId);
        if(userVS == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("not found - userId: " + userId).build();
        }
        TimePeriod.Lapse lapse =  TimePeriod.Lapse.valueOf(lapseStr.toUpperCase());
        TimePeriod timePeriod = DateUtils.getLapsePeriod(Calendar.getInstance(req.getLocale()).getTime(), lapse);
        return balancesBean.getBalancesDto(userVS, timePeriod);
    }

    @Path("/userVS/id/{userId}/{year}/{month}/{day}")
    @GET  @Produces(MediaType.APPLICATION_JSON)
    public Response userVSWithDate(@PathParam("userId") long userId, @PathParam("year") int year, @PathParam("month") int month,
            @PathParam("day") int day, @Context ServletContext context, @Context HttpServletRequest req,
            @Context HttpServletResponse resp) throws Exception {
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        return getUserVSBalancesDto(req, resp, context, userId, DateUtils.getWeekPeriod(calendar));
    }

    private Response getUserVSBalancesDto(HttpServletRequest req, HttpServletResponse resp, ServletContext context,
               long userId, TimePeriod timePeriod) throws Exception {
        UserVS uservs = dao.find(UserVS.class, userId);
        if(uservs == null) {
            throw new NotFoundException("ERROR - UserVS not found - userId: " + userId);
        } else {
            BalancesDto balancesDto = balancesBean.getBalancesDto(uservs, timePeriod);
            if(req.getContentType() != null && req.getContentType().contains("json")) {
                return Response.ok().type(MediaTypeVS.JSON).entity(
                        JSON.getMapper().writeValueAsBytes(balancesDto)).build();
            } else {
                req.setAttribute("balancesDto", JSON.getMapper().writeValueAsString(balancesDto));
                context.getRequestDispatcher("/balance/userVS.xhtml").forward(req, resp);
                return Response.ok().build();
            }
        }
    }

    @Path("/userVS/id/{userId}/db")
    @GET  @Produces(MediaType.APPLICATION_JSON)
    public Map userVSDB(@PathParam("userId") long userId, @Context ServletContext context,
            @Context HttpServletRequest req, @Context HttpServletResponse resp) {
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
            Map<String, Object> dataMap = JSON.getMapper().readValue(reportFiles.getJsonFile(),
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
