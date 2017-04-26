package org.currency.web.ejb;

import org.votingsystem.currency.BalanceUtils;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.Interval;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class BalancesEJB {

    private static Logger log = Logger.getLogger(BalancesEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private SignatureService signatureService;
    @Inject private BankEJB bankBean;
    @Inject private UserEJB userBean;
    @Inject private TransactionEJB transactionBean;
    @Inject private CurrencyAccountEJB currencyAccountBean;

    public BalancesDto getSystemBalancesDto(Interval timePeriod) throws Exception {
        log.info("timePeriod: " + timePeriod.toString());
        List<Transaction> transactionList = em.createNamedQuery(Transaction.FIND_TRANS_BY_TYPE_LIST)
                .setParameter("dateFrom", timePeriod.getDateFrom().toLocalDateTime())
                .setParameter("dateTo", timePeriod.getDateTo().toLocalDateTime())
                .setParameter("state", Transaction.State.OK)
                .setParameter("typeList", Arrays.asList(CurrencyOperation.CURRENCY_SEND)).getResultList();
        BalancesDto balancesDto = getBalancesDto(transactionList, Transaction.Source.FROM).setTimePeriod(timePeriod);
        transactionList = em.createNamedQuery(Transaction.FIND_TRANS_BY_TYPE_LIST)
                .setParameter("dateFrom", timePeriod.getDateFrom())
                .setParameter("dateTo", timePeriod.getDateTo())
                .setParameter("state", Transaction.State.OK)
                .setParameter("typeList", Arrays.asList(CurrencyOperation.CURRENCY_REQUEST,
                        CurrencyOperation.TRANSACTION_FROM_BANK)).getResultList();
        BalancesDto balancesToDto = getBalancesDto(transactionList, Transaction.Source.TO);
        balancesDto.setTo(balancesToDto);
        return balancesDto;
    }

    public BalancesDto getBankBalancesDto(Bank bank, Interval timePeriod) throws Exception {
        BalancesDto balancesDto = getBalancesDto(
                transactionBean.getTransactionFromList(bank, timePeriod), Transaction.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setTransactionToList(Collections.emptyList());
        balancesDto.setUser(userBean.getUserDto(bank, false));
        return balancesDto;
    }

    public BalancesDto getUserBalancesDto(User user, Interval timePeriod, boolean checkWithAccounts) throws Exception {
        BalancesDto balancesDto = getBalancesDto(
                transactionBean.getTransactionFromList(user, timePeriod), Transaction.Source.FROM);
        balancesDto.setTimePeriod(timePeriod);
        balancesDto.setUser(userBean.getUserDto(user, false));

        BalancesDto balancesToDto = getBalancesDto(
                transactionBean.getTransactionToList(user, timePeriod), Transaction.Source.TO);
        balancesDto.setTo(balancesToDto);

        balancesDto.calculateCash();
        if(checkWithAccounts && User.Type.CURRENCY_SERVER != user.getType() && timePeriod.isCurrentWeekPeriod())
            currencyAccountBean.checkBalancesWithCurrencyAccounts(user, balancesDto.getBalancesCash());
        return balancesDto;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void updateSystemBalance(BigDecimal amount, CurrencyCode currencyCode) throws ValidationException {
        List<CurrencyAccount> accounts = em.createNamedQuery(
                CurrencyAccount.FIND_BY_USER_IBAN_AND_CURRENCY_CODE_AND_STATE)
                .setParameter("userIBAN", config.getSystemUser().getIBAN())
                .setParameter("currencyCode", currencyCode)
                .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        if(!accounts.isEmpty()) {
            CurrencyAccount currencyAccount = accounts.iterator().next();
            currencyAccount.setBalance(currencyAccount.getBalance().add(amount));
        } else {
            CurrencyAccount currencyAccount = new CurrencyAccount(config.getSystemUser(), amount, currencyCode);
            em.persist(currencyAccount);
        }
    }

    public BalancesDto getBalancesDto(List<Transaction> transactionList, Transaction.Source source) throws ValidationException {
        List<TransactionDto> transactionFromList = new ArrayList<>();
        for(Transaction transaction : transactionList) {
            transactionFromList.add(transactionBean.getTransactionDto(transaction));
        }
        switch (source) {
            case FROM:
                return BalancesDto.FROM(transactionFromList, BalanceUtils.getBalances(transactionList));
            case TO:
                return BalancesDto.TO(transactionFromList, BalanceUtils.getBalances(transactionList));
        }
        throw new ValidationException("unknown source: " + source);
    }

    public BalancesDto getBalancesDto(User user, Interval timePeriod) throws Exception {
        if(user instanceof Bank) return getBankBalancesDto((Bank) user, timePeriod);
        else return getUserBalancesDto(user, timePeriod, true);
    }

}