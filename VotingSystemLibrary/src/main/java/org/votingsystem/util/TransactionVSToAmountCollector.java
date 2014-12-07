package org.votingsystem.util;

import org.votingsystem.cooin.model.TransactionVS;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSToAmountCollector implements Collector<TransactionVS, Map<String, BigDecimal>[], Map<String, BigDecimal>> {

    @Override public Supplier<Map<String, BigDecimal>[]> supplier() {
        return () -> new Map[1];
    }

    @Override public BiConsumer<Map<String, BigDecimal>[], TransactionVS> accumulator() {
        return (a, transaction) -> {
            a[0].get("total").add(transaction.getAmount());
            if(transaction.getValidTo() != null) {
                a[0].get("timeLimited").add(transaction.getAmount());
            }
        };
    }

    //to join two accumulators together into one. It is used when collector is executed in parallel
    @Override public BinaryOperator<Map<String, BigDecimal>[]> combiner() {
        return (a, b) -> {
            a[0].get("total").add(b[0].get("total"));
            a[0].get("timeLimited").add(b[0].get("timeLimited"));
            return a; };
    }

    @Override public Function<Map<String, BigDecimal>[], Map<String, BigDecimal>> finisher() {
        return a -> a[0];
    }


    @Override public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
