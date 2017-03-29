package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.throwable.XAdESValidationException;
import org.votingsystem.throwable.XMLValidationException;

import javax.persistence.PersistenceException;
import javax.servlet.ServletException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;

/**
 * Application error codes
 *
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum AppCode {

    //not found exception
    @JsonProperty("vs_0400")
    vs_0400(400),
    //not allowed exception
    @JsonProperty("vs_0405")
    vs_0405(405),
    //bad request - generic error
    @JsonProperty("vs_0410")
    vs_0410(410),
    //bad request - XML validation generic error
    @JsonProperty("vs_0420")
    vs_0420(420),
    //bad request - XML signature error
    @JsonProperty("vs_0430")
    vs_0430(430),
    //system exception
    @JsonProperty("vs_0500")
    vs_0500(500),
    //database connection exception
    @JsonProperty("vs_0510")
    vs_0510(510);

    private int statusCode;

    AppCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public static AppCode getByException(Exception ex) {
        if(ex instanceof PersistenceException) {
            return vs_0510;
        } else if(ex instanceof XMLValidationException) {
            return vs_0420;
        } else if(ex instanceof ServletException) {
            return vs_0500;
        } else if(ex instanceof NotAllowedException) {
            return vs_0405;
        } else if(ex instanceof NotFoundException) {
            return vs_0400;
        } else if(ex instanceof XAdESValidationException) {
            return vs_0430;
        } else {
            return vs_0410;
        }
    }

    public int getStatusCode() {
        return statusCode;
    }
}
