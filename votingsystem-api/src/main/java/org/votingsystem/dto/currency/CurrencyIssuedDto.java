package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.CurrencyCode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyIssuedDto {

    private Map<CurrencyCode, BigDecimal> active = new HashMap<>();
    private Map<CurrencyCode, BigDecimal> expended = new HashMap<>();
    private Map<CurrencyCode, BigDecimal> lapsed = new HashMap<>();


    public CurrencyIssuedDto() {}

    public CurrencyIssuedDto(Map<CurrencyCode, BigDecimal> active, Map<CurrencyCode, BigDecimal> expended,
                             Map<CurrencyCode, BigDecimal> lapsed) {
        this.active = active;
        this.expended = expended;
        this.lapsed = lapsed;
    }

    public Map<CurrencyCode, BigDecimal> getActive() {
        return active;
    }

    public Map<CurrencyCode, BigDecimal> getExpended() {
        return expended;
    }

    public Map<CurrencyCode, BigDecimal> getLapsed() {
        return lapsed;
    }

    public void addCurrency(BigDecimal amount, CurrencyCode currencyCode, Currency.State state) {
        switch (state) {
            case EXPENDED:
                addCurrencyToMap(amount, currencyCode, expended);
                break;
            case OK:
                addCurrencyToMap(amount, currencyCode, active);
                break;
            case LAPSED:
                addCurrencyToMap(amount, currencyCode, lapsed);
                break;
        }
    }

    private void addCurrencyToMap(BigDecimal amount, CurrencyCode currencyCode, Map<CurrencyCode, BigDecimal> map) {
        if(map.containsKey(currencyCode)) {
            map.put(currencyCode, map.get(currencyCode).add(amount));
        } else map.put(currencyCode, amount);

    }
}
