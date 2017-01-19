package org.votingsystem.cms;

import org.votingsystem.model.currency.Transaction;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionUtils {

    public static Map<String, Map<String, BigDecimal>> getCurrencyMap(List<Transaction> transactionList) {
        Map<String, Map<String, BigDecimal>> result = new HashMap<>();
        for(Transaction transaction : transactionList) {
            if(result.containsKey(transaction.getCurrencyCode())) {
                if(result.get(transaction.getCurrencyCode()).containsKey(transaction.getTag().getName())) {
                    BigDecimal newAmount = result.get(transaction.getCurrencyCode()).get(
                            transaction.getTag().getName()).add(transaction.getAmount());
                    result.get(transaction.getCurrencyCode()).put(transaction.getTag().getName(), newAmount);
                } else result.get(transaction.getCurrencyCode()).put(transaction.getTag().getName(),
                        transaction.getAmount());
            } else {
                Map tagMap = new HashMap<>();
                tagMap.put(transaction.getTag().getName(), transaction.getAmount());
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
