package org.votingsystem.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.servlet.http.HttpServletRequest;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class HttpRequest {

    private static final String LOCALHOST="127.0.0.1";

    public enum Method {
        @JsonProperty("GET")
        GET,
        @JsonProperty("POST")
        POST,
        @JsonProperty("POST_FORM")
        POST_FORM;
    }


    public static String getContentType (final HttpServletRequest req, boolean withAcceptHeader) {
        String contentType = "";
        if(req.getContentType() != null)
            contentType = req.getContentType().toLowerCase();
        if(!withAcceptHeader)
            return contentType;
        String acceptHeader = "";
        if(req.getHeader("Accept") != null)
            acceptHeader = req.getHeader("Accept").toLowerCase();
        return contentType + ";" + acceptHeader;
    }

    private boolean isRequestFromLocalHost(HttpServletRequest req){
        if(!LOCALHOST.equals(req.getRemoteAddr()))
            return false;
        else
            return true;
    }

}
