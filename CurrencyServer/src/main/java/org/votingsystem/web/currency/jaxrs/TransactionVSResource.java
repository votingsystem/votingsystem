package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.dto.voting.MetaInf;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.web.currency.cdi.ConfigVSImpl;
import org.votingsystem.web.currency.ejb.BalancesBean;
import org.votingsystem.web.currency.ejb.CurrencyBean;
import org.votingsystem.web.currency.ejb.TransactionVSBean;
import org.votingsystem.web.currency.ejb.UserVSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/transactionVS")
public class TransactionVSResource {

    private static final Logger log = Logger.getLogger(TransactionVSResource.class.getSimpleName());

    @Inject UserVSBean serVSBean;
    @Inject TransactionVSBean transactionVSBean;
    @Inject SignatureBean signatureBean;
    @Inject CurrencyBean currencyBean;
    @Inject BalancesBean balancesBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;

    @Path("/id/{id}")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response get(@PathParam("id") long id) throws UnsupportedEncodingException, JsonProcessingException {
        TransactionVS transactionVS = dao.find(TransactionVS.class, id);
        if(transactionVS != null) {
            TransactionVSDto transactionDto = transactionVSBean.getTransactionDto(transactionVS);
            transactionDto.setReceipt(new String(transactionVS.getMessageSMIME().getContent(), "UTF-8"));
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(transactionDto)).build();
        } else return Response.status(Response.Status.NOT_FOUND).entity("ERROR - TransactionVS not found - id: " + id).build();
    }

    @Path("/")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response index(@DefaultValue("0") @QueryParam("offset") int offset,
              @DefaultValue("100") @QueryParam("max") int max,
              @QueryParam("transactionvsType") String transactionvsType, @Context ServletContext context,
              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        List<TransactionVS.Type> transactionTypeList = Arrays.asList(TransactionVS.Type.values());
        try {
            if(transactionvsType != null) transactionTypeList = Arrays.asList(TransactionVS.Type.valueOf(transactionvsType));
        } catch(Exception ex) {}
        Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(TransactionVS.class);
        criteria.add(Restrictions.isNull("transactionParent"));
        criteria.add(Restrictions.in("type", transactionTypeList));
        List<TransactionVS> transactionList = criteria.setFirstResult(offset).setMaxResults(max).list();
        List<TransactionVSDto> resultList = new ArrayList<>();
        for(TransactionVS transactionVS : transactionList) {
            resultList.add(transactionVSBean.getTransactionDto(transactionVS));
        }
        long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }

    @Path("/") @POST @Produces(MediaType.APPLICATION_JSON)
    public Response post(MessageSMIME messageSMIME, @Context HttpServletRequest req) throws Exception {
        ResultListDto dto = transactionVSBean.processTransactionVS(messageSMIME);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(dto)).build();
    }

    @Transactional
    @Path("/from/{dateFrom}/to/{dateTo}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response search(@DefaultValue("0") @QueryParam("offset") int offset,
                       @DefaultValue("100") @QueryParam("max") int max,
                       @QueryParam("transactionvsType") String transactionvsType,
                       @QueryParam("tag") String tag,
                       @QueryParam("searchText") String searchText,
                       @QueryParam("contentType") String contentType,
                       @PathParam("dateFrom") String dateFromStr, @PathParam("dateTo") String dateToStr,
                       @Context ServletContext context, @Context HttpServletRequest req,
                       @Context HttpServletResponse resp) throws IOException, ParseException, ServletException, ValidationExceptionVS {
        TransactionVS.Type transactionType = null;
        BigDecimal amount = null;
        Date dateFrom = DateUtils.getURLPart(dateFromStr);
        Date dateTo = DateUtils.getURLPart(dateToStr);
        try {
            if(transactionvsType != null && !"ALL".equals(transactionvsType.toUpperCase()))
                transactionType = TransactionVS.Type.valueOf(transactionvsType);
            else transactionType = TransactionVS.Type.valueOf(searchText);} catch(Exception ex) {}
        try {amount = new BigDecimal(searchText);} catch(Exception ex) {}
        Criteria criteria = dao.getEM().unwrap(Session.class).createCriteria(TransactionVS.class);
        criteria.add(Restrictions.between("dateCreated", dateFrom, dateTo));
        criteria.add(Restrictions.isNull("transactionParent"));
        Disjunction orDisjunction = Restrictions.or();
        if(transactionType != null) orDisjunction.add(Restrictions.eq("type", transactionType));
        if(amount != null) orDisjunction.add(Restrictions.eq("amount", amount));
        if(tag != null) {
            criteria.add(Restrictions.eq("tag", config.getTag(tag)));
        }
        if(searchText != null) {
            orDisjunction.add(Restrictions.ilike("subject", "%" + searchText + "%"));
            orDisjunction.add(Restrictions.ilike("currencyCode", "%" + searchText + "%"));
        }
        criteria.add(orDisjunction);
        List<TransactionVS> transactionList = criteria.setFirstResult(offset).setMaxResults(max).list();
        if("zip".equals(contentType)) {
            String tempPath = ConfigVSImpl.getTempPath() + File.separator + UUID.randomUUID().toString();
            File tempDir = new File(config.getServerDir().getAbsolutePath() + tempPath);
            tempDir.mkdirs();
            File metaInfFile = new File(config.getServerDir().getAbsolutePath() + tempPath + File.separator + "meta.inf");
            MetaInf metaInf = new MetaInf().setType(TypeVS.TRANSACTIONVS_INFO);
            JSON.getMapper().writeValue(new FileOutputStream(metaInfFile), metaInf);
            String desc = (transactionType == null? "":transactionvsType) + (tag == null? "":tag);
            File zipFile = new File (tempDir, "transaction_" + desc + "_" + transactionList.size() +  ".zip");
            for(TransactionVS transactionVS :  transactionList) {
                File smimeFile = new File(format("{0}/transaction_{1}.p7m", tempDir.getAbsolutePath(), transactionVS.getId()));
                IOUtils.write(transactionVS.getMessageSMIME().getContent(), new FileOutputStream(smimeFile));
            }
            new ZipUtils(tempDir).zipIt(zipFile);
            resp.sendRedirect(config.getStaticResURL() + tempPath + File.separator + zipFile.getName());
            return Response.ok().build();
        } else {
            List<TransactionVSDto> resultList = new ArrayList<>();
            for(TransactionVS transactionVS :  transactionList) {
                resultList.add(transactionVSBean.getTransactionDto(transactionVS));
            }
            long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
            ResultListDto resultListDto = new ResultListDto(resultList, offset, max, totalCount);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
        }
    }

    @Path("/userVS/id/{userId}/{timePeriod}") @Transactional
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response transactionsTo(@PathParam("userId") long userId, @Context ServletContext context,
             @PathParam("timePeriod") String lapseStr,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        UserVS userVS = dao.find(UserVS.class, userId);
        if(userVS == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("not found - userId: " + userId).build();
        }
        Interval.Lapse lapse =  Interval.Lapse.valueOf(lapseStr.toUpperCase());
        Interval timePeriod = DateUtils.getLapsePeriod(Calendar.getInstance(req.getLocale()).getTime(), lapse);
        List<TransactionVS> transactionsToList = transactionVSBean.getTransactionToList(userVS, timePeriod);
        List<TransactionVSDto> transactionsToListDto = new ArrayList<>();
        for(TransactionVS transaction : transactionsToList) {
            transactionsToListDto.add(transactionVSBean.getTransactionDto(transaction));
        }
        List<TransactionVS> transactionsFromList = transactionVSBean.getTransactionFromList(userVS, timePeriod);
        List<TransactionVSDto> transactionsFromListDto = new ArrayList<>();
        for(TransactionVS transaction : transactionsFromList) {
            transactionsFromListDto.add(transactionVSBean.getTransactionDto(transaction));
        }
        BalancesDto balancesDto = new BalancesDto();
        balancesDto.setTransactionToList(transactionsToListDto);
        balancesDto.setTransactionFromList(transactionsFromListDto);
        if(contentType.contains("json")) return Response.ok().entity(
                JSON.getMapper().writeValueAsBytes(balancesDto)).build();
        else {
            req.getSession().setAttribute("balancesDto", JSON.getMapper().writeValueAsString(balancesDto));
            return Response.temporaryRedirect(new URI("../transactionVS/userVS.xhtml")).build();
        }
    }

    @Path("/currency")
    @POST @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
    public Response currency(byte[] postData) throws Exception {
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(
                currencyBean.processCurrencyBatch(CurrencyBatchDto.FROM_BYTES(postData)))).build() ;
    }

}
