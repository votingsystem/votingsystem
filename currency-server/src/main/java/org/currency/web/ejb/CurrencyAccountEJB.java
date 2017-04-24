package org.currency.web.ejb;

import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class CurrencyAccountEJB {

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;

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