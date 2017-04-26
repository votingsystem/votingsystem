package org.currency.web.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.europa.esig.dss.InMemoryDocument;
import org.apache.commons.io.IOUtils;
import org.currency.web.ejb.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.dto.metadata.MetaInfDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.http.HttpResponse;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;
import org.votingsystem.xml.XML;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
@Path("/transaction")
public class TransactionResourceEJB {

    private static final Logger log = Logger.getLogger(TransactionResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private UserEJB serVSBean;
    @Inject private TransactionEJB transactionBean;
    @Inject private SignatureService signatureService;
    @Inject private CurrencyEJB currencyBean;
    @Inject private BalancesEJB balancesBean;
    @Inject private ConfigCurrencyServer config;

    @Path("/id/{id}")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response get(@PathParam("id") long id) throws UnsupportedEncodingException, JsonProcessingException {
        Transaction transaction = em.find(Transaction.class, id);
        if(transaction != null) {
            TransactionDto transactionDto = transactionBean.getTransactionDto(transaction);
            transactionDto.setSignedDocumentBase64(
                    Base64.getEncoder().encodeToString(transaction.getSignedDocument().getBody().getBytes()));
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(transactionDto)).build();
        } else return Response.status(Response.Status.NOT_FOUND).entity("ERROR - Transaction not found - id: " + id).build();
    }

    @Path("/")
    @GET @Produces(MediaType.APPLICATION_JSON) @Transactional
    public Response index(@DefaultValue("0") @QueryParam("offset") int offset,
              @DefaultValue("100") @QueryParam("max") int max,
              @QueryParam("transactionType") String transactionType, @Context ServletContext context,
              @Context HttpServletRequest req, @Context HttpServletResponse resp) throws IOException, ServletException {
        List<CurrencyOperation> transactionTypeList = Arrays.asList(CurrencyOperation.TRANSACTION_FROM_BANK,
                CurrencyOperation.TRANSACTION_FROM_USER, CurrencyOperation.CURRENCY_REQUEST, CurrencyOperation.CURRENCY_CHANGE);

        try {
            if(transactionType != null) transactionTypeList = Arrays.asList(CurrencyOperation.valueOf(transactionType));
        } catch(Exception ex) {}
        Criteria criteria = em.unwrap(Session.class).createCriteria(Transaction.class);
        criteria.add(Restrictions.isNull("transactionParent"));
        criteria.add(Restrictions.in("type", transactionTypeList));
        List<Transaction> transactionList = criteria.setFirstResult(offset).setMaxResults(max).list();
        List<TransactionDto> resultList = new ArrayList<>();
        for(Transaction transaction : transactionList) {
            resultList.add(transactionBean.getTransactionDto(transaction));
        }
        criteria.setFirstResult(0); //reset offset for total count
        long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
        ResultListDto resultListDto = new ResultListDto(resultList, offset, max, totalCount);
        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
    }

    @Path("/") @POST @Produces(MediaType.APPLICATION_JSON)
    public Response post(SignedDocument signedDocument, @Context HttpServletRequest req) throws Exception {
        ResultListDto dto = transactionBean.processTransaction(signedDocument);
        return Response.ok().entity(XML.getMapper().writeValueAsBytes(dto)).build();
    }

