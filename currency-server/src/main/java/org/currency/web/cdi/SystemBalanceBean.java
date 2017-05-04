package org.currency.web.cdi;

import com.google.common.collect.ImmutableMap;
import org.currency.web.ejb.ConfigCurrencyServer;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named("systemBalanceBean")
@Stateless
public class SystemBalanceBean implements Serializable {

    @PersistenceContext
    private EntityManager em;
    @Inject
    private ConfigCurrencyServer config;

    public Map<CurrencyCode, BigDecimal> getSystemAccountsMap() {
        List<Object[]> resultList = em.createQuery("select SUM(c.balance), c.currencyCode from CurrencyAccount c " +
                "where c.state =:state and c.user=:user group by c.currencyCode")
                .setParameter("user", config.getSystemUser())
                .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        Map<CurrencyCode, BigDecimal> totalBalanceMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            BigDecimal amount = (BigDecimal) listItem[0];
            CurrencyCode currencyCode = (CurrencyCode)listItem[1];
            totalBalanceMap.put(currencyCode, amount);
        }
        return totalBalanceMap;
    }

    public Map<CurrencyCode, Map> getTransactionsFromBankMap() {
        List<Object[]> resultList = em.createQuery(
                "select SUM(t.amount), t.currencyCode, COUNT(t) from Transaction t " +
                        "where t.state =:state and t.type=:type group by t.currencyCode")
                .setParameter("type", CurrencyOperation.TRANSACTION_FROM_BANK)
                .setParameter("state", Transaction.State.OK).getResultList();
        Map<CurrencyCode, Map> resultMap = new HashMap<>();
        for(Object[] listItem : resultList) {
            BigDecimal amount = (BigDecimal) listItem[0];
            CurrencyCode currencyCode = (CurrencyCode)listItem[1];
            Long numTransactions = (Long) listItem[2];
            resultMap.put(currencyCode, ImmutableMap.of("numTransactions", numTransactions, "amount", amount));
        }
        return resultMap;
    }

}
