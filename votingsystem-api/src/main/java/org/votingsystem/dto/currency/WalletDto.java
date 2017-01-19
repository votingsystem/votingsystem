package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletDto {

    private Collection<CurrencyDto> currencyCollection;

    public WalletDto () {}

    public WalletDto (Collection<CurrencyDto> currencyCollection) {
        this.currencyCollection = currencyCollection;

    }

    public Collection<CurrencyDto> getCurrencyCollection() {
        return currencyCollection;
    }

    public void setCurrencyCollection(Collection<CurrencyDto> currencyCollection) {
        this.currencyCollection = currencyCollection;
    }

}
