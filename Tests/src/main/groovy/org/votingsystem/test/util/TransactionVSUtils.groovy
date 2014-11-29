package org.votingsystem.test.util

import org.apache.log4j.Logger
import org.votingsystem.model.TagVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils
import org.votingsystem.cooin.model.TransactionVS

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TransactionVSUtils {

    static Logger log =  Logger.getLogger(TransactionVSUtils.class);

    public static Map getGroupVSTransactionVS(TransactionVS transactionVS, UserVS groupVS) {
        Map result = new HashMap();
        result.put("operation", transactionVS.type.toString());
        result.put("fromUser", groupVS.getName());
        result.put("fromUserIBAN", groupVS.getIBAN());
        result.put("amount", transactionVS.amount.toString());
        result.put("currencyCode", transactionVS.currencyCode);
        result.put("subject", transactionVS.subject + " - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()));
        if(TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS != transactionVS.type) {
            result.put("toUserName", transactionVS.toUserVS.getName());
            result.put("toUserIBAN", Arrays.asList(transactionVS.toUserVS.getIBAN()));
        } else result.put("toUserIBAN", transactionVS.getToUserVSList());
        result.put("isTimeLimited", transactionVS.isTimeLimited);
        result.put("tags", Arrays.asList(transactionVS.tag.getName()));
        result.put("UUID", UUID.randomUUID().toString());
        return result;
    }

    public static Map getBankVSTransactionVS(TransactionVS transactionVS) {
        Map result = new HashMap();
        result.put("operation", transactionVS.type.toString());
        result.put("bankIBAN", transactionVS.fromUserVS.getIBAN());
        result.put("fromUser", transactionVS.fromUser);
        result.put("fromUserIBAN", transactionVS.fromUserIBAN);
        result.put("amount", transactionVS.amount.toString());
        result.put("currencyCode", transactionVS.currencyCode);
        result.put("subject", transactionVS.subject + " - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()));
        result.put("toUserName", transactionVS.toUserVS.getName());
        result.put("toUserIBAN", Arrays.asList(transactionVS.toUserVS.getIBAN()));
        result.put("isTimeLimited", transactionVS.isTimeLimited);
        result.put("tags", Arrays.asList(transactionVS.tag.getName()));
        result.put("UUID", UUID.randomUUID().toString());
        return result;
    }

    public static Map getCurrencyMap(List<TransactionVS> transactionVSList) {
        Map result = [:]
        for(TransactionVS transactionVS:transactionVSList) {
            if(result[transactionVS.currencyCode]) {
                if(result[transactionVS.currencyCode][transactionVS.tag.name]) {
                    result[transactionVS.currencyCode][transactionVS.tag.name] =
                            ((BigDecimal)result[transactionVS.currencyCode][transactionVS.tag.name]).add(transactionVS.amount)
                } else result[transactionVS.currencyCode] = [(transactionVS.tag.name):transactionVS.amount]
            } else result[(transactionVS.currencyCode)] = [(transactionVS.tag.name):transactionVS.amount]
        }
        return result;
    }


    public static Map sumCurrencyMap(Map destMap, Map mapToSum) {
        for(String currency:mapToSum.keySet()) {
            if(destMap[currency]) {
                for(String tag : mapToSum[currency].keySet()) {
                    if(destMap[currency][tag]) {
                        destMap[currency][(tag)] =  destMap[currency][(tag)].add(mapToSum[currency][(tag)])
                    } else destMap[currency][(tag)] = mapToSum[currency][(tag)]
                }
            } else destMap[(currency)] = ([:] << mapToSum[currency])
        }
        return destMap
    }

    public static Map<String, Map> filterBalanceTo(Map<String, Map> balanceTo) {
        Map result = [:]
        for(String currency : balanceTo.keySet()) {
            Map currencyMap = [:]
            balanceTo[currency].each { tagEntry ->
                currencyMap[(tagEntry.key)]= tagEntry.value.total
            }
            result[(currency)] = currencyMap
        }
        return result
    }

    public static Map<String, BigDecimal> balancesCash(Map<String, Map> balancesTo, Map<String, Map> balancesFrom) {
        Map<String, Map> balancesCash = filterBalanceTo(balancesTo);
        for(String currency: balancesFrom.keySet()) {
            if(balancesCash [currency]) {
                for(String tag : balancesFrom[currency].keySet()) {
                    if(balancesCash [currency][tag]) {
                        balancesCash [currency][tag] =  balancesCash [currency][tag].subtract( balancesFrom[currency][tag])
                        if(balancesCash [currency][tag].compareTo(BigDecimal.ZERO) < 0) {
                            balancesCash [currency][TagVS.WILDTAG] =
                                    balancesCash[currency][TagVS.WILDTAG].add( balancesCash [currency][tag])
                            balancesCash [currency][tag] = BigDecimal.ZERO
                        }
                    } else balancesCash [currency][TagVS.WILDTAG] =
                            balancesCash[currency][TagVS.WILDTAG].subtract( balancesFrom[currency][tag])
                }
            } else {
                balancesCash[(currency)] = [:]
                balancesCash[(currency)].putAll(balancesFrom[currency])
                for(String tag : balancesCash[(currency)].keySet()) {
                    balancesCash[currency][tag] = balancesCash[currency][tag].negate()
                }
            }
        }
        return balancesCash
    }

    public static Map setBigDecimalToPlainString(Map mapToTransform) {
        for(String currency: mapToTransform.keySet()) {
            for(String tag: mapToTransform[currency].keySet()) {
                if(mapToTransform[currency][tag] instanceof BigDecimal) mapToTransform[currency][tag] =
                        ((BigDecimal)mapToTransform[currency][tag]).toPlainString()
                else {
                    mapToTransform[currency][tag].total =  ((BigDecimal)mapToTransform[currency][tag].total).toPlainString()
                    mapToTransform[currency][tag].timeLimited =  ((BigDecimal)mapToTransform[currency][tag].timeLimited).toPlainString()
                }
            }
        }
        return mapToTransform
    }
}
