package org.votingsystem.currency;

import org.votingsystem.model.currency.Currency;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MapUtils {

    public static Map<String, BigDecimal> getCurrencyMap(Collection<Currency> currencyList) {
        Map<String, BigDecimal> currencyMap = new HashMap<String, BigDecimal>();
        for(Currency currency : currencyList){
            if(currencyMap.containsKey(currency.getCurrencyCode())) currencyMap.put(currency.getCurrencyCode().toString(),
                    currencyMap.get(currency.getCurrencyCode()).add(currency.getAmount()));
            else currencyMap.put(currency.getCurrencyCode().toString(), currency.getAmount());
        }
        return currencyMap;
    }

}
