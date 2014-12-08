package org.votingsystem.groovy.util

import org.votingsystem.model.TagVS
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.cooin.model.TransactionVS

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TransactionVSUtils {

    public static Map getCurrencyMap(List<TransactionVS> transactionVSList) {
        Map result = [:]
        for(TransactionVS transactionVS:transactionVSList) {
            if(result[transactionVS.currencyCode]) {
                if(result[transactionVS.currencyCode][transactionVS.tag.name]) {
                    result[transactionVS.currencyCode] =
                            ((BigDecimal)result[transactionVS.currencyCode]).add(transactionVS.amount)
                } else result[transactionVS.currencyCode] = [(transactionVS.tag.name):transactionVS.amount]
            } else result[(transactionVS.currencyCode)] = [(transactionVS.tag.name):transactionVS.amount]
        }
        return result;
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

    public static String getBalancesMapMsg(String forLbl, Map<String, Map> balancesMap) {
        StringBuilder sb = new StringBuilder()
        for(String currency: balancesMap.keySet()) {
            for(String tag : balancesMap[currency].keySet()) {
                if(sb.length() == 0) sb.append("${balancesMap[currency][tag]} $currency $forLbl $tag")
                else sb.append(", ${balancesMap[currency][tag]} $currency $forLbl $tag")
            }
        }
        sb.toString()
    }


    public static BigDecimal checkRemainingForTag(Map balancesFrom, Map balancesTo, String tagName, String currencyCode) {
        BigDecimal result = BigDecimal.ZERO;
        if(balancesTo[currencyCode] && balancesTo[currencyCode][tagName]?.timeLimited)
            result = result.add(new BigDecimal(balancesTo[currencyCode][tagName].timeLimited))
        if(balancesFrom[currencyCode] && balancesFrom[currencyCode][tagName])
            result = result.subtract(new BigDecimal(balancesFrom[currencyCode][tagName]))
        if(result.compareTo(BigDecimal.ZERO) < 0) throw new ExceptionVS("Negative period balance for tag '$tagName' " +
                "'$currencyCode': ${result.toString()} ")
        return result;
    }
}
