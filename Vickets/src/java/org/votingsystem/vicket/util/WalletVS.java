package org.votingsystem.vicket.util;

import org.apache.log4j.Logger;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.vicket.model.UserVSAccount;
import org.votingsystem.model.VicketTagVS;

import java.math.BigDecimal;
import java.util.*;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletVS {

    private static Logger log = Logger.getLogger(WalletVS.class);

    private Set<UserVSAccount> accounts = null;
    private String currencyCode;
    private Map<String, UserVSAccount> tagDataMap = new HashMap<String, UserVSAccount>();

    public WalletVS(List accountList, String currencyCode) {
        this.currencyCode = currencyCode;
        accounts = new HashSet(accountList);
        for(UserVSAccount account : accounts) {
            tagDataMap.put(account.getTag().getName(), account);
        }
    }

    public ResponseVS<Map<UserVSAccount, BigDecimal>> getAccountMovementsForTransaction(
            VicketTagVS tag, BigDecimal amount, String currencyCode) throws Exception {
        if(amount.compareTo(BigDecimal.ZERO) < 0) throw new ExceptionVS(
                ApplicationContextHolder.getMessage("negativeAmountRequestedErrorMsg", amount.toString()));

        Map<UserVSAccount, BigDecimal> result = new HashMap<UserVSAccount, BigDecimal>();
        UserVSAccount wildTagAccount = tagDataMap.get(VicketTagVS.WILDTAG);
        if(tag == null) throw new ExceptionVS("Transaction without tag!!!");
        if(!VicketTagVS.WILDTAG.equals(tag.getName())) {
            UserVSAccount tagAccount = tagDataMap.get(tag.getName());
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
                    return new ResponseVS<>(ResponseVS.SC_ERROR, ApplicationContextHolder.getMessage(
                            "lowBalanceForTagErrorMsg", tag.getName(), available.toString() + " " + currencyCode,
                            amount.toString() + " " + currencyCode));
                }
            }
        } else {
            if(wildTagAccount.getBalance().compareTo(amount) > 0) {
                result.put(wildTagAccount, amount);
            } else {
                return new ResponseVS<>(ResponseVS.SC_ERROR, ApplicationContextHolder.getMessage(
                        "wildTagLowBalanceErrorMsg", wildTagAccount.getBalance() + " " + currencyCode,  amount + " " + currencyCode));
            }
        }
        return new ResponseVS<Map<UserVSAccount, BigDecimal>>(ResponseVS.SC_OK, null, result);
    }

    public BigDecimal getTagBalance(String tagName) {
        UserVSAccount account = tagDataMap.get(tagName);
        if(account != null) return account.getBalance();
        else return null;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

}
