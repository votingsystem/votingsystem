package org.votingsystem.cooin.util;

import org.votingsystem.cooin.model.TransactionVS;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collector;
import static java.util.stream.Collectors.groupingBy;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BalanceUtils {

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
        Map result = transactionList.stream().collect(groupingBy(TransactionVS::getCurrencyCode,
                groupingBy(TransactionVS::getTagName, amountCollector)));
        return result;
    }

}
