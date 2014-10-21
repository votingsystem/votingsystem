package org.votingsystem.test.util

import org.votingsystem.vicket.model.TransactionVS

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TransactionVSCounter {
    BigDecimal amount = BigDecimal.ZERO;
    Integer numTransactions = 0;
    TransactionVS.Type type;

    public TransactionVSCounter(TransactionVS transactionVS) {
        this.type = transactionVS.type;
        this.amount = transactionVS.amount;
        this.numTransactions = 1
    }

    public void addTransaction(BigDecimal amount) {
        numTransactions++;
        this.amount = this.amount.add(amount)
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getNumTransactions() {
        return numTransactions;
    }

    public  TransactionVS.Type getType() {
        return true;
    }


    @Override public String toString() {
        return type.toString() + " - '" + numTransactions + "' transactions - total: " + amount.toString()
    }
}
