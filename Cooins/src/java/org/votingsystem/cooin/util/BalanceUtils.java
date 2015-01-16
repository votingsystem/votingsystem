package org.votingsystem.cooin.util;

import org.votingsystem.cooin.model.TransactionVS;
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


    public static Map getBalances(Collection<TransactionVS> transactionList, TransactionVS.Source source) {
        Collector<TransactionVS, ?, ?> amountCollector = null;
        switch(source) {
            case FROM:
                amountCollector = new TransactionVSFromAmountCollector();
                break;
            case TO:
                amountCollector = new TransactionVSToAmountCollector();
                break;
        }
        Map<String, List<TransactionVS>> currencyMaps =  transactionList.stream().collect(groupingBy(currencyCode));
        Map<String, Map> result = new HashMap<>();
        for(String currency : currencyMaps.keySet()) {
            Map tagVSMap = currencyMaps.get(currency).stream().collect(groupingBy(tagName, amountCollector));
            result.put(currency, tagVSMap);
        }
        return result;
    }

    public static String getCurrencyCode(TransactionVS transactionVS) { return transactionVS.getCurrencyCode();}
}
