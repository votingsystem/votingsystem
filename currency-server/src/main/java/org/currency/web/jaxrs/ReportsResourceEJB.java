package org.currency.web.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.currency.web.ejb.ConfigCurrencyServer;
import org.currency.web.util.AuditLogger;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.http.MediaType;
import org.votingsystem.util.*;

import javax.ejb.Stateless;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/reports")
public class ReportsResourceEJB {

    private static final Logger log = Logger.getLogger(ReportsResourceEJB.class.getName());

    @Inject private ConfigCurrencyServer config;


    @Path("/")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response index(@Context ServletContext context,
                         @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        File weekReportsBaseDir = new File(config.getApplicationDataPath() +  "/backup/week-reports");
        List<Interval> periods = new ArrayList<>();
        if(weekReportsBaseDir.exists()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            Files.list(weekReportsBaseDir.toPath()).forEach(file -> {
                try {
                    String[] fileNameParts = file.getFileName().toString().split("_");
                    Interval timePeriod = new Interval(
                            LocalDate.parse(fileNameParts[0], formatter).atStartOfDay(ZoneId.systemDefault()),
                            LocalDate.parse(fileNameParts[1], formatter).atStartOfDay(ZoneId.systemDefault()));
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
        File reportsFile = AuditLogger.getReportsLogFile();
        if(!reportsFile.exists()) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - not found - file: " + AuditLogger.getReportsLogFile().getAbsolutePath()).build();
        StringBuilder stringBuilder = new StringBuilder("{\"resultList\":[");
        stringBuilder.append(FileUtils.getStringFromFile(reportsFile));
        stringBuilder.append("]}");
        return Response.ok().type(MediaType.JSON).entity(stringBuilder.toString()).build();
    }

    @Path("/transaction")
    @GET  @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON) @Transactional
    public Response transaction(@Context ServletContext context, @QueryParam("transactionType") String transactionType,
                                @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        File reportsFile = AuditLogger.getTransactionsLogFile();
        if(!reportsFile.exists()) return Response.status(Response.Status.NOT_FOUND).entity(
                "ERROR - not found - file: " + AuditLogger.getTransactionsLogFile().getAbsolutePath()).build();
        ObjectMapper mapper =  JSON.getMapper();
        String result = null;
        AtomicLong totalCount = new AtomicLong(0);
        if(transactionType != null) {
            List<TransactionDto> resultList = new ArrayList<>();
            CurrencyOperation type = CurrencyOperation.valueOf(transactionType);
            Files.readAllLines(Paths.get(reportsFile.toURI()), Charset.defaultCharset()).forEach(line -> {
                    totalCount.getAndIncrement();
                    try {
                        TransactionDto transactionDto =  mapper.readValue(line, TransactionDto.class);
                        if(type == transactionDto.getOperation().getCurrencyOperationType()) {
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
