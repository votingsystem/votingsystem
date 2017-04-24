package org.currency.web.ejb;

import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class WalletEJB {

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;

    public Map<CurrencyAccount, BigDecimal> getAccountMovementsForTransaction(String fromUserIBAN,
                      BigDecimal amount, CurrencyCode currencyCode) throws Exception {
        if(amount.compareTo(BigDecimal.ZERO) < 0) throw new ValidationException(
                "negativeAmountRequestedErrorMsg: " +  amount.toString());
        List<CurrencyAccount> currencyAccounts = em.createNamedQuery(
                CurrencyAccount.FIND_BY_USER_IBAN_AND_CURRENCY_CODE_AND_STATE)
                .setParameter("userIBAN", fromUserIBAN).setParameter("currencyCode", currencyCode)
                .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        if(currencyAccounts.isEmpty())
            throw new ValidationException("Account not found for IBAN:" + fromUserIBAN + " - " + currencyCode);
        CurrencyAccount currencyAccount = currencyAccounts.iterator().next();
        Map<CurrencyAccount, BigDecimal> result = new HashMap<>();
        if(currencyAccount.getBalance().compareTo(amount) < 0)
            throw new ValidationException("LOW BALANCE - request: " + amount + " " + currencyCode + " - available: " +
                currencyAccount.getBalance() + " " + currencyAccount.getCurrencyCode());
        result.put(currencyAccount, amount);
        return result;
    }

}