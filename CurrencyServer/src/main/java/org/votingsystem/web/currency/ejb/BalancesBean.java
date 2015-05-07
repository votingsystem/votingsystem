package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.BankVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.util.currency.BalanceUtils;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class BalancesBean {

    private static Logger log = Logger.getLogger(BalancesBean.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject GroupVSBean groupVSBean;
    @Inject SignatureBean signatureBean;
    @Inject BankVSBean bankVSBean;
    @Inject UserVSBean userVSBean;
    @Inject DAOBean dao;
    private MessagesVS messages = MessagesVS.getCurrentInstance();
    @Inject TransactionVSBean transactionVSBean;
    @Inject CurrencyAccountBean currencyAccountBean;

    public BalancesDto getSystemBalancesDto(TimePeriod timePeriod) throws Exception {
        log.info("timePeriod: " + timePeriod.toString());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("timePeriod", timePeriod);
        resultMap.put("userVS", userVSBean.getUserVSDto(config.getSystemUser(), false));
        Query query = dao.getEM().createNamedQuery("findSystemTransactionVSList").setParameter("state", TransactionVS.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("typeList", Arrays.asList(TransactionVS.Type.CURRENCY_SEND));
        List<TransactionVS> transactionList = query.getResultList();
        BalancesDto balancesDto = getBalancesDto(
                transactionList, TransactionVS.Source.FROM);
        query = dao.getEM().createNamedQuery("findSystemTransactionVSFromList").setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("typeList", Arrays.asList(TransactionVS.Type.CURRENCY_SEND));
        transactionList = query.getResultList();
        BalancesDto balancesToDto = getBalancesDto(transactionList, TransactionVS.Source.FROM);
        balancesDto.setTo(balancesToDto);
        return balancesDto;
    }

    public BalancesDto getBankVSBalancesDto(BankVS bankVS, TimePeriod timePeriod) throws Exception {
        BalancesDto balancesDto = getBalancesDto(
                transactionVSBean.getTransactionFromList(bankVS, timePeriod), TransactionVS.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setUserVS(userVSBean.getUserVSDto(bankVS, false));
        return balancesDto;
    }

    public BalancesDto getGroupVSBalancesDto(GroupVS groupVS, TimePeriod timePeriod) throws Exception {
        BalancesDto balancesDto = getBalancesDto(
                transactionVSBean.getTransactionFromList(groupVS, timePeriod), TransactionVS.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setUserVS(UserVSDto.BASIC(groupVS));

        BalancesDto balancesToDto = getBalancesDto(
                transactionVSBean.getTransactionToList(groupVS, timePeriod), TransactionVS.Source.TO);
        balancesDto.setTo(balancesToDto);
        balancesDto.calculateCash();
        currencyAccountBean.checkBalancesWithCurrencyAccounts(groupVS, balancesDto.getBalancesCash());
        return balancesDto;
    }

    public BalancesDto getUserVSBalancesDto(UserVS userVS, TimePeriod timePeriod) throws Exception {
        BalancesDto balancesDto = getBalancesDto(
                transactionVSBean.getTransactionFromList(userVS, timePeriod), TransactionVS.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setUserVS(userVSBean.getUserVSDto(userVS, false));

        BalancesDto balancesToDto = getBalancesDto(
                transactionVSBean.getTransactionToList(userVS, timePeriod), TransactionVS.Source.TO);
        balancesDto.setTo(balancesToDto);

        balancesDto.calculateCash();
        if(UserVS.Type.SYSTEM != userVS.getType() && timePeriod.isCurrentWeekPeriod())
            currencyAccountBean.checkBalancesWithCurrencyAccounts(userVS, balancesDto.getBalancesCash());
        return balancesDto;
    }

    public void updateTagBalance(BigDecimal amount, String currencyCode, TagVS tag) throws Exception {
        Query query = dao.getEM().createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                .setParameter("state", CurrencyAccount.State.ACTIVE).setParameter("userIBAN", config.getSystemUser().getIBAN())
                .setParameter("tag", tag).setParameter("currencyCode", currencyCode);
        CurrencyAccount tagAccount = dao.getSingleResult(CurrencyAccount.class, query);
        if(tagAccount == null) throw new Exception("THERE'S NOT ACTIVE SYSTEM ACCOUNT FOR TAG " + tag.getName() +
                " and currency " + currencyCode);
        dao.getEM().merge(tagAccount.setBalance(tagAccount.getBalance().add(amount)));
    }

    public BalancesDto getBalancesDto(List<TransactionVS> transactionList, TransactionVS.Source source) throws ExceptionVS {
        List<TransactionVSDto> transactionFromList = new ArrayList<>();
        for(TransactionVS transaction : transactionList) {
            transactionFromList.add(transactionVSBean.getTransactionDto(transaction));
        }
        switch (source) {
            case FROM:
                return BalancesDto.FROM(transactionFromList, BalanceUtils.getBalancesFrom(transactionList));
            case TO:
                return BalancesDto.TO(transactionFromList, BalanceUtils.getBalancesTo(transactionList));
        }
        throw new ExceptionVS("unknown source: " + source);
    }

    public BalancesDto getBalancesDto(UserVS userVS, TimePeriod timePeriod) throws Exception {
        if(userVS instanceof BankVS) return getBankVSBalancesDto((BankVS) userVS, timePeriod);
        else if(userVS instanceof GroupVS) return getGroupVSBalancesDto((GroupVS) userVS, timePeriod);
        else return getUserVSBalancesDto(userVS, timePeriod);
    }

}