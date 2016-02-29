package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.TypeVS;

import java.io.Serializable;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QRMessageDto<T> implements Serializable {

    public static final String WEB_SOCKET_SESSION_KEY = "ws_sid";

    @JsonIgnore private TypeVS typeVS;
    @JsonIgnore private T data;
    @JsonIgnore private String origingHashCertVS;
    @JsonIgnore private Currency currency ;
    private TypeVS operation;
    private Long deviceId;
    private String sessionId;
    private Date dateCreated;
    private String hashCertVS;
    private String url;
    private String UUID;

    public QRMessageDto() {}

    public QRMessageDto(String sessionId, TypeVS typeVS) {
        this.sessionId = sessionId;
        this.typeVS = typeVS;
        dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0,3);
    }

    public QRMessageDto(DeviceVSDto deviceVSDto, TypeVS typeVS) {
        this.typeVS = typeVS;
        this.deviceId = deviceVSDto.getId();
        dateCreated = new Date();
        this.UUID = java.util.UUID.randomUUID().toString().substring(0,3);
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public QRMessageDto setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getOrigingHashCertVS() {
        return origingHashCertVS;
    }

    public void setOrigingHashCertVS(String origingHashCertVS) {
        this.origingHashCertVS = origingHashCertVS;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getSessionId() {
        return sessionId;
    }

    public QRMessageDto setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public QRMessageDto setOperation(TypeVS operation) {
        this.operation = operation;
        return this;
    }
}