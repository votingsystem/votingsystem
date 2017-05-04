package org.currency.web.cdi;

import com.google.common.collect.ImmutableMap;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Named("currencyBean")
@Stateless
public class CurrencyBean implements Serializable {

    private static final Logger log = Logger.getLogger(CurrencyBean.class.getName());

    @PersistenceContext
    private EntityManager em;

    public Map<CurrencyCode, Map> getCurrencyRequestMap() {
        List<Object[]> resultList = em.createQuery("select SUM(t.amount), t.currencyCode, COUNT(t) from Transaction t " +
                "where t.state =:state and t.type=:transactionType group by t.currencyCode")
                .setParameter("transactionType", CurrencyOperation.CURRENCY_REQUEST)
                .setParameter("state", Transaction.State.OK).getResultList();
        Map<CurrencyCode, Map> currencyRequestMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            CurrencyCode currencyCode = (CurrencyCode)listItem[1];
            BigDecimal amount = (BigDecimal) listItem[0];
            Long numTransactions = (Long) listItem[2];
            currencyRequestMap.put(currencyCode, ImmutableMap.of("numTransactions", numTransactions, "amount", amount));
        }
        return currencyRequestMap;
    }

    public Map<CurrencyCode, Map> getCurrencyExpendedMap() {
        List<Object[]> resultList = em.createQuery("select SUM(t.amount), t.currencyCode, COUNT(t) from Transaction t " +
                "where t.state =:state and t.type in :transactionTypeList group by t.currencyCode")
                .setParameter("transactionTypeList", Arrays.asList(CurrencyOperation.CURRENCY_CHANGE, CurrencyOperation.CURRENCY_SEND))
                .setParameter("state", Transaction.State.OK).getResultList();
        Map<CurrencyCode, Map> currencyRequestMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            CurrencyCode currencyCode = (CurrencyCode)listItem[1];
            BigDecimal amount = (BigDecimal) listItem[0];
            Long numTransactions = (Long) listItem[2];
            currencyRequestMap.put(currencyCode, ImmutableMap.of("numTransactions", numTransactions, "amount", amount));
        }
        return currencyRequestMap;
    }

    public Map<CurrencyCode, BigDecimal> getCurrencyActiveMap() {
        List<Object[]> resultList = em.createQuery("select SUM(c.amount), c.currencyCode from Currency c " +
                "where c.state =:state group by c.currencyCode")
                .setParameter("state", Currency.State.OK).getResultList();
        Map<CurrencyCode, BigDecimal> currencyRequestMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            BigDecimal amount = (BigDecimal) listItem[0];
            CurrencyCode currencyCode = (CurrencyCode)listItem[1];
            currencyRequestMap.put(currencyCode, amount);
        }
        return currencyRequestMap;
    }
}