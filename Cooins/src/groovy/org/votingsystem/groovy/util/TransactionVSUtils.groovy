package org.votingsystem.groovy.util

import org.votingsystem.model.TagVS
import org.votingsystem.util.ExceptionVS
import org.votingsystem.cooin.model.TransactionVS
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TransactionVSUtils {

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

    public static Map addTransactionVSToBalance(Map<String, Map> balancesMap, TransactionVS transactionVS) {
        if(balancesMap[transactionVS.currencyCode]) {
            Map<String, Map> currencyMap = balancesMap[transactionVS.currencyCode]
            if(currencyMap[transactionVS.tag.name]) {
                if(transactionVS.validTo){
                    currencyMap[transactionVS.tag.name].total = currencyMap[transactionVS.tag.name].total.add(
                            transactionVS.amount)
                    currencyMap[transactionVS.tag.name].timeLimited = currencyMap[transactionVS.tag.name].timeLimited.add(
                            transactionVS.amount)
                } else {
                    currencyMap[transactionVS.tag.name].total = currencyMap[transactionVS.tag.name].total.add(
                            transactionVS.amount)
                }
            } else {
                Map tagDataMap
                if(transactionVS.validTo){
                    tagDataMap = [total:transactionVS.amount, timeLimited:transactionVS.amount]
                } else tagDataMap = [total:transactionVS.amount, timeLimited:BigDecimal.ZERO]
                currencyMap[(transactionVS.tag.name)] = tagDataMap
            }
        } else {
            Map tagDataMap
            if(transactionVS.validTo){
                tagDataMap = [(transactionVS.tag.name):[total:transactionVS.amount, timeLimited:transactionVS.amount]]
            } else tagDataMap = [(transactionVS.tag.name):[total:transactionVS.amount, timeLimited:BigDecimal.ZERO]]
            balancesMap[(transactionVS.currencyCode)] = tagDataMap
        }
        return balancesMap
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

    public static Map<String, Map> getBalancesMap(Collection<TransactionVS> transactionVSCollection) {
        Map resultMap = [:]
        for(TransactionVS transactionVS : transactionVSCollection) {
            if(resultMap[transactionVS.getCurrencyCode()]) {
                if(resultMap[transactionVS.getCurrencyCode()][transactionVS.getTag().getName()]) {
                    resultMap[transactionVS.getCurrencyCode()][transactionVS.getTag().getName()] =
                            resultMap[transactionVS.getCurrencyCode()][transactionVS.getTag().getName()].add(transactionVS.amount)
                } else resultMap[transactionVS.getCurrencyCode()][transactionVS.getTag().getName()] = transactionVS.amount
            } else {
                resultMap[(transactionVS.getCurrencyCode())] = [(transactionVS.getTag().getName()): transactionVS.amount]
            }
        }
        return resultMap
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