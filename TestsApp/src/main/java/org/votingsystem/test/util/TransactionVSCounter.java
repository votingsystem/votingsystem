package org.votingsystem.test.util;

import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.currency.TransactionVS;

import java.math.BigDecimal;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSCounter {

    private BigDecimal amount = BigDecimal.ZERO;
    private Integer numTransactions = 0;
    private TransactionVS.Type type;

    public TransactionVSCounter(TransactionVSDto transactionVS) {
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
