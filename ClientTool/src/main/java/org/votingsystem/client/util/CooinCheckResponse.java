package org.votingsystem.client.util;

import org.votingsystem.model.ResponseVS;

import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinCheckResponse {

    private Integer statusCode;
    private String message;
    private Set<String> cooinOKSet = null;
    private Set<String> cooinWithErrorSet = null;

    public CooinCheckResponse(Integer statusCode, String message, Set<String> OKSet, Set<String> errorSet){
        this.statusCode = statusCode;
        this.message = message;
        this.cooinOKSet = OKSet;
        this.cooinWithErrorSet = errorSet;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Set<String> getCooinOKSet() {
        return cooinOKSet;
    }

    public Set<String> getCooinWithErrorSet() {
        return cooinWithErrorSet;
    }

    public String getMessage() {
        return message;
    }

    public static CooinCheckResponse load(ResponseVS responseVS) {
        return new CooinCheckResponse(responseVS.getStatusCode(), responseVS.getMessage(), null, null);
    }

}
