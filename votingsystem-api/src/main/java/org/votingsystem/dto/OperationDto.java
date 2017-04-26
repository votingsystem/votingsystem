package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationDto<T> {

    @JacksonXmlProperty(localName = "Operation")
    private OperationTypeDto operation;
    private Integer statusCode;
    private String message;
    private String deviceId;
    private String httpSessionId;
    private String base64Data;
    private String sessionUUID;
    private String UUID;

    public OperationDto() {}

    public OperationDto(OperationTypeDto operation) {
        this.operation = operation;
    }

    public OperationDto(Integer statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
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

    public OperationTypeDto getOperation() {
        return operation;
    }

    public String getMessage() {
        return message;
    }

    public OperationDto setMessage(String message) {
        this.message = message;
        return this;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public OperationDto setBase64Data(String base64Data) {
        this.base64Data = base64Data;
        return this;
    }

    public OperationDto setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public OperationDto setHttpSessionId(String httpSessionId) {
        this.httpSessionId = httpSessionId;
        return this;
    }

    public OperationDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public OperationDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

    public String getSessionUUID() {
        return sessionUUID;
    }

    public OperationDto setSessionUUID(String sessionUUID) {
        this.sessionUUID = sessionUUID;
        return this;
    }

}