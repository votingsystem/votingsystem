package org.votingsystem.web.currency.jaxrs;

import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyTransactionBatch;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.MediaTypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.currency.ejb.CurrencyBean;
import org.votingsystem.web.currency.ejb.TransactionVSBean;
import org.votingsystem.web.currency.ejb.UserVSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/transactionVS")
public class TransactionVSResource {

    private static final Logger log = Logger.getLogger(TransactionVSResource.class.getSimpleName());

    @Inject UserVSBean serVSBean;
    @Inject TransactionVSBean transactionVSBean;
    @Inject
    SignatureBean signatureBean;
    @Inject CurrencyBean currencyBean;
    @Inject
    DAOBean dao;
    @Inject ConfigVS config;

    @Path("/id/{id}") //old_url -> /transactionVS/$id
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object get(@PathParam("id") long id) throws UnsupportedEncodingException {
        Map resultMap = null;
        TransactionVS transactionVS = dao.find(TransactionVS.class, id);
        if(transactionVS != null) {
            resultMap = transactionVSBean.getTransactionMap(transactionVS);
            resultMap.put("receipt", new String(transactionVS.getMessageSMIME().getContent(), "UTF-8"));
        } else resultMap = new HashMap<>();
        return resultMap;
    }

    @Path("/")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object index(@DefaultValue("0") @QueryParam("offset") int offset,
              @DefaultValue("100") @QueryParam("max") int max, @Context ServletContext context,
              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        Query query = dao.getEM().createQuery("select t from TransactionVS t where t.transactionParent is null")
                .setMaxResults(max).setFirstResult(offset);
        List<TransactionVS> transactionList = query.getResultList();
        query = dao.getEM().createQuery("select COUNT(t) from TransactionVS t where t.transactionParent is null");
        long totalCount = (long) query.getSingleResult();
        List<Map> resultList = new ArrayList<>();
        for(TransactionVS transactionVS : transactionList) {
            resultList.add(transactionVSBean.getTransactionMap(transactionVS));
        }
        Map resultMap = new HashMap<>();
        resultMap.put("transactionRecords", resultList);
        resultMap.put("offset", offset);
        resultMap.put("max", max);
        resultMap.put("totalCount", totalCount);

        if(contentType.contains("json")) return resultMap;
        else {
            req.setAttribute("transactionsMap", JSON.getInstance().writeValueAsString(resultMap));
            context.getRequestDispatcher("/jsf/transactionVS/index.jsp").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/") @POST @Consumes(MediaTypeVS.JSON_SIGNED)
    public Object  post(MessageSMIME messageSMIME, @Context HttpServletRequest req) throws Exception {
        return Response.ok().entity(transactionVSBean.processTransactionVS(messageSMIME)).build();
    }

    @Path("/from/{dateFrom}/to/{dateTo}") //old_url -> /transactionVS/from/$dateFrom/to/$dateTo
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object search(@DefaultValue("0") @QueryParam("offset") int offset,
                       @DefaultValue("100") @QueryParam("max") int max,
                       @QueryParam("transactionvsType") String transactionvsType,
                       @QueryParam("searchText") String searchText,
                       @PathParam("dateFrom") String dateFromStr, @PathParam("dateTo") String dateToStr,
                       @Context ServletContext context, @Context HttpServletRequest req,
                       @Context HttpServletResponse resp) throws IOException, ParseException, ServletException {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        TransactionVS.Type transactionType = null;
        BigDecimal amount = null;
        Date dateFrom = DateUtils.getURLDate(dateFromStr);
        Date dateTo = DateUtils.getURLDate(dateToStr);
        try {
            if(transactionvsType != null) transactionType = TransactionVS.Type.valueOf(transactionvsType);
            else transactionType = TransactionVS.Type.valueOf(searchText);} catch(Exception ex) {}
        try {amount = new BigDecimal(searchText);} catch(Exception ex) {}
        String queryListPrefix = "select t from TransactionVS t ";
        String queryCountPrefix = "select COUNT(t) from TransactionVS t ";
        String querySufix = "where t.transactionParent is null " +
                "and t.dateCreated between :dateFrom and :dateTo and (t.type =:type or t.amount =:amount " +
                "or t.subject like :searchText or t.currencyCode like :searchText)";
        Query query = dao.getEM().createQuery(queryListPrefix + querySufix).setParameter("dateFrom", dateFrom)
                .setParameter("dateTo", dateTo).setParameter("type", transactionType).setParameter("amount", amount)
                .setParameter("searchText", searchText).setFirstResult(offset).setMaxResults(max);
        List<TransactionVS> transactionList = query.getResultList();
        query = dao.getEM().createQuery(queryCountPrefix + querySufix).setParameter("dateFrom", dateFrom)
                .setParameter("dateTo", dateTo).setParameter("type", transactionType).setParameter("amount", amount)
                .setParameter("searchText", searchText);
        long totalCount = (long) query.getSingleResult();
        List<Map> resultList = new ArrayList<>();
        for(TransactionVS transactionVS :  transactionList) {
            resultList.add(transactionVSBean.getTransactionMap(transactionVS));
        }
        Map resultMap = new HashMap<>();
        resultMap.put("transactionRecords", resultList);
        resultMap.put("offset", offset);
        resultMap.put("max", max);
        resultMap.put("totalCount", totalCount);

        if(contentType.contains("json")) return resultMap;
        else {
            req.setAttribute("transactionsMap", JSON.getInstance().writeValueAsString(resultMap));
            context.getRequestDispatcher("/jsf/transactionVS/index.jsp").forward(req, resp);
            return Response.ok().build();
        }
    }

    @Path("/userVS/id/{userId}/{timePeriod}") //old_url -> /userVS/$id/transacionVS/$timePeriod
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object userVS(@PathParam("userId") long userId, @PathParam("timePeriod") String lapseStr,
                         @Context ServletContext context, @Context HttpServletRequest req,
                         @Context HttpServletResponse resp) throws Exception {
        UserVS userVS = dao.find(UserVS.class, userId);
        if(userVS == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("not found - userId: " + userId).build();
        }
        DateUtils.TimePeriod.Lapse lapse =  DateUtils.TimePeriod.Lapse.valueOf(lapseStr.toUpperCase());
        DateUtils.TimePeriod timePeriod = DateUtils.getLapsePeriod(Calendar.getInstance(req.getLocale()).getTime(), lapse);
        return transactionVSBean.getDataWithBalancesMap(userVS, timePeriod);
    }

    @Path("/currency")
    @POST @Produces(MediaType.APPLICATION_JSON) @Consumes({"application/json"})
    public Object currency(Map transactionBatchMap) throws Exception {
        return currencyBean.processCurrencyTransaction(new CurrencyTransactionBatch(transactionBatchMap));
    }
}
