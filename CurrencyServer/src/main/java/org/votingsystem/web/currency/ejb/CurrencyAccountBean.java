package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Stateless
public class CurrencyAccountBean {

    @Inject
    DAOBean dao;
    @Inject ConfigVS config;
    @Inject IBANBean ibanBean;


    public CurrencyAccount checkUserVSAccount(UserVS userVS) throws Exception {
        Query query = dao.getEM().createNamedQuery("findAccountByUser").setParameter("userVS", userVS);
        CurrencyAccount userAccount = dao.getSingleResult(CurrencyAccount.class, query);
        if(userAccount == null) {
            userVS.setIBAN(ibanBean.getIBAN(userVS.getId()));
            userAccount = dao.persist(new CurrencyAccount(userVS, BigDecimal.ZERO, Currency.getInstance("EUR")
                    .getCurrencyCode(), config.getTag(TagVS.WILDTAG)));
        }
        return userAccount;
    }

    public Map getUserVSAccountMap(CurrencyAccount currencyAccount) {
        Map result = new HashMap<>();
        result.put("id", currencyAccount.getId());
        result.put("currency", currencyAccount.getCurrencyCode());
        result.put("IBAN", currencyAccount.getIBAN());
        result.put("amount", currencyAccount.getBalance());
        result.put("lastUpdated", currencyAccount.getLastUpdated());
        Map tagData = new HashMap<>();
        tagData.put("id", currencyAccount.getTag().getId());
        tagData.put("name", currencyAccount.getTag().getName());
        result.put("tag", tagData);
        return result;
    }

    public Map getAccountsBalanceMap(UserVS userVS) {
        Query query = dao.getEM().createNamedQuery("findAccountByTypeAndUser").setParameter(
                "type", CurrencyAccount.Type.SYSTEM).setParameter("userVS", userVS);
        List<CurrencyAccount> currencyAccounts = query.getResultList();
        Map<String,Map<String, Map>> result = new HashMap<>();
        for(CurrencyAccount account: currencyAccounts) {
            if(result.containsKey(account.getIBAN())) {
                if(result.get(account.getIBAN()).containsKey(account.getCurrencyCode())) {
                    result.get(account.getIBAN()).get(account.getCurrencyCode()).put(account.getTag().getName(),
                            account.getBalance().toString());
                } else {
                    Map tagDataMap = new HashMap<>();
                    tagDataMap.put(account.getTag().getName(), account.getBalance().toString());
                    result.get(account.getIBAN()).put(account.getCurrencyCode(), tagDataMap);
                }
            } else {
                Map currencyDataMap = new HashMap<>();
                Map tagDataMap = new HashMap<>();
                tagDataMap.put(account.getTag().getName(), account.getBalance().toString());
                currencyDataMap.put(account.getCurrencyCode(), tagDataMap);
                result.put(account.getIBAN(), currencyDataMap);
            }
        }
        return result;
    }

    public void checkBalancesMap(UserVS userVS, Map<String, Map<String, BigDecimal>> balancesMap) throws ExceptionVS {
        Map<String, Map> accountsMap = getAccountsBalanceMap(userVS);
        if(accountsMap.keySet().size() > 1) throw new ExceptionVS("UserVS: " + userVS.getId() + "has " +
                accountsMap.keySet().size() + " accounts");
        if(accountsMap.keySet().isEmpty()) return;
        accountsMap = accountsMap.values().iterator().next();
        for(String currency : accountsMap.keySet()) {
            BigDecimal wildTagExpendedInTags = BigDecimal.ZERO;
            BigDecimal wildTagBalance = BigDecimal.ZERO;
            if(balancesMap.containsKey(currency)) {
                for(Object tag: accountsMap.get(currency).keySet()) {
                    BigDecimal tagAccount = new BigDecimal((String) accountsMap.get(currency).get(tag));
                    if(balancesMap.get(currency).containsKey(tag)) {
                        BigDecimal tagBalance = (BigDecimal) balancesMap.get(currency).get(tag);
                        if(TagVS.WILDTAG.equals(tag)) wildTagBalance = tagBalance;
                        else if(tagAccount.compareTo(tagBalance) != 0)
                            wildTagExpendedInTags = wildTagExpendedInTags.add(tagBalance.subtract(tagAccount));
                    } else {
                        if(tagAccount.compareTo(BigDecimal.ZERO) != 0) throw new ExceptionVS("Balance Error with userVS "
                                + userVS.getId() + " - " + tag + " - " + currency + " - accounts: " + accountsMap +
                                "balances: " + balancesMap);
                    }
                }
            } else {
                for(Object tag: accountsMap.get(currency).keySet()) {
                    BigDecimal tagAccount = new BigDecimal((String) accountsMap.get(currency).get(tag));
                    if(tagAccount.compareTo(BigDecimal.ZERO) != 0) throw new ExceptionVS("Error with userVS: " +
                            userVS.getId() + " - tag " + tag + " " + currency + " - accounts: " + accountsMap +
                            " - balance:" + balancesMap);
                }
            }
            //check WILDTAG result
            BigDecimal wildTagAccount = new BigDecimal((String) accountsMap.get(currency).get(TagVS.WILDTAG));
            if(wildTagAccount.compareTo(wildTagBalance.subtract(wildTagExpendedInTags)) != 0) throw new ExceptionVS(
                    "Balance Error with user " + "'$userVS.id' - '$currency' - accounts: '$accountsMap' - " +
                            "wildTagExpendedInTags  not resolved '$wildTagExpendedInTags'");

        }
    }
}