    @Transactional
    @Path("/from/{dateFrom}/to/{dateTo}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response search(@DefaultValue("0") @QueryParam("offset") int offset,
                       @DefaultValue("10000") @QueryParam("max") int max,
                       @QueryParam("transactionType") String transactionTypeStr,
                       @QueryParam("tag") String tag,
                       @QueryParam("searchText") String searchText,
                       @QueryParam("contentType") String contentType,
                       @PathParam("dateFrom") String dateFromStr, @PathParam("dateTo") String dateToStr,
                       @Context ServletContext context, @Context HttpServletRequest req,
                       @Context HttpServletResponse resp) throws IOException, ParseException, ServletException, ValidationException {
        CurrencyOperation transactionType = null;
        BigDecimal amount = null;
        LocalDateTime dateFrom = DateUtils.getURLPart(dateFromStr);
        LocalDateTime dateTo = DateUtils.getURLPart(dateToStr);
        try {
            if(transactionType != null && !"ALL".equals(transactionTypeStr.toUpperCase()))
                transactionType = CurrencyOperation.valueOf(transactionTypeStr);
            else transactionType = CurrencyOperation.valueOf(searchText);} catch(Exception ex) {}
        try {amount = new BigDecimal(searchText);} catch(Exception ex) {}
        Criteria criteria = em.unwrap(Session.class).createCriteria(Transaction.class);
        criteria.add(Restrictions.between("dateCreated", dateFrom, dateTo));
        criteria.add(Restrictions.isNull("transactionParent"));
        Disjunction orDisjunction = Restrictions.or();
        if(transactionType != null) orDisjunction.add(Restrictions.eq("type", transactionType));
        if(amount != null) orDisjunction.add(Restrictions.eq("amount", amount));
        if(searchText != null) {
            orDisjunction.add(Restrictions.ilike("subject", "%" + searchText + "%"));
            orDisjunction.add(Restrictions.ilike("currencyCode", "%" + searchText + "%"));
        }
        criteria.add(orDisjunction);
        List<Transaction> transactionList = criteria.setFirstResult(offset).setMaxResults(max).list();
        if("zip".equals(contentType)) {
            String tempPath = config.getTempDir() + File.separator + UUID.randomUUID().toString();
            File tempDir = new File(config.getApplicationDataPath() + tempPath);
            tempDir.mkdirs();
            File metaInfFile = new File(tempPath + File.separator + "meta.inf");
            MetaInfDto metaInf = new MetaInfDto().setOperation(CurrencyOperation.TRANSACTION_INFO);
            JSON.getMapper().writeValue(new FileOutputStream(metaInfFile), metaInf);
            String desc = (transactionType == null? "":transactionType) + (tag == null? "":tag);
            File zipFile = new File (tempDir, "transaction_" + desc + "_" + transactionList.size() +  ".zip");
            for(Transaction transaction :  transactionList) {
                File cmsFile = new File(format("{0}/transaction_{1}.p7s", tempDir.getAbsolutePath(), transaction.getId()));
                IOUtils.write(transaction.getSignedDocument().getBody(), new FileOutputStream(cmsFile));
            }
            new ZipUtils(tempDir).zipIt(zipFile);
            resp.sendRedirect(config.getStaticResourcesURL() + tempPath + File.separator + zipFile.getName());
            return Response.ok().build();
        } else {
            List<TransactionDto> resultList = new ArrayList<>();
            for(Transaction transaction :  transactionList) {
                resultList.add(transactionBean.getTransactionDto(transaction));
            }
            criteria.setFirstResult(0); //reset offset for total count
            long totalCount = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).longValue();
            ResultListDto resultListDto = new ResultListDto(resultList, offset, max, totalCount);
            return Response.ok().entity(JSON.getMapper().writeValueAsBytes(resultListDto)).build();
        }
    }

    @Path("/user/id/{userId}/{timePeriod}") @Transactional
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response transactionsTo(@PathParam("userId") long userId, @Context ServletContext context,
             @PathParam("timePeriod") String lapseStr,
             @Context HttpServletRequest req, @Context HttpServletResponse resp) throws Exception {
        String contentType = req.getContentType() != null ? req.getContentType():"";
        User user = em.find(User.class, userId);
        if(user == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("not found - userId: " + userId).build();
        }
        Interval.Lapse lapse =  Interval.Lapse.valueOf(lapseStr.toUpperCase());
        Interval timePeriod = DateUtils.getLapsePeriod(LocalDateTime.now(), lapse);
        List<Transaction> transactionsToList = transactionBean.getTransactionToList(user, timePeriod);
        List<TransactionDto> transactionsToListDto = new ArrayList<>();
        for(Transaction transaction : transactionsToList) {
            transactionsToListDto.add(transactionBean.getTransactionDto(transaction));
        }
        List<Transaction> transactionsFromList = transactionBean.getTransactionFromList(user, timePeriod);
        List<TransactionDto> transactionsFromListDto = new ArrayList<>();
        for(Transaction transaction : transactionsFromList) {
            transactionsFromListDto.add(transactionBean.getTransactionDto(transaction));
        }
        BalancesDto balancesDto = new BalancesDto();
        balancesDto.setTransactionToList(transactionsToListDto);
        balancesDto.setTransactionFromList(transactionsFromListDto);
        if(contentType.contains("json")) return Response.ok().entity(
                JSON.getMapper().writeValueAsBytes(balancesDto)).build();
        else {
            req.getSession().setAttribute("balancesDto", JSON.getMapper().writeValueAsString(balancesDto));
            return Response.temporaryRedirect(new URI("../transaction/user.xhtml")).build();
        }
    }

    @Path("/currency")
    @POST @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
    public Response currency(byte[] postData, @Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        SignatureParams signatureParams = new SignatureParams(null, User.Type.ID_CARD_USER,
                SignedDocumentType.CURRENCY_CHANGE).setWithTimeStampValidation(true);
        SignedDocument signedDocument = signatureService.validateXAdESAndSave(
                new InMemoryDocument(postData), signatureParams);
        CurrencyBatchResponseDto responseDto = currencyBean.processCurrencyBatch(signedDocument);
        return HttpResponse.sendResponseDto(ResponseDto.SC_OK, req, res, responseDto);
    }

}