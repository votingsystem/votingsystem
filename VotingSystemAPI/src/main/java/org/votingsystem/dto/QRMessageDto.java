package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.TypeVS;

import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QRMessageDto<T> implements Serializable {

    private static Logger log = Logger.getLogger(QRMessageDto.class.getName());


    public static final int INIT_REMOTE_SIGNED_BROWSER_SESSION = 0;
    public static final int QR_MESSAGE_INFO                    = 1;

    public static final String WEB_SOCKET_SESSION_KEY = "ws_sid";
    public static final String DEVICE_ID_KEY          = "d_id";
    public static final String OPERATION_KEY          = "op";
    public static final String OPERATION_ID_KEY       = "op_id";

    @JsonIgnore private TypeVS typeVS;
    @JsonIgnore private T data;
    @JsonIgnore private String origingHashCertVS;
    @JsonIgnore private Currency currency ;
    private TypeVS operation;
    private Long deviceId;
    private String sessionId;
    private String operationId;
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

    public static QRMessageDto FROM_QR_CODE(String msg) {
        QRMessageDto qrMessageDto = new QRMessageDto();
        if (msg.contains(DEVICE_ID_KEY + "="))
            qrMessageDto.setDeviceId(Long.valueOf(msg.split(DEVICE_ID_KEY + "=")[1].split(";")[0]));
        if (msg.contains(OPERATION_KEY + "=")) {
            int operationCode = Integer.valueOf(msg.split(OPERATION_KEY + "=")[1].split(";")[0]);
            switch (operationCode) {
                case INIT_REMOTE_SIGNED_BROWSER_SESSION:
                    qrMessageDto.setTypeVS(TypeVS.INIT_REMOTE_SIGNED_BROWSER_SESSION);
                    break;
                case QR_MESSAGE_INFO:
                    qrMessageDto.setTypeVS(TypeVS.QR_MESSAGE_INFO);
                    break;
                default:
                    log.log(Level.SEVERE, "unknown operation code: " + operationCode);
            }
        }
        if (msg.contains(OPERATION_ID_KEY + "="))
            qrMessageDto.setOperationId(msg.split(OPERATION_ID_KEY + "=")[1].split(";")[0]);
        if (msg.contains(WEB_SOCKET_SESSION_KEY + "="))
            qrMessageDto.setSessionId(msg.split(WEB_SOCKET_SESSION_KEY + "=")[1].split(";")[0]);
        return qrMessageDto;
    }

    public static String toQRCode(TypeVS operation, String operationId, String deviceId, String sessionId) {
        StringBuilder result = new StringBuilder();
        if(deviceId != null) result.append(DEVICE_ID_KEY + "=" + deviceId + ";");
        if(operation != null) result.append(OPERATION_KEY + "=" + operation + ";");
        if(operationId != null) result.append(OPERATION_ID_KEY + "=" + operationId + ";");
        if(sessionId != null) result.append(WEB_SOCKET_SESSION_KEY + "=" + sessionId + ";");
        return result.toString();
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

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }
}