package org.votingsystem.cooin.util;

import org.apache.log4j.Logger;
import org.votingsystem.cooin.model.CooinAccount;
import org.votingsystem.model.TagVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletVS {

    private static Logger log = Logger.getLogger(WalletVS.class);

    private Set<CooinAccount> accounts = null;
    private String currencyCode;
    private Map<String, CooinAccount> tagDataMap = new HashMap<String, CooinAccount>();

    public WalletVS(List accountList, String currencyCode) {
        this.currencyCode = currencyCode;
        accounts = new HashSet(accountList);
        for(CooinAccount account : accounts) {
            tagDataMap.put(account.getTag().getName(), account);
        }
    }

    public Map<CooinAccount, BigDecimal> getAccountMovementsForTransaction(
            TagVS tag, BigDecimal amount, String currencyCode) throws Exception {
        if(amount.compareTo(BigDecimal.ZERO) < 0) throw new ExceptionVS(
                ApplicationContextHolder.getMessage("negativeAmountRequestedErrorMsg", amount.toString()));
        Map<CooinAccount, BigDecimal> result = new HashMap<CooinAccount, BigDecimal>();
        CooinAccount wildTagAccount = tagDataMap.get(TagVS.WILDTAG);
        if(tag == null) throw new ExceptionVS("Transaction without tag!!!");
        if(!TagVS.WILDTAG.equals(tag.getName())) {
            CooinAccount tagAccount = tagDataMap.get(tag.getName());
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
                    throw new ValidationExceptionVS(this.getClass(), ApplicationContextHolder.getMessage(
                            "lowBalanceForTagErrorMsg", tag.getName(), available.toString() + " " + currencyCode,
                            amount.toString() + " " + currencyCode));
                }
            }
        } else {
            if(wildTagAccount.getBalance().compareTo(amount) > 0) {
                result.put(wildTagAccount, amount);
            } else throw new ValidationExceptionVS(this.getClass(), ApplicationContextHolder.getMessage(
                    "wildTagLowBalanceErrorMsg", wildTagAccount.getBalance() + " " + currencyCode,  amount + " " +
                    currencyCode));
        }
        return result;
    }

    public BigDecimal getTagBalance(String tagName) {
        CooinAccount account = tagDataMap.get(tagName);
        if(account != null) return account.getBalance();
        else return null;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

}
