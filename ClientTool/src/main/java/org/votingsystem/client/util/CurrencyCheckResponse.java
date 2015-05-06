package org.votingsystem.client.util;

import org.votingsystem.model.ResponseVS;

import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyCheckResponse {

    private Integer statusCode;
    private String message;
    private Set<String> currencyOKSet = null;
    private Set<String> currencyWithErrorSet = null;

    public CurrencyCheckResponse(Integer statusCode, String message, Set<String> OKSet, Set<String> errorSet){
        this.statusCode = statusCode;
        this.message = message;
        this.currencyOKSet = OKSet;
        this.currencyWithErrorSet = errorSet;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Set<String> getCurrencyOKSet() {
        return currencyOKSet;
    }

    public Set<String> getCurrencyWithErrorSet() {
        return currencyWithErrorSet;
    }

    public String getMessage() {
        return message;
    }

    public static CurrencyCheckResponse load(ResponseVS responseVS) {
        return new CurrencyCheckResponse(responseVS.getStatusCode(), responseVS.getMessage(), null, null);
    }

}
