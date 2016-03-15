package org.votingsystem.test.util;

import org.votingsystem.model.currency.Transaction;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionUtils {

    static Logger log =  Logger.getLogger(TransactionUtils.class.getName());

    public static Map<String, Map<String, BigDecimal>> getCurrencyMap1(List<Transaction> transactionList) {
        Map<String, Map<String, BigDecimal>> currencyMap = new HashMap<String, Map<String, BigDecimal>>();
        Map<String, BigDecimal> tagMap = null;
        for(Transaction transaction : transactionList) {
            if((tagMap = currencyMap.get(transaction.getCurrencyCode())) != null) {
                if((tagMap = currencyMap.get(transaction.getTag().getName())) != null) {
                    BigDecimal newAmount = tagMap.get(transaction.getTag().getName()).add(transaction.getAmount());
                    tagMap.put(transaction.getTag().getName(), newAmount);
                    continue;
                } else tagMap.put(transaction.getTag().getName(), transaction.getAmount());
            } else {
                tagMap = new HashMap<String, BigDecimal>();
                tagMap.put(transaction.getTag().getName(), transaction.getAmount());
                currencyMap.put(transaction.getCurrencyCode(), tagMap);
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


}