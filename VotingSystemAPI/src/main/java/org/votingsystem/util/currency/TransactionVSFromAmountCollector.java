package org.votingsystem.util.currency;

import org.votingsystem.model.currency.TransactionVS;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSFromAmountCollector implements Collector<TransactionVS, BigDecimal[], BigDecimal> {

    @Override public Supplier<BigDecimal[]> supplier() {
        //return () -> new BigDecimal[]{BigDecimal.ZERO};//problems with spring-loaded
        return new Supplier<BigDecimal[]>() {
            @Override public BigDecimal[] get() {
                return new BigDecimal[]{BigDecimal.ZERO};
            }
        };
    }

    @Override public BiConsumer<BigDecimal[], TransactionVS> accumulator() {
        //return (a, t) -> {a[0] = a[0].add(t.getAmount());};//problems with spring-loaded
        return new BiConsumer<BigDecimal[], TransactionVS>() {
            @Override public void accept(BigDecimal[] bigDecimals, TransactionVS transactionVS) {
                bigDecimals[0] = bigDecimals[0].add(transactionVS.getAmount());
            }
        };
    }

    //to join two accumulators together into one. It is used when collector is executed in parallel
    @Override public BinaryOperator<BigDecimal[]> combiner() {
        //return (a, b) -> { a[0] = a[0].add(b[0]); return a; };//problems with spring-loaded
        return new BinaryOperator<BigDecimal[]>(){
            @Override public BigDecimal[] apply(BigDecimal[] bigDecimals, BigDecimal[] bigDecimals2) {
                bigDecimals[0] = bigDecimals[0].add(bigDecimals2[0]);
                return bigDecimals;
            }
        };
    }

    @Override public Function<BigDecimal[], BigDecimal> finisher() {
        //return a -> a[0];//problems with spring-loaded
        return new Function<BigDecimal[], BigDecimal>(){
            @Override public BigDecimal apply(BigDecimal[] bigDecimals) {
                return bigDecimals[0];
            }
        };
    }

    @Override public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

}
