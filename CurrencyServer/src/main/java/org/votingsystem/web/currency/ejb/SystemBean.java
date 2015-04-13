package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.web.ejb.DAOBean;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Stateless
public class SystemBean {

    private static Logger log = Logger.getLogger(SystemBean.class.getSimpleName());

    @PersistenceContext EntityManager em;
    @Inject
    DAOBean dao;
    @Inject UserVSBean userVSBean;
    @Inject GroupVSBean groupVSBean;
    @Inject BankVSBean bankVSBean;
    @Inject TransactionVSBean transactionVSBean;
    private UserVS systemUser;

    @PostConstruct
    public void initialize() throws Exception {
        Query query = dao.getEM().createNamedQuery("findUserByType").setParameter("type", UserVS.Type.SYSTEM);
        systemUser = dao.getSingleResult(UserVS.class, query);
    }

    public Map genBalanceForSystem(DateUtils.TimePeriod timePeriod) throws Exception {
        log.info("timePeriod: " + timePeriod.toString());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("timePeriod", timePeriod.getMap());
        resultMap.put("userVS", timePeriod.getMap());
        resultMap.put("userVS", userVSBean.getUserVSDataMap(systemUser, false));
        Query query = em.createNamedQuery("findSystemTransactionVSList").setParameter("state", TransactionVS.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("typeList", Arrays.asList(TransactionVS.Type.CURRENCY_SEND));
        List<TransactionVS> transactionList = query.getResultList();
        Map transactionListWithBalances = transactionVSBean.getTransactionListWithBalances(
                transactionList, TransactionVS.Source.FROM);
        resultMap.put("transactionFromList", transactionListWithBalances.get("transactionList"));
        resultMap.put("balancesFrom", transactionListWithBalances.get("balances"));

        query = em.createNamedQuery("findSystemTransactionVSFromList").setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("typeList", Arrays.asList(TransactionVS.Type.CURRENCY_SEND));
        transactionList = query.getResultList();
        transactionListWithBalances = transactionVSBean.getTransactionListWithBalances(
                transactionList, TransactionVS.Source.FROM);
        resultMap.put("transactionToList", transactionListWithBalances.get("transactionList"));
        resultMap.put("balancesTo", transactionListWithBalances.get("balances"));
        return resultMap;
    }


    public void updateTagBalance(BigDecimal amount, String currencyCode, TagVS tag) throws Exception {
        Query query = em.createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                .setParameter("state", CurrencyAccount.State.ACTIVE).setParameter("userIBAN", systemUser.getIBAN())
                .setParameter("tag", tag).setParameter("currencyCode", currencyCode);
        CurrencyAccount tagAccount = dao.getSingleResult(CurrencyAccount.class, query);
        if(tagAccount == null) throw new Exception("THERE'S NOT ACTIVE SYSTEM ACCOUNT FOR TAG " + tag.getName() +
                " and currency " + currencyCode);
        em.merge(tagAccount.setBalance(tagAccount.getBalance().add(amount)));
    }

    public Map genBalance(UserVS uservs, DateUtils.TimePeriod timePeriod) throws Exception {
        if(UserVS.Type.SYSTEM == uservs.getType()) return genBalanceForSystem(timePeriod);
        else if(uservs instanceof BankVS) return bankVSBean.getDataWithBalancesMap((BankVS) uservs, timePeriod);
        else if(uservs instanceof GroupVS) return groupVSBean.getDataWithBalancesMap(uservs, timePeriod);
        else return userVSBean.getDataWithBalancesMap(uservs, timePeriod);
    }

}
