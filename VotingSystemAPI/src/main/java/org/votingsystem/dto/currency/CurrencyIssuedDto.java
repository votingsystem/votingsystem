package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyIssuedDto {

    private String message;
    private List<String> issuedCurrency;

    public CurrencyIssuedDto () {}

    public CurrencyIssuedDto (List<String> issuedCurrency, String message) {
        this.message = message;
        this.issuedCurrency = issuedCurrency;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getIssuedCurrency() {
        return issuedCurrency;
    }

    public void setIssuedCurrency(List<String> issuedCurrency) {
        this.issuedCurrency = issuedCurrency;
    }
}
