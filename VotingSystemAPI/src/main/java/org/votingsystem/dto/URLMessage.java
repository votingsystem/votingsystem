package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class URLMessage {

    private Integer statusCode;
    private String message;
    @JsonProperty("UUID")
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
