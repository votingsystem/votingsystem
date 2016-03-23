package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserDto;
import org.votingsystem.model.CurrencyCode;
import org.votingsystem.model.TagVS;
import org.votingsystem.util.Interval;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalancesDto {

    private UserDto user;
    private Interval timePeriod;
    private List<TransactionDto> transactionList;
    private List<TransactionDto> transactionFromList;
    private List<TransactionDto> transactionToList;
    private Map<CurrencyCode, Map> balances;
    private Map<CurrencyCode, Map<String, BigDecimal>> balancesFrom = new HashMap<>();
    private Map<CurrencyCode, Map<String, IncomesDto>> balancesTo = new HashMap<>();
    private Map<CurrencyCode, Map<String, BigDecimal>> balancesCash = new HashMap<>();

    public BalancesDto() {}


    public static BalancesDto TO(List<TransactionDto> transactionList, Map<CurrencyCode, Map<String, IncomesDto>> balances) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionToList(transactionList);
        dto.setBalancesTo(balances);
        return dto;
    }

    public static BalancesDto FROM(List<TransactionDto> transactionList, Map<CurrencyCode, Map<String, BigDecimal>> balances) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionFromList(transactionList);
        dto.setBalancesFrom(balances);
        return dto;
    }

    public void setTo(List<TransactionDto> transactionList, Map<CurrencyCode, Map<String, IncomesDto>> balances) {
        setTransactionToList(transactionList);
        setBalancesTo(balances);
    }

    public void setTo(BalancesDto balancesToDto) {
        setTransactionToList(balancesToDto.getTransactionToList());
        setBalancesTo(balancesToDto.getBalancesTo());
    }


    public void setFrom(List<TransactionDto> transactionList, Map<CurrencyCode, Map<String, BigDecimal>> balances) {
        setTransactionFromList(transactionList);
        setBalancesFrom(balances);
    }

    public void calculateCash() {
        setBalancesCash(filterBalanceTo(balancesTo));
        for(CurrencyCode currency: balancesFrom.keySet()) {
            if(balancesCash.containsKey(currency)) {
                for(String tag : getBalancesFrom().get(currency).keySet()) {
                    if(getBalancesCash().get(currency).containsKey(tag)) {
                        BigDecimal newAmount = getBalancesCash().get(currency).get(tag).subtract(getBalancesFrom().get(currency).get(tag));
                        if(newAmount.compareTo(BigDecimal.ZERO) < 0) {
                            getBalancesCash().get(currency).put(TagVS.WILDTAG, getBalancesCash().get(currency).
                                    get(TagVS.WILDTAG).add(newAmount));
                            getBalancesCash().get(currency).put(tag, BigDecimal.ZERO);
                        } else  getBalancesCash().get(currency).put(tag, newAmount);
                    } else {
                        getBalancesCash().get(currency).put(TagVS.WILDTAG,  getBalancesCash().get(currency).get(TagVS.WILDTAG)
                                .subtract(getBalancesFrom().get(currency).get(tag)));
                    }
                }
            } else {
                Map<String, BigDecimal> tagData = new HashMap<String, BigDecimal>(getBalancesFrom().get(currency));
                for(String tag: tagData.keySet()) {
                    tagData.put(tag, tagData.get(tag).negate());
                }
            }
        }
    }

    public static Map<CurrencyCode, Map<String, BigDecimal>> filterBalanceTo(Map<CurrencyCode, Map<String, IncomesDto>> balanceTo) {
        Map<CurrencyCode, Map<String, BigDecimal>> result = new HashMap<>();
        for(CurrencyCode currency : balanceTo.keySet()) {
            Map<String, BigDecimal> currencyMap = new HashMap<>();
            for(String tag : balanceTo.get(currency).keySet()) {
                currencyMap.put(tag, balanceTo.get(currency).get(tag).getTotal());
            }
            result.put(currency, currencyMap);
        }
        return result;
    }

    public BigDecimal getTimeLimitedNotExpended(CurrencyCode currencyCode, String tagName) {
        BigDecimal result = BigDecimal.ZERO;
        if(balancesTo.containsKey(currencyCode) && balancesTo.get(currencyCode).containsKey(tagName)) {
            result = balancesTo.get(currencyCode).get(tagName).getTimeLimited();
        }
        if(balancesFrom.containsKey(currencyCode) && balancesFrom.get(currencyCode).containsKey(tagName)) {
            result = result.subtract(balancesFrom.get(currencyCode).get(tagName));
        }
        if(result.compareTo(BigDecimal.ZERO) > 0) return result;
        else return BigDecimal.ZERO;
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

    public Map<CurrencyCode, Map> getBalances() {
        return balances;
    }

    public void setBalances(Map<CurrencyCode, Map> balances) {
        this.balances = balances;
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

    public void setTimePeriod(Interval timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Map<CurrencyCode, Map<String, BigDecimal>> getBalancesCash() {
        if(balancesCash == null) calculateCash();
        return balancesCash;
    }

    public void setBalancesCash(Map<CurrencyCode, Map<String, BigDecimal>> balancesCash) {
        this.balancesCash = balancesCash;
    }

    public Map<CurrencyCode, Map<String, BigDecimal>> getBalancesFrom() {
        return balancesFrom;
    }

    public void setBalancesFrom(Map<CurrencyCode, Map<String, BigDecimal>> balancesFrom) {
        this.balancesFrom = balancesFrom;
    }

    public Map<CurrencyCode, Map<String, IncomesDto>> getBalancesTo() {
        return balancesTo;
    }

    public void setBalancesTo(Map<CurrencyCode, Map<String, IncomesDto>> balancesTo) {
        this.balancesTo = balancesTo;
    }
}
