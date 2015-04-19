package org.votingsystem.test.util;

import org.votingsystem.model.currency.TransactionVS;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSUtils {

    static Logger log =  Logger.getLogger(TransactionVSUtils.class.getName());

    public static Map<String, Map<String, BigDecimal>> getCurrencyMap1(List<TransactionVS> transactionVSList) {
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


}