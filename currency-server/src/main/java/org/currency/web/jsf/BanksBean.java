package org.currency.web.jsf;

import com.google.common.collect.ImmutableMap;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named("banksBean")
@Stateless
public class BanksBean implements Serializable {

    @PersistenceContext
    private EntityManager em;


    public List<Bank> getBankList() {
        List<Bank> bankList = em.createQuery("select b from Bank b").getResultList();
        return bankList;
    }

    public Map<String, Map<CurrencyCode, Map>> getSummaryMap() {
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
        return totalBalanceMap;
    }

}
