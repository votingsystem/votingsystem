package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.JSON;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto<T> {

    private Integer statusCode;
    private String operation;
    private String message_type;
    private String message;
    private String callerCallback;
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


    public static <T> MessageDto<T> WEB_SOCKET(int statusCode, T data) {
        MessageDto dto = new MessageDto(statusCode, null);
        dto.setOperation("vs-websocket-message");
        dto.setData(data);
        return dto;
    }

    public static MessageDto SIGNAL(int statusCode, String operation) {
        MessageDto dto = new MessageDto(statusCode, null);
        dto.setOperation(operation);
        return dto;
    }

    public static MessageDto NEW_TAB(String url) {
        MessageDto dto = new MessageDto();
        dto.setOperation("url_tab");
        dto.setMessage_type("message_to_webextension");
        dto.setURL(url);
        return dto;
    }

    public static MessageDto OPERATION_CALLBACK(int statusCode, String message, String callerCallback) {
        MessageDto dto = new MessageDto(statusCode, message);
        dto.setCallerCallback(callerCallback);
        return dto;
    }

    public static MessageDto DIALOG_CLOSE() {
        MessageDto dto = new MessageDto();
        dto.setOperation("dialog_closed");
        dto.setMessage_type("message_to_webextension");
        return dto;
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

    public void setURL(String URL) {
        this.URL = URL;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getMessage_type() {
        return message_type;
    }

    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    public String getCallerCallback() {
        return callerCallback;
    }

    public void setCallerCallback(String callerCallback) {
        this.callerCallback = callerCallback;
    }
}
