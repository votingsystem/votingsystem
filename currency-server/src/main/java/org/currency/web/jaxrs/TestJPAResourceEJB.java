package org.currency.web.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.currency.web.ejb.ConfigCurrencyServer;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test-jpa")
public class TestJPAResourceEJB {

    private static final Logger log = Logger.getLogger(TestJPAResourceEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;

    @GET @Path("/sum-balance")
    public Response sumBalance(@Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws JsonProcessingException, ValidationException {
        Map<String, Map> resultMap = new HashMap<>();
        List<Object[]> resultList = em.createQuery("select SUM(c.balance), c.currencyCode from CurrencyAccount c " +
                "where c.state =:state and c.user=:user group by c.currencyCode")
                .setParameter("user", config.getSystemUser())
                .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        Map<CurrencyCode, BigDecimal> totalBalanceMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            totalBalanceMap.put((CurrencyCode)listItem[1], (BigDecimal) listItem[0]);
        }
        resultMap.put("totalBalanceMap", totalBalanceMap);

        resultList = em.createQuery("select SUM(t.amount), t.currencyCode from Transaction t " +
                "where t.state =:state and t.type=:transactionType group by t.currencyCode")
                .setParameter("transactionType", CurrencyOperation.TRANSACTION_FROM_BANK)
                .setParameter("state", Transaction.State.OK).getResultList();
        Map<CurrencyCode, BigDecimal> transactionsFromBanksMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            transactionsFromBanksMap.put((CurrencyCode)listItem[1], (BigDecimal) listItem[0]);
        }
        resultMap.put("transactionsFromBanksMap", transactionsFromBanksMap);

        resultList = em.createQuery("select SUM(t.amount), t.currencyCode from Transaction t " +
                "where t.state =:state and t.type=:transactionType group by t.currencyCode")
                .setParameter("transactionType", CurrencyOperation.CURRENCY_REQUEST)
                .setParameter("state", Transaction.State.OK).getResultList();
        Map<CurrencyCode, BigDecimal> currencyRequestMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            currencyRequestMap.put((CurrencyCode)listItem[1], (BigDecimal) listItem[0]);
        }
        resultMap.put("currencyRequestMap", currencyRequestMap);

        resultList = em.createQuery("select SUM(t.amount), t.currencyCode from Transaction t " +
                "where t.state =:state and t.type in :transactionTypeList group by t.currencyCode")
                .setParameter("transactionTypeList", Arrays.asList(CurrencyOperation.CURRENCY_CHANGE, CurrencyOperation.CURRENCY_SEND))
                .setParameter("state", Transaction.State.OK).getResultList();
        Map<CurrencyCode, BigDecimal> currencyExpendedMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            currencyExpendedMap.put((CurrencyCode)listItem[1], (BigDecimal) listItem[0]);
        }
        resultMap.put("currencyExpendedMap", currencyExpendedMap);

        return Response.ok().entity(new JSON().getMapper().writeValueAsBytes(resultMap)).build();
    }

    @GET @Path("/bank-trans")
    public Response sumBankTrans(@Context HttpServletRequest req, @Context HttpServletResponse resp)
            throws JsonProcessingException, ValidationException {
        List<Object[]> resultList = em.createQuery(
                "select SUM(t.amount), t.currencyCode, t.fromUser.name, COUNT(t) from Transaction t " +
                "where t.state =:state and t.type=:type group by t.currencyCode, t.fromUser.name")
                .setParameter("type", CurrencyOperation.TRANSACTION_FROM_BANK)
                .setParameter("state", Transaction.State.OK).getResultList();
        Map<String, Map<CurrencyCode, Map>> totalBalanceMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            String bankName = (String) listItem[2];
            CurrencyCode currencyCode = (CurrencyCode)listItem[1];
            BigDecimal amount = (BigDecimal) listItem[0];
            Long numTransactions = (Long) listItem[3];
            if(totalBalanceMap.containsKey(bankName)) {
                totalBalanceMap.get(bankName).put(currencyCode, ImmutableMap.of("numTransactions", numTransactions, "amount", amount)) ;
            } else {
                Map<CurrencyCode, Map> currencyMap = new HashMap<>();
                currencyMap.put(currencyCode, ImmutableMap.of("numTransactions", numTransactions, "amount", amount));
                totalBalanceMap.put(bankName, currencyMap);
            }
        }
        return Response.ok().entity(new JSON().getMapper().writeValueAsBytes(totalBalanceMap)).build();
    }

    @Transactional
    @Path("/cert-by-hash") @POST
    @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response uuid(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        String uuid = FileUtils.getStringFromStream(req.getInputStream());
        List<Bank> bankList = em.createQuery(
                "select b from Certificate c JOIN c.signer b where c.UUID=:UUID")
                .setParameter("UUID", uuid).getResultList();
        if (bankList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Cert with uuid: " + uuid +
                    " not found").build();
        }
        User bank = bankList.iterator().next();
        return Response.ok().entity("Signer UUID: " + bank.getUUID() + " - simple name: " +
                bank.getClass().getSimpleName()).build();
    }

    @Transactional
    @Path("/hibernate-criteria") @GET
    @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    public Response hibernateCriteria(@Context HttpServletRequest req, @Context HttpServletResponse res) throws Exception {
        List<CurrencyOperation> transactionTypeList = Arrays.asList(CurrencyOperation.TRANSACTION_FROM_BANK);
        Criteria criteria = em.unwrap(Session.class).createCriteria(Transaction.class);
        criteria.add(Restrictions.in("type", transactionTypeList));
        List<Transaction> transactionList = criteria.setFirstResult(0).setMaxResults(100).list();
        return Response.ok().entity("Num. transactions: "+ transactionList.size()).build();
    }

}