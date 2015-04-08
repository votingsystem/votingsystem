package org.votingsystem.dto;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class URLMessage {

    private Integer statusCode;
    private String message;
    private String URL;

    public URLMessage(Integer statusCode, String message, String URL) {
        this.statusCode = statusCode;
        this.message = message;
        this.URL = URL;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public String getURL() {
        return URL;
    }
}
