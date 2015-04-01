package org.votingsystem.web.currency.util;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ExceptionVS;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSUtils {

    public static Map<String, Map<String, BigDecimal>> getCurrencyMap(List<TransactionVS> transactionVSList) {
        Map<String, Map<String, BigDecimal>> result = new HashMap<>();
        for(TransactionVS transactionVS:transactionVSList) {
            if(result.containsKey(transactionVS.getCurrencyCode())) {
                if(result.get(transactionVS.getCurrencyCode()).containsKey(transactionVS.getTag().getName())) {
                    BigDecimal newAmount = result.get(transactionVS.getCurrencyCode()).get(
                            transactionVS.getTag().getName()).add(transactionVS.getAmount());
                    result.get(transactionVS.getCurrencyCode()).put(transactionVS.getTag().getName(), newAmount);
                } else result.get(transactionVS.getCurrencyCode()).put(transactionVS.getTag().getName(),
                        transactionVS.getAmount());
            } else {
                Map tagMap = new HashMap<>();
                tagMap.put(transactionVS.getTag().getName(), transactionVS.getAmount());
            }
        }
        return result;
    }

    public static Map<String, Map<String, BigDecimal>> filterBalanceTo(Map<String, Map<String, Map>> balanceTo) {
        Map result = new HashMap<>();
        for(String currency : balanceTo.keySet()) {
            Map currencyMap = new HashMap<>();
            for(String tag : balanceTo.get(currency).keySet()) {
                currencyMap.put(tag, balanceTo.get(currency).get(tag).get("total"));
            }
            result.put(currency, currencyMap);
        }
        return result;
    }

    public static Map<String, Map<String, BigDecimal>> balancesCash(Map<String, Map<String, Map>> balancesTo,
                        Map<String, Map<String, BigDecimal>> balancesFrom) {
        Map<String, Map<String, BigDecimal>> balancesCash = filterBalanceTo(balancesTo);
        for(String currency: balancesFrom.keySet()) {
            if(balancesCash.containsKey(currency)) {
                for(String tag : balancesFrom.get(currency).keySet()) {
                    if(balancesCash.get(currency).containsKey(tag)) {
                        BigDecimal newAmount = balancesCash.get(currency).get(tag).subtract(balancesFrom.get(currency).get(tag));
                        if(newAmount.compareTo(BigDecimal.ZERO) < 0) {
                            balancesCash.get(currency).put(TagVS.WILDTAG, balancesCash.get(currency).
                                    get(TagVS.WILDTAG).add(newAmount));
                            balancesCash.get(currency).put(tag, BigDecimal.ZERO);
                        } else  balancesCash.get(currency).put(tag, newAmount);
                    } else {
                        balancesCash.get(currency).put(TagVS.WILDTAG,  balancesCash.get(currency).get(TagVS.WILDTAG)
                                .subtract(balancesFrom.get(currency).get(tag)));
                    }
                }
            } else {
                Map<String, BigDecimal> tagData = new HashMap<String, BigDecimal>(balancesFrom.get(currency));
                for(String tag: tagData.keySet()) {
                    tagData.put(tag, tagData.get(tag).negate());
                }
            }
        }
        return balancesCash;
    }

    public static String getBalancesMapMsg(String forLbl, Map<String, Map<String, Object>> balancesMap) {
        StringBuilder sb = new StringBuilder();
        for(String currency: balancesMap.keySet()) {
            for(String tag : balancesMap.get(currency).keySet()) {
                sb.append(balancesMap.get(currency).get(tag) + " " + currency + " " + forLbl + " " + tag + " - ");
            }
        }
        return sb.toString();
    }


    public static BigDecimal checkRemainingForTag(Map<String, Map<String, String>> balancesFrom,
                  Map<String, Map<String, Map>> balancesTo, String tagName, String currencyCode) throws ExceptionVS {
        BigDecimal result = BigDecimal.ZERO;
        if(balancesTo.containsKey(currencyCode) && balancesTo.get(currencyCode).containsKey(tagName)) {
            Map tagData =  balancesTo.get(currencyCode).get(tagName);
            if(tagData.containsKey("timeLimited")) result = result.add(new BigDecimal(
                    (String) balancesTo.get(currencyCode).get(tagName).get("timeLimited")));
        }
        if(balancesFrom.containsKey(currencyCode) && balancesFrom.get(currencyCode).containsKey(tagName)) {
            result = result.subtract(new BigDecimal(balancesFrom.get(currencyCode).get(tagName)));
        }
        if(result.compareTo(BigDecimal.ZERO) < 0 && !TagVS.WILDTAG.equals(tagName)) throw new ExceptionVS(
                "Negative period balance for tag:" + tagName + " - " + currencyCode + result.toString());
        return result;
    }
}
