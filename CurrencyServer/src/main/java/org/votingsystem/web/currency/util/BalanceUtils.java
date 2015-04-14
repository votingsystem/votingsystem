package org.votingsystem.web.currency.util;

import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.model.currency.TransactionVS;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BalanceUtils {

    private static final Function<TransactionVS, String> currencyCode = new Function<TransactionVS, String> () {
        @Override public String apply(TransactionVS transactionVS) { return transactionVS.getCurrencyCode(); }
    };
    private static final Function<TransactionVS, String> tagName = new Function<TransactionVS, String> () {
        @Override public String apply(TransactionVS transactionVS) { return transactionVS.getTagName(); }
    };


    public static Map<String, Map<String, BigDecimal>> getBalancesFrom(Collection<TransactionVS> transactionList) {
        Collector<TransactionVS, ?, ?> amountCollector = new TransactionVSFromAmountCollector();
        Map<String, List<TransactionVS>> currencyMaps =  transactionList.stream().collect(groupingBy(currencyCode));
        Map<String, Map<String, BigDecimal>> result = new HashMap<>();
        for(String currency : currencyMaps.keySet()) {
            Map tagVSMap = currencyMaps.get(currency).stream().collect(groupingBy(tagName, amountCollector));
            result.put(currency, tagVSMap);
        }
        return result;
    }

    public static Map<String, Map<String, IncomesDto>> getBalancesTo(Collection<TransactionVS> transactionList) {
        Collector<TransactionVS, ?, ?> amountCollector = new TransactionVSToAmountCollector();
        Map<String, Map<String, IncomesDto>> result = new HashMap<>();
        Map<String, List<TransactionVS>> currencyMaps =  transactionList.stream().collect(groupingBy(currencyCode));
        for(String currency : currencyMaps.keySet()) {
            Map tagVSMap = currencyMaps.get(currency).stream().collect(groupingBy(tagName, amountCollector));
            result.put(currency, tagVSMap);
        }
        return result;
    }

}
