package org.votingsystem.web.currency.ejb;

import com.google.common.eventbus.Subscribe;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.service.EventBusService;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.web.ejb.DAOBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


@Singleton
@Startup
public class SystemBean implements Serializable{

    private static Logger log = Logger.getLogger(SystemBean.class.getName());

    @Inject DAOBean dao;
    @Inject IBANBean ibanBean;
    @Inject UserVSBean userVSBean;
    @Inject GroupVSBean groupVSBean;
    @Inject BankVSBean bankVSBean;
    @Inject TransactionVSBean transactionVSBean;
    private UserVS systemUser;

    @PostConstruct public void initialize() {
        log.info(" --- initialize --- ");
        Query query = dao.getEM().createNamedQuery("findUserByType").setParameter("type", UserVS.Type.SYSTEM);
        systemUser = dao.getSingleResult(UserVS.class, query);
        EventBusService.getInstance().register(this);
    }

    @PreDestroy public void destroy() {
        log.info(" --- destroy --- ");
        EventBusService.getInstance().unRegister(this);
    }

    @Subscribe public void newUserVS(final UserVS userVS) {
        log.info("newUserVS: " + userVS.getId());
        userVS.setIBAN(ibanBean.getIBAN(userVS.getId()));
        dao.merge(userVS);
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
