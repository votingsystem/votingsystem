package org.votingsystem.currency;

import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.model.currency.Transaction;

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
public class TransactionToAmountCollector implements Collector<Transaction, IncomesDto[], IncomesDto> {

    @Override public Supplier<IncomesDto[]> supplier() {
        /*problems with spring-loaded
        return () -> {
            return new IncomesDto[]{IncomesDto.ZERO};
        };*/
        return new Supplier<IncomesDto[]>() {
            @Override public IncomesDto[] get() {
                return new IncomesDto[]{IncomesDto.ZERO()};
            }
        };
    }

    @Override public BiConsumer<IncomesDto[], Transaction> accumulator() {
        /*problems with spring-loaded
        return (a, transaction) -> {
            a[0].add(transaction);
        };*/
        return new BiConsumer<IncomesDto[], Transaction>() {
            @Override public void accept(IncomesDto[] incomes, Transaction transaction) {
                incomes[0].add(transaction);
            }
        };
    }

    //to join two accumulators together into one. It is used when collector is executed in parallel
    @Override public BinaryOperator<IncomesDto[]> combiner() {
        /*problems with spring-loaded
        return (a, b) -> {
            a[0].setTotal(a[0].getTotal().add(b[0].getTotal()));
            a[0].setTimeLimited(a[0].getTimeLimited().add(b[0].getTimeLimited()));
            return a; };*/
        return new  BinaryOperator<IncomesDto[]>() {
            @Override public IncomesDto[] apply(IncomesDto[] incom1, IncomesDto[] incom2) {
                incom1[0].addTotal(incom2[0].getTotal());
                incom1[0].addTimeLimited(incom2[0].getTimeLimited());
                return incom1;
            }
        };
    }

    @Override public Function<IncomesDto[], IncomesDto> finisher() {
        //return a -> a[0];//problems with spring-loaded
        return new  Function<IncomesDto[], IncomesDto>(){
            @Override public IncomesDto apply(IncomesDto[] incomes) {
                return incomes[0];
            }
        };
    }


    @Override public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
