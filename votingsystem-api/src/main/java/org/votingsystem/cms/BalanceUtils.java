package org.votingsystem.cms;

import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.CurrencyCode;

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

    private static final Function<Transaction, CurrencyCode> currencyCode = new Function<Transaction, CurrencyCode> () {
        @Override public CurrencyCode apply(Transaction transaction) { return transaction.getCurrencyCode(); }
    };
    private static final Function<Transaction, String> tagName = new Function<Transaction, String> () {
        @Override public String apply(Transaction transaction) { return transaction.getTag().getName(); }
    };


    public static Map<CurrencyCode, Map<String, BigDecimal>> getBalancesFrom(Collection<Transaction> transactionList) {
        Collector<Transaction, ?, ?> amountCollector = new TransactionFromAmountCollector();
        Map<CurrencyCode, List<Transaction>> currencyMaps =  transactionList.stream().collect(groupingBy(currencyCode));
        Map<CurrencyCode, Map<String, BigDecimal>> result = new HashMap<>();
        for(CurrencyCode currency : currencyMaps.keySet()) {
            Map tagVSMap = currencyMaps.get(currency).stream().collect(groupingBy(tagName, amountCollector));
            result.put(currency, tagVSMap);
        }
        return result;
    }

    public static Map<CurrencyCode, Map<String, IncomesDto>> getBalancesTo(Collection<Transaction> transactionList) {
        Collector<Transaction, ?, ?> amountCollector = new TransactionToAmountCollector();
        Map<CurrencyCode, Map<String, IncomesDto>> result = new HashMap<>();
        Map<CurrencyCode, List<Transaction>> currencyMaps =  transactionList.stream().collect(groupingBy(currencyCode));
        for(CurrencyCode currency : currencyMaps.keySet()) {
            Map tagVSMap = currencyMaps.get(currency).stream().collect(groupingBy(tagName, amountCollector));
            result.put(currency, tagVSMap);
        }
        return result;
    }

}
