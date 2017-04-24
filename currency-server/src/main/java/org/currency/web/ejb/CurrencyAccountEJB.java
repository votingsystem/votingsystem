package org.currency.web.ejb;

import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

@Stateless
public class CurrencyAccountEJB {

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;

    @TransactionAttribute(REQUIRES_NEW)
    public CurrencyAccount checkUserAccountForCurrency(User user, CurrencyCode currencyCode) throws ValidationException {
        CurrencyAccount result;
        List<CurrencyAccount> currencyAccounts =
                em.createNamedQuery(CurrencyAccount.FIND_BY_USER_IBAN_AND_CURRENCY_CODE_AND_STATE)
                .setParameter("userIBAN", user.getIBAN())
                .setParameter("currencyCode", currencyCode)
                .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        if(currencyAccounts.isEmpty()) {
            result = new CurrencyAccount(user, BigDecimal.ZERO, currencyCode);
            em.persist(result);
        } else result = currencyAccounts.iterator().next();
        return result;
    }

    public Map<CurrencyCode, BigDecimal> getAccountsBalanceMap(User user) {
        List<CurrencyAccount> currencyAccounts= em.createNamedQuery(CurrencyAccount.FIND_BY_TYPE_AND_USER).setParameter(
                "type", CurrencyAccount.Type.SYSTEM).setParameter("user", user).getResultList();
        Map<CurrencyCode, BigDecimal> result = new HashMap<>();
        for(CurrencyAccount account: currencyAccounts) {
            result.put(account.getCurrencyCode(), account.getBalance());
        }
        return result;
    }

    //Method that checks that the calculated balance from transactions corresponds with the accounts state
    public void checkBalancesWithCurrencyAccounts(User user, Map<CurrencyCode, BigDecimal> balancesMap) throws ValidationException {
        Map<CurrencyCode, BigDecimal> accountsMap = getAccountsBalanceMap(user);
        if(accountsMap.keySet().size() > 1) throw new ValidationException("User: " + user.getId() + "has " +
                accountsMap.keySet().size() + " accounts");
        if(accountsMap.keySet().isEmpty()) return;
        if(accountsMap.keySet().size() != balancesMap.keySet().size())
            throw new ValidationException("User " + user.getUUID() + " with " + accountsMap.keySet().size() + " system accounts and " +
                    balancesMap.keySet().size() + " accounts in transactions");
        for(CurrencyCode currencyCode : accountsMap.keySet()) {
            if(accountsMap.get(currencyCode).compareTo(balancesMap.get(currencyCode)) != 0)
                throw new ValidationException(MessageFormat.format(
                        "User {0} has {1} {2} on accounts and {3} {2} in transaction balance",  user.getUUID(),
                        accountsMap.get(currencyCode), currencyCode, balancesMap.get(currencyCode)));
        }
    }
}