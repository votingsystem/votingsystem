package org.votingsystem.web.currency.ejb;

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
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;


@Stateless
public class CurrencyAccountBean {

    @Inject DAOBean dao;
    @Inject ConfigVS config;

    public CurrencyAccount checkUserAccount(User user) throws Exception {
        Query query = dao.getEM().createNamedQuery("findAccountByUser").setParameter("user", user);
        CurrencyAccount userAccount = dao.getSingleResult(CurrencyAccount.class, query);
        if(userAccount == null) {
            userAccount = dao.persist(new CurrencyAccount(user, BigDecimal.ZERO, Currency.getInstance("EUR")
                    .getCurrencyCode(), config.getTag(TagVS.WILDTAG)));
        }
        return userAccount;
    }

    public Map<String, Map<String, Map<String, BigDecimal>>> getAccountsBalanceMap(User user) {
        Query query = dao.getEM().createNamedQuery("findAccountByTypeAndUser").setParameter(
                "type", CurrencyAccount.Type.SYSTEM).setParameter("user", user);
        List<CurrencyAccount> currencyAccounts = query.getResultList();
        Map<String, Map<String, Map<String, BigDecimal>>> result = new HashMap<>();
        for(CurrencyAccount account: currencyAccounts) {
            if(result.containsKey(account.getIBAN())) {
                if(result.get(account.getIBAN()).containsKey(account.getCurrencyCode())) {
                    result.get(account.getIBAN()).get(account.getCurrencyCode()).put(account.getTag().getName(),
                            account.getBalance());
                } else {
                    Map tagDataMap = new HashMap<>();
                    tagDataMap.put(account.getTag().getName(), account.getBalance().toString());
                    result.get(account.getIBAN()).put(account.getCurrencyCode(), tagDataMap);
                }
            } else {
                Map<String, Map<String, BigDecimal>> currencyDataMap = new HashMap<>();
                Map<String, BigDecimal> tagDataMap = new HashMap<>();
                tagDataMap.put(account.getTag().getName(), account.getBalance());
                currencyDataMap.put(account.getCurrencyCode(), tagDataMap);
                result.put(account.getIBAN(), currencyDataMap);
            }
        }
        return result;
    }

    //Method that checks that the calculated balance from transactions corresponds with the accounts state
    public void checkBalancesWithCurrencyAccounts(User user, Map<String, Map<String, BigDecimal>> balancesMap) throws ExceptionVS {
        Map<String, Map<String, Map<String, BigDecimal>>> accountsMap = getAccountsBalanceMap(user);
        if(accountsMap.keySet().size() > 1) throw new ExceptionVS("User: " + user.getId() + "has " +
                accountsMap.keySet().size() + " accounts");
        if(accountsMap.keySet().isEmpty()) return;
        //TODO - bucle when User could have multiple accounts
        Map<String, Map<String, BigDecimal>> mainAccountMap = accountsMap.values().iterator().next();
        for(String currency : mainAccountMap.keySet()) {
            BigDecimal wildTagExpendedInTags = BigDecimal.ZERO;
            BigDecimal wildTagBalance = BigDecimal.ZERO;
            if(balancesMap.containsKey(currency)) {
                for(String tag: mainAccountMap.get(currency).keySet()) {
                    BigDecimal tagAccountAmount = mainAccountMap.get(currency).get(tag);
                    BigDecimal tagBalanceAmount = null;
                    if((tagBalanceAmount = balancesMap.get(currency).get(tag)) != null) {
                        if(TagVS.WILDTAG.equals(tag)) wildTagBalance = tagBalanceAmount;
                        else if(tagAccountAmount.compareTo(tagBalanceAmount) != 0)
                            wildTagExpendedInTags = wildTagExpendedInTags.add(tagBalanceAmount.subtract(tagAccountAmount));
                    } else {
                        if(tagAccountAmount.compareTo(BigDecimal.ZERO) != 0) throw new ExceptionVS(
                                "tagAccountAmount Error with user " + user.getId() + " - " + tag + " - " +
                                currency + " - tagAccountAmount: " + tagAccountAmount);
                    }
                }
            } else {//check if there's a tag with negative balance
                for(String tag: mainAccountMap.get(currency).keySet()) {
                    BigDecimal tagAccountAmount = mainAccountMap.get(currency).get(tag);
                    if(tagAccountAmount.compareTo(BigDecimal.ZERO) < 0) throw new ExceptionVS("Error with user: " +
                            user.getId() + " - tag " + tag + " - tagAccountAmount: " + tagAccountAmount + " - currency: " + currency +
                            " - accounts: " + accountsMap + " - balance:" + balancesMap);
                }
            }
            //check WILDTAG balance result match with stored in account
            BigDecimal wildTagAccountAmount = mainAccountMap.get(currency).get(TagVS.WILDTAG);
            if(wildTagAccountAmount.compareTo(wildTagBalance.subtract(wildTagExpendedInTags)) != 0) throw new ExceptionVS(
                    format("ERROR - wildTag mismatch -  user: ''{0}'' - currency: ''{1}'' - wildTagAccountAmount: ''{2}'' " +
                            "- wildTagExpendedInTags: ''{3}'' - wildTagBalance: ''{4}'' - accounts: ''{5}''",
                            user.getId(), currency, wildTagAccountAmount, wildTagExpendedInTags, wildTagBalance, accountsMap));

        }
    }
}