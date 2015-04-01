package org.votingsystem.web.currency.util;

import org.votingsystem.model.currency.TransactionVS;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
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
        /*problems with spring-loaded
        return () -> {
            Map<String, BigDecimal> result = new HashMap<String, BigDecimal>();
            result.put("total", BigDecimal.ZERO);
            result.put("timeLimited", BigDecimal.ZERO);
            return new Map[]{result};
        };*/
        return new Supplier<Map<String, BigDecimal>[]>() {
            @Override public Map<String, BigDecimal>[] get() {
                Map<String, BigDecimal> result = new HashMap<String, BigDecimal>();
                result.put("total", BigDecimal.ZERO);
                result.put("timeLimited", BigDecimal.ZERO);
                return new Map[]{result};
            }
        };
    }

    @Override public BiConsumer<Map<String, BigDecimal>[], TransactionVS> accumulator() {
        /*problems with spring-loaded
        return (a, transaction) -> {
            a[0].put("total", a[0].get("total").add(transaction.getAmount()));
            if(transaction.getValidTo() != null) {
                a[0].put("timeLimited", a[0].get("timeLimited").add(transaction.getAmount()));
            }
        };*/
        return new BiConsumer<Map<String, BigDecimal>[], TransactionVS>() {
            @Override public void accept(Map<String, BigDecimal>[] maps, TransactionVS transactionVS) {
                maps[0].put("total", maps[0].get("total").add(transactionVS.getAmount()));
                if(transactionVS.getValidTo() != null) {
                    maps[0].put("timeLimited", maps[0].get("timeLimited").add(transactionVS.getAmount()));
                }
            }
        };
    }

    //to join two accumulators together into one. It is used when collector is executed in parallel
    @Override public BinaryOperator<Map<String, BigDecimal>[]> combiner() {
        /*problems with spring-loaded
        return (a, b) -> {
            a[0].put("total", a[0].get("total").add(b[0].get("total")));
            a[0].put("timeLimited", a[0].get("timeLimited").add(b[0].get("timeLimited")));
            return a; };*/
        return new  BinaryOperator<Map<String, BigDecimal>[]>() {
            @Override public Map<String, BigDecimal>[] apply(Map<String, BigDecimal>[] maps, Map<String, BigDecimal>[] maps2) {
                maps[0].put("total", maps[0].get("total").add(maps2[0].get("total")));
                maps[0].put("timeLimited", maps[0].get("timeLimited").add(maps2[0].get("timeLimited")));
                return maps;
            }
        };
    }

    @Override public Function<Map<String, BigDecimal>[], Map<String, BigDecimal>> finisher() {
        //return a -> a[0];//problems with spring-loaded
        return new  Function<Map<String, BigDecimal>[], Map<String, BigDecimal>>(){
            @Override public Map<String, BigDecimal> apply(Map<String, BigDecimal>[] maps) {
                return maps[0];
            }
        };
    }


    @Override public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
