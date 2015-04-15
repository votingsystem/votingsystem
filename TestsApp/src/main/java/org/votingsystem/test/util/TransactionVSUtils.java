package org.votingsystem.test.util;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSUtils {

    static Logger log =  Logger.getLogger(TransactionVSUtils.class.getSimpleName());

   /* public static Map getGroupVSTransactionVS(TransactionVS transactionVS, UserVS groupVS) {
        Map result = new HashMap();
        result.put("operation", transactionVS.getType().toString());
        result.put("fromUser", groupVS.getName());
        result.put("fromUserIBAN", groupVS.getIBAN());
        result.put("amount", transactionVS.getAmount().toString());
        result.put("currencyCode", transactionVS.getCurrencyCode());
        result.put("subject", transactionVS.getSubject() + " - " + DateUtils.getDayWeekDateStr(new Date()));
        if(TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS != transactionVS.getType()) {
            result.put("toUserName", transactionVS.getToUserVS().getName());
            result.put("toUserIBAN", Arrays.asList(transactionVS.getToUserVS().getIBAN()));
        } else result.put("toUserIBAN", transactionVS.getToUserVSList());
        result.put("isTimeLimited", transactionVS.getIsTimeLimited());
        result.put("tags", Arrays.asList(transactionVS.getTag().getName()));
        result.put("UUID", UUID.randomUUID().toString());
        return result;
    }*/

    /*public static Map getBankVSTransactionVS(TransactionVS transactionVS) {
        Map result = new HashMap();
        result.put("operation", transactionVS.getType().toString());
        result.put("bankIBAN", transactionVS.getFromUserVS().getIBAN());
        result.put("fromUser", transactionVS.getFromUser());
        result.put("fromUserIBAN", transactionVS.getFromUserIBAN());
        result.put("amount", transactionVS.getAmount().toString());
        result.put("currencyCode", transactionVS.getCurrencyCode());
        result.put("subject", transactionVS.getSubject() + " - " + DateUtils.getDayWeekDateStr(new Date()));
        result.put("toUserName", transactionVS.getToUserVS().getName());
        result.put("toUserIBAN", Arrays.asList(transactionVS.getToUserVS().getIBAN()));
        result.put("isTimeLimited", transactionVS.getIsTimeLimited());
        result.put("tags", Arrays.asList(transactionVS.getTag().getName()));
        result.put("UUID", UUID.randomUUID().toString());
        return result;
    }*/

    public static Map<String, Map<String, BigDecimal>> getCurrencyMap(List<TransactionVS> transactionVSList) {
        Map<String, Map<String, BigDecimal>> currencyMap = new HashMap<String, Map<String, BigDecimal>>();
        Map<String, BigDecimal> tagMap = null;
        for(TransactionVS transactionVS:transactionVSList) {
            if((tagMap = currencyMap.get(transactionVS.getCurrencyCode())) != null) {
                if((tagMap = currencyMap.get(transactionVS.getTag().getName())) != null) {
                    BigDecimal newAmount = tagMap.get(transactionVS.getTag().getName()).add(transactionVS.getAmount());
                    tagMap.put(transactionVS.getTag().getName(), newAmount);
                    continue;
                } else tagMap.put(transactionVS.getTag().getName(), transactionVS.getAmount());
            } else {
                tagMap = new HashMap<String, BigDecimal>();
                tagMap.put(transactionVS.getTag().getName(), transactionVS.getAmount());
                currencyMap.put(transactionVS.getCurrencyCode(), tagMap);
            }
        }
        return currencyMap;
    }

    public static Map sumCurrencyMap(Map<String, Map<String, BigDecimal>> destMap, Map<String, Map<String, BigDecimal>> mapToSum) {
        Map<String, BigDecimal> tagMap = null;
        for(String currency:mapToSum.keySet()) {
            if((tagMap = destMap.get(currency)) != null) {
                for(String tag : mapToSum.get(currency).keySet()) {
                    if(tagMap.containsKey(tag)) {
                        BigDecimal newAmount = tagMap.get(tag).add( mapToSum.get(currency).get(tag));
                        tagMap.put(tag, newAmount);
                    } else {
                        tagMap.put(tag,  mapToSum.get(currency).get(tag));
                    }
                }
            } else {
                destMap.put(currency, new HashMap<>(mapToSum.get(currency)));
            }
        }
        return destMap;
    }

    public static Map<String, Map<String, BigDecimal>> filterBalanceTo(Map<String, Map<String, Map>> balanceTo) {
        Map result = new HashMap<>();
        Map currencyMap = null;
        for(String currency : balanceTo.keySet()) {
            currencyMap = new HashMap<>();
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
        Map<String, BigDecimal> tagMap = null;
        for(String currency: balancesFrom.keySet()) {
            if(balancesCash.containsKey(currency)) {
                for(String tag : balancesFrom.get(currency).keySet()) {
                    if(balancesCash.get(currency).get(tag) != null) {
                        balancesCash.get(currency).put(tag, balancesCash.get(currency).get(tag).subtract(balancesFrom.get(currency).get(tag)));
                        if(balancesCash.get(currency).get(tag).compareTo(BigDecimal.ZERO) < 0) {
                            balancesCash.get(currency).put(TagVS.WILDTAG, balancesCash.get(currency).get(
                                    TagVS.WILDTAG).add(balancesCash.get(currency).get(tag)));
                            balancesCash.get(currency).put(tag, BigDecimal.ZERO);
                        }
                    } else balancesCash.get(currency).put(TagVS.WILDTAG, balancesCash.get(currency).get(
                            TagVS.WILDTAG).subtract(balancesFrom.get(currency).get(tag)));
                }
            } else {
                balancesCash.put(currency, new HashMap<>(balancesFrom.get(currency)));
                for(String tag : balancesCash.get(currency).keySet()) {
                    balancesCash.get(currency).put(tag, balancesCash.get(currency).get(tag).negate());
                }
            }
        }
        return balancesCash;
    }

    public static Map<String, Report> sumReport(Map<String, Report> reportMap, Map<String, Report> newReport) throws ExceptionVS {
        Map<String, Report> result = new HashMap<>();
        result.put("transactionFromList", reportMap.get("transactionFromList").sum(newReport.get("transactionFromList")));
        result.put("transactionToList", reportMap.get("transactionToList").sum(newReport.get("transactionToList")));
        return result;
    }

}