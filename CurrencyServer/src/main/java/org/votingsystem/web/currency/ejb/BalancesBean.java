package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Group;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.Interval;
import org.votingsystem.util.currency.BalanceUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

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

    private static Logger log = Logger.getLogger(BalancesBean.class.getName());

    @Inject ConfigVS config;
    @Inject GroupBean groupBean;
    @Inject CMSBean cmsBean;
    @Inject BankBean bankBean;
    @Inject UserBean userBean;
    @Inject DAOBean dao;
    @Inject TransactionBean transactionBean;
    @Inject CurrencyAccountBean currencyAccountBean;

    public BalancesDto getSystemBalancesDto(Interval timePeriod) throws Exception {
        log.info("timePeriod: " + timePeriod.toString());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("timePeriod", timePeriod);
        resultMap.put("user", userBean.getUserDto(config.getSystemUser(), false));
        Query query = dao.getEM().createNamedQuery("findSystemTransactionList").setParameter("state", Transaction.State.OK)
                .setParameter("dateFrom", timePeriod.getDateFrom()).setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("typeList", Arrays.asList(Transaction.Type.CURRENCY_SEND));
        List<Transaction> transactionList = query.getResultList();
        BalancesDto balancesDto = getBalancesDto(
                transactionList, Transaction.Source.FROM);
        query = dao.getEM().createNamedQuery("findSystemTransactionFromList").setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("typeList", Arrays.asList(Transaction.Type.CURRENCY_SEND));
        transactionList = query.getResultList();
        BalancesDto balancesToDto = getBalancesDto(transactionList, Transaction.Source.FROM);
        balancesDto.setTo(balancesToDto);
        return balancesDto;
    }

    public BalancesDto getBankBalancesDto(Bank bank, Interval timePeriod) throws Exception {
        BalancesDto balancesDto = getBalancesDto(
                transactionBean.getTransactionFromList(bank, timePeriod), Transaction.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setUser(userBean.getUserDto(bank, false));
        return balancesDto;
    }

    public BalancesDto getGroupBalancesDto(Group group, Interval timePeriod) throws Exception {
        BalancesDto balancesDto = getBalancesDto(
                transactionBean.getTransactionFromList(group, timePeriod), Transaction.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setUser(UserDto.BASIC(group));

        BalancesDto balancesToDto = getBalancesDto(
                transactionBean.getTransactionToList(group, timePeriod), Transaction.Source.TO);
        balancesDto.setTo(balancesToDto);
        balancesDto.calculateCash();
        currencyAccountBean.checkBalancesWithCurrencyAccounts(group, balancesDto.getBalancesCash());
        return balancesDto;
    }

    public BalancesDto getUserBalancesDto(User user, Interval timePeriod) throws Exception {
        BalancesDto balancesDto = getBalancesDto(
                transactionBean.getTransactionFromList(user, timePeriod), Transaction.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setUser(userBean.getUserDto(user, false));

        BalancesDto balancesToDto = getBalancesDto(
                transactionBean.getTransactionToList(user, timePeriod), Transaction.Source.TO);
        balancesDto.setTo(balancesToDto);

        balancesDto.calculateCash();
        if(User.Type.SYSTEM != user.getType() && timePeriod.isCurrentWeekPeriod())
            currencyAccountBean.checkBalancesWithCurrencyAccounts(user, balancesDto.getBalancesCash());
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

    public BalancesDto getBalancesDto(List<Transaction> transactionList, Transaction.Source source) throws ExceptionVS {
        List<TransactionDto> transactionFromList = new ArrayList<>();
        for(Transaction transaction : transactionList) {
            transactionFromList.add(transactionBean.getTransactionDto(transaction));
        }
        switch (source) {
            case FROM:
                return BalancesDto.FROM(transactionFromList, BalanceUtils.getBalancesFrom(transactionList));
            case TO:
                return BalancesDto.TO(transactionFromList, BalanceUtils.getBalancesTo(transactionList));
        }
        throw new ExceptionVS("unknown source: " + source);
    }

    public BalancesDto getBalancesDto(User user, Interval timePeriod) throws Exception {
        if(user instanceof Bank) return getBankBalancesDto((Bank) user, timePeriod);
        else if(user instanceof Group) return getGroupBalancesDto((Group) user, timePeriod);
        else return getUserBalancesDto(user, timePeriod);
    }

}