package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.*;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/reports")
public class ReportsResource {

    private static final Logger log = Logger.getLogger(ServerInfoResource.class.getName());

    @Inject ConfigVS config;

    @Path("/{year}/{month}/{day}/week")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response week(@PathParam("year") int year, @PathParam("month") int month, @PathParam("day") int day,
                          @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        File reportsFile = new File(LoggerVS.getWeekPeriodLogPath());
        Calendar calendar = DateUtils.getCalendar(year, month, day);
        Interval timePeriod = DateUtils.getWeekPeriod(calendar);
        //TODO
        if(reportsFile.exists()) {
            StringBuilder stringBuilder = new StringBuilder("{");
            stringBuilder.append(FileUtils.getStringFromFile(reportsFile));
            stringBuilder.append("}");
            return Response.ok().type(MediaType.JSON).entity(stringBuilder.toString()).build();
        } else return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - not found - file: " + LoggerVS.getWeekPeriodLogPath()).build();
    }

    @Path("/")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response index(@Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        File weekReportsBaseDir = new File(config.getServerDir() +  "/backup/weekReports");
        List<Interval> periods = new ArrayList<>();
        if(weekReportsBaseDir.exists()) {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Files.list(weekReportsBaseDir.toPath()).forEach(file -> {
                try {
                    String[] fileNameParts = file.getFileName().toString().split("_");
                    Interval timePeriod = new Interval(formatter.parse(fileNameParts[0]), formatter.parse(fileNameParts[1]));
                    periods.add(timePeriod);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });
        }
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(periods)).build();
    }

    @Path("/logs")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response logs(@Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        File reportsFile = new File(LoggerVS.getReporstLogPath());
        if(!reportsFile.exists()) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - not found - file: " + LoggerVS.getReporstLogPath()).build();
        StringBuilder stringBuilder = new StringBuilder("{\"resultList\":[");
        stringBuilder.append(FileUtils.getStringFromFile(reportsFile));
        stringBuilder.append("]}");
        return Response.ok().type(MediaType.JSON).entity(stringBuilder.toString()).build();
    }

    @Path("/transaction")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response transaction(@Context ServletContext context, @QueryParam("transactionType") String transactionType,
                                @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        File reportsFile = new File(LoggerVS.getTransactionsLogPath());
        if(!reportsFile.exists()) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - not found - file: " + LoggerVS.getReporstLogPath()).build();
        ObjectMapper mapper =  JSON.getMapper();
        String result = null;
        AtomicLong totalCount = new AtomicLong(0);
        if(transactionType != null) {
            List<TransactionDto> resultList = new ArrayList<>();
            Transaction.Type type = Transaction.Type.valueOf(transactionType);
            Files.readAllLines(Paths.get(reportsFile.toURI()), Charset.defaultCharset()).forEach(line -> {
                    totalCount.getAndIncrement();
                    try {
                        TransactionDto transactionDto =  mapper.readValue(line, TransactionDto.class);
                        if(type == transactionDto.getType()) {
                            resultList.add(transactionDto);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            );
            ResultListDto<TransactionDto> resultListDto = new ResultListDto<>(resultList);
            result = mapper.writeValueAsString(resultListDto);
        } else {
            StringBuilder stringBuilder = new StringBuilder("{\"resultList\":[");
            stringBuilder.append(FileUtils.getStringFromFile(reportsFile));
            stringBuilder.append("]}");
            result = stringBuilder.toString();
        }
        return Response.ok().type(MediaType.JSON).entity(result).build();
    }

}
