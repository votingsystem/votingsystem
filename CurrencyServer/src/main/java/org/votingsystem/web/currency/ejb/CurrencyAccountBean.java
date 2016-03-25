package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.CurrencyCode;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;


@Stateless
public class CurrencyAccountBean {

    @Inject DAOBean dao;
    @Inject ConfigVS config;

    public CurrencyAccount checkWildtagUserAccountForCurrency(User user, CurrencyCode currencyCode) throws Exception {
        Query query = dao.getEM().createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                .setParameter("userIBAN", user.getIBAN())
                .setParameter("tag", config.getTag(TagVS.WILDTAG))
                .setParameter("currencyCode", currencyCode)
                .setParameter("state", CurrencyAccount.State.ACTIVE);
        CurrencyAccount wildTagAccount = dao.getSingleResult(CurrencyAccount.class, query);
        if(wildTagAccount == null) {
            wildTagAccount = dao.persist(new CurrencyAccount(user, BigDecimal.ZERO, currencyCode,
                    config.getTag(TagVS.WILDTAG)));
        }
        return wildTagAccount;
    }

    public Map<String, Map<CurrencyCode, Map<String, BigDecimal>>> getAccountsBalanceMap(User user) {
        Query query = dao.getEM().createNamedQuery("findAccountByTypeAndUser").setParameter(
                "type", CurrencyAccount.Type.SYSTEM).setParameter("user", user);
        List<CurrencyAccount> currencyAccounts = query.getResultList();
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
    public void checkBalancesWithCurrencyAccounts(User user, Map<CurrencyCode, Map<String, BigDecimal>> balancesMap) throws ExceptionVS {
        Map<String, Map<CurrencyCode, Map<String, BigDecimal>>> accountsMap = getAccountsBalanceMap(user);
        if(accountsMap.keySet().size() > 1) throw new ExceptionVS("User: " + user.getId() + "has " +
                accountsMap.keySet().size() + " accounts");
        if(accountsMap.keySet().isEmpty()) return;
        //TODO - bucle when User could have multiple accounts
        Map<CurrencyCode, Map<String, BigDecimal>> mainAccountMap = accountsMap.values().iterator().next();
        for(CurrencyCode currencyCode : mainAccountMap.keySet()) {
            BigDecimal wildTagExpendedInTags = BigDecimal.ZERO;
            BigDecimal wildTagBalance = BigDecimal.ZERO;
            if(balancesMap.containsKey(currencyCode)) {
                for(String tag: mainAccountMap.get(currencyCode).keySet()) {
                    BigDecimal tagAccountAmount = mainAccountMap.get(currencyCode).get(tag);
                    BigDecimal tagBalanceAmount = null;
                    if((tagBalanceAmount = balancesMap.get(currencyCode).get(tag)) != null) {
                        if(TagVS.WILDTAG.equals(tag)) wildTagBalance = tagBalanceAmount;
                        else if(tagAccountAmount.compareTo(tagBalanceAmount) != 0)
                            wildTagExpendedInTags = wildTagExpendedInTags.add(tagBalanceAmount.subtract(tagAccountAmount));
                    } else {
                        if(tagAccountAmount.compareTo(BigDecimal.ZERO) != 0) throw new ExceptionVS(
                                "tagAccountAmount Error with user " + user.getId() + " - " + tag + " - " +
                                        currencyCode + " - tagAccountAmount: " + tagAccountAmount);
                    }
                }
            } else {//check if there's a tag with negative balance
                for(String tag: mainAccountMap.get(currencyCode).keySet()) {
                    BigDecimal tagAccountAmount = mainAccountMap.get(currencyCode).get(tag);
                    if(tagAccountAmount.compareTo(BigDecimal.ZERO) < 0) throw new ExceptionVS("Error with user: " +
                            user.getId() + " - tag " + tag + " - tagAccountAmount: " + tagAccountAmount +
                            " - currency: " + currencyCode + " - accounts: " + accountsMap + " - balance:" + balancesMap);
                }
            }
            //check WILDTAG balance result match with stored in account
            BigDecimal wildTagAccountAmount = mainAccountMap.get(currencyCode).get(TagVS.WILDTAG);
            if(wildTagAccountAmount.compareTo(wildTagBalance.subtract(wildTagExpendedInTags)) != 0) throw new ExceptionVS(
                    format("ERROR - wildTag mismatch -  user: ''{0}'' - currency: ''{1}'' - wildTagAccountAmount: ''{2}'' " +
                            "- wildTagExpendedInTags: ''{3}'' - wildTagBalance: ''{4}'' - accounts: ''{5}''",
                            user.getId(), currencyCode, wildTagAccountAmount, wildTagExpendedInTags, wildTagBalance, accountsMap));

        }
    }
}