package org.votingsystem.currency.web.ejb;

import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Tag;
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

import static java.text.MessageFormat.format;

@Stateless
public class CurrencyAccountEJB {

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;

    public CurrencyAccount checkWildtagUserAccountForCurrency(User user, CurrencyCode currencyCode) throws ValidationException {
        CurrencyAccount result;
        List<CurrencyAccount> currencyAccounts =
                em.createNamedQuery(CurrencyAccount.FIND_BY_USER_IBAN_AND_TAG_AND_CURRENCY_CODE_AND_STATE)
                .setParameter("userIBAN", user.getIBAN())
                .setParameter("tag", config.getTag(Tag.WILDTAG))
                .setParameter("currencyCode", currencyCode)
                .setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        if(currencyAccounts.isEmpty()) {
            result = new CurrencyAccount(user, BigDecimal.ZERO, currencyCode, config.getTag(Tag.WILDTAG));
            em.persist(result);
        } else result = currencyAccounts.iterator().next();
        return result;
    }

    public Map<String, Map<CurrencyCode, Map<String, BigDecimal>>> getAccountsBalanceMap(User user) {
        List<CurrencyAccount> currencyAccounts= em.createNamedQuery(CurrencyAccount.FIND_BY_TYPE_AND_USER).setParameter(
                "type", CurrencyAccount.Type.SYSTEM).setParameter("user", user).getResultList();
        Map<String, Map<CurrencyCode, Map<String, BigDecimal>>> result = new HashMap<>();
        for(CurrencyAccount account: currencyAccounts) {
            if(result.containsKey(account.getIBAN())) {
                if(result.get(account.getIBAN()).containsKey(account.getCurrencyCode())) {
                    result.get(account.getIBAN()).get(account.getCurrencyCode()).put(account.getTag().getName(),
                            account.getBalance());
                } else {
                    Map<String, BigDecimal> tagDataMap = new HashMap<>();
                    tagDataMap.put(account.getTag().getName(), account.getBalance());
                    result.get(account.getIBAN()).put(account.getCurrencyCode(), tagDataMap);
                }
            } else {
                Map<CurrencyCode, Map<String, BigDecimal>> currencyDataMap = new HashMap<>();
                Map<String, BigDecimal> tagDataMap = new HashMap<>();
                tagDataMap.put(account.getTag().getName(), account.getBalance());
                currencyDataMap.put(account.getCurrencyCode(), tagDataMap);
                result.put(account.getIBAN(), currencyDataMap);
            }
        }
        return result;
    }

    //Method that checks that the calculated balance from transactions corresponds with the accounts state
    public void checkBalancesWithCurrencyAccounts(User user, Map<CurrencyCode, Map<String, BigDecimal>> balancesMap) throws ValidationException {
        Map<String, Map<CurrencyCode, Map<String, BigDecimal>>> accountsMap = getAccountsBalanceMap(user);
        if(accountsMap.keySet().size() > 1) throw new ValidationException("User: " + user.getId() + "has " +
                accountsMap.keySet().size() + " accounts");
        if(accountsMap.keySet().isEmpty()) return;
        Map<CurrencyCode, Map<String, BigDecimal>> mainAccountMap = accountsMap.values().iterator().next();
        for(CurrencyCode currencyCode : mainAccountMap.keySet()) {
            BigDecimal wildTagExpendedInTags = BigDecimal.ZERO;
            BigDecimal wildTagBalance = BigDecimal.ZERO;
            if(balancesMap.containsKey(currencyCode)) {
                for(String tag: mainAccountMap.get(currencyCode).keySet()) {
                    BigDecimal tagAccountAmount = mainAccountMap.get(currencyCode).get(tag);
                    BigDecimal tagBalanceAmount = null;
                    if((tagBalanceAmount = balancesMap.get(currencyCode).get(tag)) != null) {
                        if(Tag.WILDTAG.equals(tag)) wildTagBalance = tagBalanceAmount;
                        else if(tagAccountAmount.compareTo(tagBalanceAmount) != 0)
                            wildTagExpendedInTags = wildTagExpendedInTags.add(tagBalanceAmount.subtract(tagAccountAmount));
                    } else {
                        if(tagAccountAmount.compareTo(BigDecimal.ZERO) != 0) throw new ValidationException(format(
                                "Error with user {0} - tag: {1} - tagAccountAmount: {2} {3} - transaction balance: 0 {3}",
                                user.getId(), tag, tagAccountAmount, currencyCode));
                    }
                }
            } else {//check if there's a tag with negative balance
                for(String tag: mainAccountMap.get(currencyCode).keySet()) {
                    BigDecimal tagAccountAmount = mainAccountMap.get(currencyCode).get(tag);
                    if(tagAccountAmount.compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("Error with user: " +
                            user.getId() + " - tag " + tag + " - tagAccountAmount: " + tagAccountAmount +
                            " - currency: " + currencyCode + " - accounts: " + accountsMap + " - balance:" + balancesMap);
                }
            }
            //check WILDTAG balance result match with stored in account
            BigDecimal wildTagAccountAmount = mainAccountMap.get(currencyCode).get(Tag.WILDTAG);
            if(wildTagAccountAmount.compareTo(wildTagBalance.subtract(wildTagExpendedInTags)) != 0) throw new ValidationException(
                    format("ERROR - wildTag mismatch -  user: ''{0}'' - currency: ''{1}'' - wildTagAccountAmount: ''{2}'' " +
                            "- wildTagExpendedInTags: ''{3}'' - wildTagBalance: ''{4}'' - accounts: ''{5}''",
                            user.getId(), currencyCode, wildTagAccountAmount, wildTagExpendedInTags, wildTagBalance, accountsMap));

        }
    }
}