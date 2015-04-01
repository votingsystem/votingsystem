package org.votingsystem.test.util;

import org.votingsystem.model.currency.TransactionVS;

import java.math.BigDecimal;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSCounter {
    BigDecimal amount = BigDecimal.ZERO;
    Integer numTransactions = 0;
    TransactionVS.Type type;

    public TransactionVSCounter(TransactionVS transactionVS) {
        this.type = transactionVS.getType();
        this.amount = transactionVS.getAmount();
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

    public  TransactionVS.Type getType() {
        return type;
    }


    @Override public String toString() {
        return type.toString() + " - '" + numTransactions + "' transactions - total: " + amount.toString();
    }
}
