package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.dto.UserDto;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.Interval;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import CurrencyCode;
//import Tag;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BalancesDto {

    private UserDto user;
    private Interval timePeriod;
    private List<TransactionDto> transactionList;
    private List<TransactionDto> transactionFromList;
    private List<TransactionDto> transactionToList;

    private Map<CurrencyCode, BigDecimal> balancesFrom = new HashMap<>();
    private Map<CurrencyCode, BigDecimal> balancesTo = new HashMap<>();
    private Map<CurrencyCode, BigDecimal> balancesCash = new HashMap<>();

    public BalancesDto() {}


    public static BalancesDto TO(List<TransactionDto> transactionList, Map<CurrencyCode, BigDecimal> balancesTo) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionToList(transactionList);
        dto.setBalancesTo(balancesTo);
        return dto;
    }

    public static BalancesDto FROM(List<TransactionDto> transactionList, Map<CurrencyCode, BigDecimal> balancesFrom) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionFromList(transactionList);
        dto.setBalancesFrom(balancesFrom);
        return dto;
    }

    public void setTo(List<TransactionDto> transactionList,Map<CurrencyCode, BigDecimal> balancesTo) {
        setTransactionToList(transactionList);
        setBalancesTo(balancesTo);
    }

    public void setTo(BalancesDto balancesTo) {
        setTransactionToList(balancesTo.getTransactionToList());
        setBalancesTo(balancesTo.getBalancesTo());
    }


    public void setFrom(List<TransactionDto> transactionList, Map<CurrencyCode, BigDecimal> balancesFrom) {
        setTransactionFromList(transactionList);
        setBalancesFrom(balancesFrom);
    }

    public void calculateCash() {
        balancesCash = new HashMap<>(balancesTo);
        for(CurrencyCode currencyCode: balancesFrom.keySet()) {
            if(balancesCash.containsKey(currencyCode)) {
                balancesCash.put(currencyCode, balancesCash.get(currencyCode).subtract(
                        balancesFrom.get(currencyCode)));
            } else {
                balancesCash.put(currencyCode, balancesFrom.get(currencyCode).negate());
            }
        }
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public List<TransactionDto> getTransactionList() {
        return transactionList;
    }

    public void setTransactionList(List<TransactionDto> transactionList) {
        this.transactionList = transactionList;
    }

    public List<TransactionDto> getTransactionFromList() {
        return transactionFromList;
    }

    public void setTransactionFromList(List<TransactionDto> transactionFromList) {
        this.transactionFromList = transactionFromList;
    }

    public List<TransactionDto> getTransactionToList() {
        return transactionToList;
    }

    public void setTransactionToList(List<TransactionDto> transactionToList) {
        this.transactionToList = transactionToList;
    }

    public Interval getTimePeriod() {
        return timePeriod;
    }

    public BalancesDto setTimePeriod(Interval timePeriod) {
        this.timePeriod = timePeriod;
        return this;
    }

    public Map<CurrencyCode, BigDecimal> getBalancesCash() {
        if(balancesCash == null) calculateCash();
        return balancesCash;
    }

    public Map<CurrencyCode, BigDecimal> getBalancesFrom() {
        return balancesFrom;
    }

    public void setBalancesFrom(Map<CurrencyCode, BigDecimal> balancesFrom) {
        this.balancesFrom = balancesFrom;
    }

    public Map<CurrencyCode, BigDecimal> getBalancesTo() {
        return balancesTo;
    }

    public void setBalancesTo(Map<CurrencyCode, BigDecimal> balancesTo) {
        this.balancesTo = balancesTo;
    }
}
