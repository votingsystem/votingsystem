package org.votingsystem.util.currency;


import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.model.currency.Currency;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapUtils {

    public static Map<String, IncomesDto> getTagMapForIncomes(String tag, BigDecimal total, BigDecimal timeLimited) {
        Map<String, IncomesDto> result = new HashMap<>();
        result.put(tag, new IncomesDto(total, timeLimited));
        return result;
    }

    public static Map<String, BigDecimal> getTagMapForExpenses(String tag, BigDecimal amount) {
        Map<String, BigDecimal> result = new HashMap<>();
        result.put(tag, amount);
        return result;
    }

    public static Map<String, BigDecimal> getCurrencyMap(Collection<Currency> currencyList) {
        Map<String, BigDecimal> currencyMap = new HashMap<String, BigDecimal>();
        for(Currency currency : currencyList){
            if(currencyMap.containsKey(currency.getCurrencyCode())) currencyMap.put(currency.getCurrencyCode(),
                    currencyMap.get(currency.getCurrencyCode()).add(currency.getAmount()));
            else currencyMap.put(currency.getCurrencyCode(), currency.getAmount());
        }
        return currencyMap;
    }

}
