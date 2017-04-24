package org.votingsystem.currency;

import org.votingsystem.model.currency.Transaction;
import org.votingsystem.util.CurrencyCode;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BalanceUtils {

    private static final Function<Transaction, CurrencyCode> currencyCode = new Function<Transaction, CurrencyCode> () {
        @Override public CurrencyCode apply(Transaction transaction) { return transaction.getCurrencyCode(); }
    };

    public static Map<CurrencyCode, BigDecimal> getBalances(Collection<Transaction> transactionList) {
        /*Map<CurrencyCode, BigDecimal> result = new HashMap<>();
        for(Transaction transaction : transactionList) {
            if(result.containsKey(transaction.getCurrencyCode())) {
                result.put(transaction.getCurrencyCode(), result.get(transaction.getCurrencyCode()).add(transaction.getAmount()));
            } else {
                result.put(transaction.getCurrencyCode(), transaction.getAmount());
            }
        }
        return result;*/
        return transactionList.stream().collect(Collectors.groupingBy(currencyCode, new TransactionAmountCollector()));
    }

}