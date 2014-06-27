package org.votingsystem.vicket.model;

import org.apache.log4j.Logger;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVSAccount;
import org.votingsystem.model.VicketTagVS;
import org.votingsystem.vicket.util.ApplicationContextHolder;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by jgzornoza on 25/06/14.
 */
public class WalletVS {

    private static Logger log = Logger.getLogger(WalletVS.class);

    public static final String ACCOUNT_WITHOUT_TAG = "ACCOUNT_WITHOUT_TAG";

    private Set<UserVSAccount> accounts = null;
    private String currencyCode;
    private Map<String, UserVSAccount> tagDataMap = new HashMap<String, UserVSAccount>();

    public WalletVS(List accountList, String currencyCode) {
        this.currencyCode = currencyCode;
        accounts = new HashSet(accountList);
        for(UserVSAccount account : accounts) {
            if(account.getTag() == null) {
                //There must be only one account without tag
                tagDataMap.put(ACCOUNT_WITHOUT_TAG, account);
            } else tagDataMap.put(account.getTag().getName(), account);
        }
    }

    public ResponseVS<Map<UserVSAccount, BigDecimal>> getAccountMovementsForTransaction(VicketTagVS tag, BigDecimal amount) {
        if(amount.compareTo(BigDecimal.ZERO) < 0) {
            log.debug("getMovementsForTransaction - negative amount: " + amount);
            return new ResponseVS<>(ResponseVS.SC_ERROR, ApplicationContextHolder.getMessage(
                    "negativeAmountRequestedErrorMsg", amount.toString()));
        }
        Map<UserVSAccount, BigDecimal> result = new HashMap<UserVSAccount, BigDecimal>();
        UserVSAccount noTagAccount = tagDataMap.get(ACCOUNT_WITHOUT_TAG);
        if(tag != null) {
            UserVSAccount tagAccount = tagDataMap.get(tag.getName());
            if(tagAccount != null && tagAccount.getBalance().compareTo(amount) > 0) result.put(tagAccount, amount);
            else {
                BigDecimal tagMissing = amount;
                if(tagAccount != null) tagMissing = amount.subtract(tagAccount.getBalance());
                if(noTagAccount.getBalance().compareTo(tagMissing) > 0) {
                    if(tagAccount != null) result.put(tagAccount, tagAccount.getBalance());
                    result.put(noTagAccount, tagMissing);
                } else {
                    BigDecimal available = tagAccount.getBalance().add(noTagAccount.getBalance());
                    return new ResponseVS<>(ResponseVS.SC_ERROR, ApplicationContextHolder.getMessage(
                            "lowBalanceForTagErrorMsg", tag.getName(), available.toString(), amount.toString()));
                }
            }
        } else {
            if(noTagAccount.getBalance().compareTo(amount) > 0) {
                result.put(noTagAccount, amount);
            } else {
                return new ResponseVS<>(ResponseVS.SC_ERROR, ApplicationContextHolder.getMessage(
                        "balanceErrorMsg", noTagAccount.getBalance() + " " + currencyCode,  amount + " " + currencyCode));
            }
        }
        return new ResponseVS<Map<UserVSAccount, BigDecimal>>(ResponseVS.SC_OK,  result);
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
