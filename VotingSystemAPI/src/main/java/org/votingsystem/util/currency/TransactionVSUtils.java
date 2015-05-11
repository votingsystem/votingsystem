package org.votingsystem.util.currency;

import org.votingsystem.model.currency.TransactionVS;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
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

    public static String getBalancesMapMsg(String forLbl, Map<String, Map<String, Object>> balancesMap) {
        StringBuilder sb = new StringBuilder();
        for(String currency: balancesMap.keySet()) {
            for(String tag : balancesMap.get(currency).keySet()) {
                sb.append(balancesMap.get(currency).get(tag) + " " + currency + " " + forLbl + " " + tag + " - ");
            }
        }
        return sb.toString();
    }

}
