package org.votingsystem.test.util;

import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.currency.Transaction;

import java.math.BigDecimal;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionCounter {

    private BigDecimal amount = BigDecimal.ZERO;
    private Integer numTransactions = 0;
    private Transaction.Type type;

    public TransactionCounter(TransactionDto transaction) {
        this.type = transaction.getType();
        this.amount = transaction.getAmount();
        this.numTransactions = 1;
    }

    public void addTransaction(BigDecimal amount) {
        numTransactions++;
        this.amount = this.amount.add(amount);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getNumTransactions() {
        return numTransactions;
    }

    public  Transaction.Type getType() {
        return type;
    }


    @Override public String toString() {
        return type.toString() + " - '" + numTransactions + "' transactions - total: " + amount.toString();
    }
}
