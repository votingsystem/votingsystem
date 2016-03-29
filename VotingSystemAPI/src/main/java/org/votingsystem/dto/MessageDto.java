package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.TypeVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto<T> {

    private Integer statusCode;
    private TypeVS operation;
    private String message;
    private String cmsMessagePEM;
    private T data;
    private String URL;

    public MessageDto () {}

    public MessageDto(Integer statusCode, String message, String URL) {
        this.statusCode = statusCode;
        this.message = message;
        this.URL = URL;
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

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    @Override public String toString() {
        return "[statusCode: " + statusCode + " - message: " + message + " - URL: " + URL +"]";
    }

    public String getURL() {
        return URL;
    }

    public MessageDto setURL(String URL) {
        this.URL = URL;
        return this;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getCmsMessagePEM() {
        return cmsMessagePEM;
    }

    @JsonIgnore
    public CMSSignedMessage getCMS() throws Exception {
        if(cmsMessagePEM != null) return CMSSignedMessage.FROM_PEM(cmsMessagePEM);
        return null;
    }

    public MessageDto setCmsMessagePEM(String cmsMessagePEM) {
        this.cmsMessagePEM = cmsMessagePEM;
        return this;
    }
}
