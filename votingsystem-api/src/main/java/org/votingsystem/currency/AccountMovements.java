package org.votingsystem.currency;


import org.votingsystem.model.currency.CurrencyAccount;

import java.math.BigDecimal;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AccountMovements {

    private boolean transactionApproved;
    private String message;
    private Map<CurrencyAccount, BigDecimal> accountFromMovements;

    public AccountMovements() {}

    public AccountMovements(boolean transactionApproved, Map<CurrencyAccount, BigDecimal> accountFromMovements) {
        this.accountFromMovements = accountFromMovements;
        this.transactionApproved = transactionApproved;
    }

    public AccountMovements(boolean transactionApproved, String message) {
        this.transactionApproved = transactionApproved;
        this.message = message;
    }

    public boolean isTransactionApproved() {
        return transactionApproved;
    }

    public void setTransactionApproved(boolean transactionApproved) {
        this.transactionApproved = transactionApproved;
    }

    public String getMessage() {
        return message;
    }

    public AccountMovements setMessage(String message) {
        this.message = message;
        return this;
    }

    public Map<CurrencyAccount, BigDecimal> getAccountFromMovements() {
        return accountFromMovements;
    }

    public void setAccountFromMovements(Map<CurrencyAccount, BigDecimal> accountFromMovements) {
        this.accountFromMovements = accountFromMovements;
    }

}