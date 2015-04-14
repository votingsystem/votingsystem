package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.ejb.DAOBean;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
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

    @Inject DAOBean dao;
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

    public BalancesDto genBalanceForSystem(TimePeriod timePeriod) throws Exception {
        log.info("timePeriod: " + timePeriod.toString());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("timePeriod", timePeriod);
        resultMap.put("userVS", userVSBean.getUserVSDto(systemUser, false));
        Query query = dao.getEM().createNamedQuery("findSystemTransactionVSList").setParameter("state", TransactionVS.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("typeList", Arrays.asList(TransactionVS.Type.CURRENCY_SEND));
        List<TransactionVS> transactionList = query.getResultList();
        BalancesDto balancesDto = transactionVSBean.getBalancesDto(
                transactionList, TransactionVS.Source.FROM);
        query = dao.getEM().createNamedQuery("findSystemTransactionVSFromList").setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("typeList", Arrays.asList(TransactionVS.Type.CURRENCY_SEND));
        transactionList = query.getResultList();
        BalancesDto balancesToDto = transactionVSBean.getBalancesDto(transactionList, TransactionVS.Source.FROM);
        balancesDto.setTo(balancesToDto);
        return balancesDto;
    }


    public void updateTagBalance(BigDecimal amount, String currencyCode, TagVS tag) throws Exception {
        Query query = dao.getEM().createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                .setParameter("state", CurrencyAccount.State.ACTIVE).setParameter("userIBAN", systemUser.getIBAN())
                .setParameter("tag", tag).setParameter("currencyCode", currencyCode);
        CurrencyAccount tagAccount = dao.getSingleResult(CurrencyAccount.class, query);
        if(tagAccount == null) throw new Exception("THERE'S NOT ACTIVE SYSTEM ACCOUNT FOR TAG " + tag.getName() +
                " and currency " + currencyCode);
        dao.getEM().merge(tagAccount.setBalance(tagAccount.getBalance().add(amount)));
    }

    public BalancesDto genBalance(UserVS uservs, TimePeriod timePeriod) throws Exception {
        if(UserVS.Type.SYSTEM == uservs.getType()) return genBalanceForSystem(timePeriod);
        else if(uservs instanceof BankVS) return bankVSBean.getBalancesDto((BankVS) uservs, timePeriod);
        else if(uservs instanceof GroupVS) return groupVSBean.getBalancesDto(uservs, timePeriod);
        else return userVSBean.getBalancesDto(uservs, timePeriod);
    }

}
