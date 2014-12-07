package org.votingsystem.util;

import org.votingsystem.cooin.model.TransactionVS;

import java.math.BigDecimal;
import java.util.Collections;
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
public class TransactionVSAmountCollector implements Collector<TransactionVS, BigDecimal[], BigDecimal> {

    @Override public Supplier<BigDecimal[]> supplier() {
        return () -> new BigDecimal[1];
    }

    @Override public BiConsumer<BigDecimal[], TransactionVS> accumulator() {
        return (a, t) -> { a[0].add(t.getAmount()); };
    }

    //to join two accumulators together into one. It is used when collector is executed in parallel
    @Override public BinaryOperator<BigDecimal[]> combiner() {
        return (a, b) -> { a[0].add(b[0]); return a; };
    }

    @Override public Function<BigDecimal[], BigDecimal> finisher() {
        return a -> a[0];
    }


    @Override public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
