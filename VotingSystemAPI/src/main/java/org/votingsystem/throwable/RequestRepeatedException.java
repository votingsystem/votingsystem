package org.votingsystem.throwable;

import org.votingsystem.model.ResponseVS;

import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RequestRepeatedException extends ExceptionVS {

    private String message;
    private String url;

    public RequestRepeatedException(String url) {
        super(url);
        this.message = url;
        this.url = url;
    }

    public RequestRepeatedException(String message, String url) {
        super(message);
        this.message = message;
        this.url = url;
    }

    public String getMsg() {
        return message;
    }

    public String getUrl() {
        return url;
    }

    public Map toMap() {
        Map result = new HashMap<>();
        result.put("statusCode", ResponseVS.SC_ERROR_REQUEST_REPEATED);
        result.put("message", message);
        result.put("URL", url);
        return result;
    }
}
