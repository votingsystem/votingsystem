package org.votingsystem.web.currency.ejb;

import org.votingsystem.model.CurrencyCode;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class WalletBean {

    @Inject ConfigVS config;
    @Inject DAOBean dao;


    public Map<CurrencyAccount, BigDecimal> getAccountMovementsForTransaction(String fromUserIBAN,
               TagVS tag, BigDecimal amount, CurrencyCode currencyCode) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        if(tag == null) throw new ExceptionVS("Transaction without tag!!!");
        if(amount.compareTo(BigDecimal.ZERO) < 0) throw new ExceptionVS(
                "negativeAmountRequestedErrorMsg: " +  amount.toString());

        Query query = dao.getEM().createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                .setParameter("userIBAN", fromUserIBAN).setParameter("currencyCode", currencyCode)
                .setParameter("tag", config.getTag(TagVS.WILDTAG)).setParameter("state", CurrencyAccount.State.ACTIVE);
        CurrencyAccount wildTagAccount = dao.getSingleResult(CurrencyAccount.class, query);
        CurrencyAccount tagAccount = null;
        if(tag != null && !tag.getName().equals(TagVS.WILDTAG)) {
            query = dao.getEM().createNamedQuery("findAccountByUserIBANAndTagAndCurrencyCodeAndState")
                    .setParameter("userIBAN", fromUserIBAN).setParameter("currencyCode", currencyCode)
                    .setParameter("tag",tag).setParameter("state", CurrencyAccount.State.ACTIVE);
            tagAccount = dao.getSingleResult(CurrencyAccount.class, query);
        }
        if(wildTagAccount == null && tagAccount == null) throw new ExceptionVS(
                "no accounts for IBAN:" + fromUserIBAN + " - " + tag.getName() + " - " + currencyCode);
        Map<CurrencyAccount, BigDecimal> result = new HashMap<>();

        if(!TagVS.WILDTAG.equals(tag.getName())) {
            if(tagAccount != null && tagAccount.getBalance().compareTo(amount) > 0) result.put(tagAccount, amount);
            else {
                BigDecimal tagAccountDeficit = amount;
                if(tagAccount != null) tagAccountDeficit = amount.subtract(tagAccount.getBalance());
                if(wildTagAccount.getBalance().compareTo(tagAccountDeficit) > 0) {
                    if(tagAccount != null) result.put(tagAccount, tagAccount.getBalance());
                    result.put(wildTagAccount, tagAccountDeficit);
                } else {
                    BigDecimal tagAccountAvailable = (tagAccount != null) ? tagAccount.getBalance():BigDecimal.ZERO;
                    BigDecimal available = tagAccountAvailable.add(wildTagAccount.getBalance());
                    String msg = "lowBalanceForTagErrorMsg: " + tag.getName() + " " + available.toString() + " " +
                            currencyCode + " - " + amount.toString() + " " + currencyCode;
                    throw new ValidationException(msg);
                }
            }
        } else {
            if(wildTagAccount.getBalance().compareTo(amount) > 0) {
                result.put(wildTagAccount, amount);
            } else throw new ValidationException(messages.get("wildTagLowBalanceErrorMsg",
                    wildTagAccount.getBalance() + " " + currencyCode,  amount + " " + currencyCode));
        }
        return result;
    }

}