package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.TypeVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto<T> {

    private TypeVS operation;
    private Integer statusCode;
    private String message;
    private String cmsMessagePEM;
    private T data;
    private String URL;
    private String deviceId;
    private String httpSessionId;
    private String UUID;

    public MessageDto() {}

    public MessageDto(Integer statusCode, String message, String URL) {
        this.statusCode = statusCode;
        this.message = message;
        this.setURL(URL);
    }

    public MessageDto(Integer statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public static MessageDto OK(String message) {
        return new MessageDto(ResponseVS.SC_OK, message, null);
    }

    public static MessageDto ERROR(String message) {
        return new MessageDto(ResponseVS.SC_ERROR, message, null);
    }

    public static MessageDto BAD_REQUEST(String message) {
        return new MessageDto(ResponseVS.SC_ERROR_REQUEST, message, null);
    }

    public static MessageDto OK(String message, String URL) {
        return new MessageDto(ResponseVS.SC_OK, message, URL);
    }

    public static MessageDto REQUEST_REPEATED(String message, String URL) {
        return new MessageDto(ResponseVS.SC_ERROR_REQUEST_REPEATED, message, URL);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }

    public String getUUID() {
        return UUID;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getCmsMessagePEM() {
        return cmsMessagePEM;
    }

    public void setCmsMessagePEM(String cmsMessagePEM) {
        this.cmsMessagePEM = cmsMessagePEM;
    }

    public String getURL() {
        return URL;
    }

    public MessageDto<T> setURL(String URL) {
        this.URL = URL;
        return this;
    }

    public void setData(T data) {
        this.data = data;
    }
}
