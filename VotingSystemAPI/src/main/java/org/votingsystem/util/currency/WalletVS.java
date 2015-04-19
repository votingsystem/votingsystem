package org.votingsystem.util.currency;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletVS {

    private static Logger log = Logger.getLogger(WalletVS.class.getSimpleName());

    private Set<CurrencyAccount> accounts = null;
    private String currencyCode;
    private Map<String, CurrencyAccount> tagDataMap = new HashMap<String, CurrencyAccount>();

    public WalletVS(List accountList, String currencyCode) {
        this.currencyCode = currencyCode;
        accounts = new HashSet(accountList);
        for(CurrencyAccount account : accounts) {
            tagDataMap.put(account.getTag().getName(), account);
        }
    }

    public Map<CurrencyAccount, BigDecimal> getAccountMovementsForTransaction(
            TagVS tag, BigDecimal amount, String currencyCode) throws Exception {
        if(amount.compareTo(BigDecimal.ZERO) < 0) throw new ExceptionVS(
                "negativeAmountRequestedErrorMsg: " +  amount.toString());
        Map<CurrencyAccount, BigDecimal> result = new HashMap<CurrencyAccount, BigDecimal>();
        CurrencyAccount wildTagAccount = tagDataMap.get(TagVS.WILDTAG);
        if(tag == null) throw new ExceptionVS("Transaction without tag!!!");
        if(!TagVS.WILDTAG.equals(tag.getName())) {
            CurrencyAccount tagAccount = tagDataMap.get(tag.getName());
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
                    throw new ValidationExceptionVS(msg);
                }
            }
        } else {
            if(wildTagAccount.getBalance().compareTo(amount) > 0) {
                result.put(wildTagAccount, amount);
            } else throw new ValidationExceptionVS("wildTagLowBalanceErrorMsg: " +
                    wildTagAccount.getBalance() + " " + currencyCode,  amount + " " + currencyCode);
        }
        return result;
    }

    public BigDecimal getTagBalance(String tagName) {
        CurrencyAccount account = tagDataMap.get(tagName);
        if(account != null) return account.getBalance();
        else return null;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

}
