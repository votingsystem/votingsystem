package org.currency.web.ejb;

import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.Messages;

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
                                                                              Tag tag, BigDecimal amount, CurrencyCode currencyCode) throws Exception {
        if(tag == null)
            throw new ValidationException("Transaction without tag!!!");
        if(amount.compareTo(BigDecimal.ZERO) < 0) throw new ValidationException(
                "negativeAmountRequestedErrorMsg: " +  amount.toString());

        List<CurrencyAccount> currencyAccounts = em.createNamedQuery(
                CurrencyAccount.FIND_BY_USER_IBAN_AND_TAG_AND_CURRENCY_CODE_AND_STATE)
                .setParameter("userIBAN", fromUserIBAN).setParameter("currencyCode", currencyCode)
                .setParameter("tag", config.getTag(Tag.WILDTAG)).setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
        if(currencyAccounts.isEmpty())
            throw new ValidationException("WILDTAG not found for IBAN:" + fromUserIBAN + " - " + currencyCode);
        CurrencyAccount wildTagAccount = currencyAccounts.iterator().next();
        CurrencyAccount tagAccount = null;
        if(tag != null && !tag.getName().equals(Tag.WILDTAG)) {
            currencyAccounts = em.createNamedQuery(CurrencyAccount.FIND_BY_USER_IBAN_AND_TAG_AND_CURRENCY_CODE_AND_STATE)
                    .setParameter("userIBAN", fromUserIBAN).setParameter("currencyCode", currencyCode)
                    .setParameter("tag",tag).setParameter("state", CurrencyAccount.State.ACTIVE).getResultList();
            if(!currencyAccounts.isEmpty())
                tagAccount = currencyAccounts.iterator().next();
        }
        if(wildTagAccount == null && tagAccount == null) throw new ValidationException(
                "no accounts for IBAN:" + fromUserIBAN + " - " + tag.getName() + " - " + currencyCode);
        Map<CurrencyAccount, BigDecimal> result = new HashMap<>();

        if(!Tag.WILDTAG.equals(tag.getName())) {
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
            } else throw new ValidationException(Messages.currentInstance().get("wildTagLowBalanceErrorMsg",
                    wildTagAccount.getBalance() + " " + currencyCode,  amount + " " + currencyCode));
        }
        return result;
    }

}