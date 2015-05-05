package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.currency.TransactionVS;
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
import javax.ws.rs.core.MediaType;
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

    private static final DateFormat formatter = new SimpleDateFormat("yyyyMMdd");

    @Inject ConfigVS config;

    @Path("/{year}/{month}/{day}/week")
    @GET  @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response week(@PathParam("year") int year, @PathParam("month") int month, @PathParam("day") int day,
                          @Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        File reportsFile = new File(LoggerVS.weekPeriodLogPath);
        Calendar calendar = DateUtils.getCalendar(year, month, day);

        TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar);

        if(reportsFile.exists()) {
            StringBuilder stringBuilder = new StringBuilder("{");
            stringBuilder.append(FileUtils.getStringFromFile(reportsFile));
            stringBuilder.append("}");
            if(contentType.contains("json")) {
                return Response.ok().type(MediaTypeVS.JSON).entity(stringBuilder.toString()).build();
            } else {
                req.setAttribute("reportsDto", stringBuilder.toString());
                context.getRequestDispatcher("/reports/week.xhtml").forward(req, resp);
                return Response.ok().build();
            }
        } else return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - not found - file: " + LoggerVS.weekPeriodLogPath).build();
    }

    @Path("/")
    @GET  @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response index(@Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        File weekReportsBaseDir = new File(config.getServerDir() +  "/weekReports");
        List<TimePeriod> periods = new ArrayList<>();
        if(weekReportsBaseDir.exists()) {
            Files.list(weekReportsBaseDir.toPath()).forEach(file -> {
                try {
                    String[] fileNameParts = file.getFileName().toString().split("_");
                    TimePeriod timePeriod = new TimePeriod(formatter.parse(fileNameParts[0]), formatter.parse(fileNameParts[1]));
                    periods.add(timePeriod);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });
        }
        req.setAttribute("periods", periods);
        context.getRequestDispatcher("/reports/index.xhtml").forward(req, resp);
        return Response.ok().build();
    }

    @Path("/logs")
    @GET  @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response logs(@Context ServletContext context,
                          @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        File reportsFile = new File(LoggerVS.reporstLogPath);
        if(!reportsFile.exists()) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - not found - file: " + LoggerVS.reporstLogPath).build();
        StringBuilder stringBuilder = new StringBuilder("{\"resultList\":[");
        stringBuilder.append(FileUtils.getStringFromFile(reportsFile));
        stringBuilder.append("]}");

        if(contentType.contains("json")) {
            return Response.ok().type(MediaTypeVS.JSON).entity(stringBuilder.toString()).build();
        } else {
            req.setAttribute("logData", stringBuilder.toString());
            context.getRequestDispatcher("/reports/logs.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/transactionvs")
    @GET  @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response transactionvs(@Context ServletContext context, @QueryParam("transactionvsType") String transactionvsType,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        File reportsFile = new File(LoggerVS.transactionsLogPath);
        if(!reportsFile.exists()) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - not found - file: " + LoggerVS.reporstLogPath).build();
        ObjectMapper mapper =  JSON.getMapper();
        String result = null;
        AtomicLong totalCount = new AtomicLong(0);
        if(transactionvsType != null) {
            List<TransactionVSDto> resultList = new ArrayList<>();
            TransactionVS.Type type = TransactionVS.Type.valueOf(transactionvsType);
            Files.readAllLines(Paths.get(reportsFile.toURI()), Charset.defaultCharset()).forEach(line -> {
                    totalCount.getAndIncrement();
                    try {
                        TransactionVSDto transactionVSDto =  mapper.readValue(line, TransactionVSDto.class);
                        if(type == transactionVSDto.getType()) {
                            resultList.add(transactionVSDto);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            );
            ResultListDto<TransactionVSDto> resultListDto = new ResultListDto<>(resultList);
            result = mapper.writeValueAsString(resultListDto);
        } else {
            StringBuilder stringBuilder = new StringBuilder("{\"resultList\":[");
            stringBuilder.append(FileUtils.getStringFromFile(reportsFile));
            stringBuilder.append("]}");
            result = stringBuilder.toString();
        }
        if(contentType.contains("json")) {
            return Response.ok().type(MediaTypeVS.JSON).entity(result).build();
        } else {
            req.setAttribute("transactionsDto", result);
            context.getRequestDispatcher("/reports/transactionvs.xhtml").forward(req, resp);
            return Response.ok().build();
        }
    }

}
