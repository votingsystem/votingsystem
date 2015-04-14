package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.util.TimePeriod;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalancesDto {

    private UserVSDto userVS;
    private TimePeriod timePeriod;
    private List<TransactionVSDto> transactionList;
    private List<TransactionVSDto> transactionFromList;
    private List<TransactionVSDto> transactionToList;
    private Map<String, Map> balances;
    private Map<String, Map<String, BigDecimal>> balancesFrom;
    private Map<String, Map<String, IncomesDto>> balancesTo;
    private Map<String, Map<String, BigDecimal>> balancesCash;

    public BalancesDto() {}


    public static BalancesDto TO(List<TransactionVSDto> transactionList, Map<String, Map<String, IncomesDto>> balances) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionToList(transactionList);
        dto.setBalancesTo(balances);
        return dto;
    }

    public static BalancesDto FROM(List<TransactionVSDto> transactionList,Map<String, Map<String, BigDecimal>> balances) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionFromList(transactionList);
        dto.setBalancesFrom(balances);
        return dto;
    }

    public void setTo(List<TransactionVSDto> transactionList, Map<String, Map<String, IncomesDto>> balances) {
        setTransactionToList(transactionList);
        setBalancesTo(balances);
    }

    public void setTo(BalancesDto balancesToDto) {
        setTransactionToList(balancesToDto.getTransactionToList());
        setBalancesTo(balancesToDto.getBalancesTo());
    }


    public void setFrom(List<TransactionVSDto> transactionList, Map<String, Map<String, BigDecimal>> balances) {
        setTransactionFromList(transactionList);
        setBalancesFrom(balances);
    }

    public void calculateCash() {
        setBalancesCash(filterBalanceTo(getBalancesTo()));
        for(String currency: getBalancesFrom().keySet()) {
            if(getBalancesCash().containsKey(currency)) {
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

    public static Map<String, Map<String, BigDecimal>> filterBalanceTo(Map<String, Map<String, IncomesDto>> balanceTo) {
        Map result = new HashMap<>();
        for(String currency : balanceTo.keySet()) {
            Map<String, BigDecimal> currencyMap = new HashMap<>();
            for(String tag : balanceTo.get(currency).keySet()) {
                currencyMap.put(tag, balanceTo.get(currency).get(tag).getTotal());
            }
            result.put(currency, currencyMap);
        }
        return result;
    }

    public UserVSDto getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVSDto userVS) {
        this.userVS = userVS;
    }

    public List<TransactionVSDto> getTransactionList() {
        return transactionList;
    }

    public void setTransactionList(List<TransactionVSDto> transactionList) {
        this.transactionList = transactionList;
    }

    public Map<String, Map> getBalances() {
        return balances;
    }

    public void setBalances(Map<String, Map> balances) {
        this.balances = balances;
    }

    public List<TransactionVSDto> getTransactionFromList() {
        return transactionFromList;
    }

    public void setTransactionFromList(List<TransactionVSDto> transactionFromList) {
        this.transactionFromList = transactionFromList;
    }

    public List<TransactionVSDto> getTransactionToList() {
        return transactionToList;
    }

    public void setTransactionToList(List<TransactionVSDto> transactionToList) {
        this.transactionToList = transactionToList;
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Map<String, Map<String, BigDecimal>> getBalancesCash() {
        if(balancesCash == null) calculateCash();
        return balancesCash;
    }

    public void setBalancesCash(Map<String, Map<String, BigDecimal>> balancesCash) {
        this.balancesCash = balancesCash;
    }

    public Map<String, Map<String, BigDecimal>> getBalancesFrom() {
        return balancesFrom;
    }

    public void setBalancesFrom(Map<String, Map<String, BigDecimal>> balancesFrom) {
        this.balancesFrom = balancesFrom;
    }

    public Map<String, Map<String, IncomesDto>> getBalancesTo() {
        return balancesTo;
    }

    public void setBalancesTo(Map<String, Map<String, IncomesDto>> balancesTo) {
        this.balancesTo = balancesTo;
    }
}
