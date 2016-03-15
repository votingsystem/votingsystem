package org.votingsystem.util.currency;

import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.model.currency.Transaction;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.groupingBy;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BalanceUtils {

    private static final Function<Transaction, String> currencyCode = new Function<Transaction, String> () {
        @Override public String apply(Transaction transaction) { return transaction.getCurrencyCode(); }
    };
    private static final Function<Transaction, String> tagName = new Function<Transaction, String> () {
        @Override public String apply(Transaction transaction) { return transaction.getTagName(); }
    };


    public static Map<String, Map<String, BigDecimal>> getBalancesFrom(Collection<Transaction> transactionList) {
        Collector<Transaction, ?, ?> amountCollector = new TransactionFromAmountCollector();
        Map<String, List<Transaction>> currencyMaps =  transactionList.stream().collect(groupingBy(currencyCode));
        Map<String, Map<String, BigDecimal>> result = new HashMap<>();
        for(String currency : currencyMaps.keySet()) {
            Map tagVSMap = currencyMaps.get(currency).stream().collect(groupingBy(tagName, amountCollector));
            result.put(currency, tagVSMap);
        }
        return result;
    }

    public static Map<String, Map<String, IncomesDto>> getBalancesTo(Collection<Transaction> transactionList) {
        Collector<Transaction, ?, ?> amountCollector = new TransactionToAmountCollector();
        Map<String, Map<String, IncomesDto>> result = new HashMap<>();
        Map<String, List<Transaction>> currencyMaps =  transactionList.stream().collect(groupingBy(currencyCode));
        for(String currency : currencyMaps.keySet()) {
            Map tagVSMap = currencyMaps.get(currency).stream().collect(groupingBy(tagName, amountCollector));
            result.put(currency, tagVSMap);
        }
        return result;
    }

}
