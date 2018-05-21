package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.http.HttpResponse;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.SystemOperation;
import org.votingsystem.xml.XML;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "Operation")
public class OperationDto<T> implements Serializable {


    @JacksonXmlProperty(localName = "Date", isAttribute = true)
    private ZonedDateTime date;
    @JacksonXmlProperty(localName = "EntityId", isAttribute = true)
    private String entityId;

    @JacksonXmlProperty(localName = "Type")
    private SystemOperation type;
    @JacksonXmlProperty(localName = "OperationCode")
    private String operationCode;

    @JacksonXmlProperty(localName = "StatusCode")
    private Integer statusCode;
    @JacksonXmlProperty(localName = "Message")
    private String message;
    @JacksonXmlProperty(localName = "DeviceId")
    private String deviceId;
    @JacksonXmlProperty(localName = "DeviceUUID")
    private String deviceUUID;
    @JacksonXmlProperty(localName = "HttpSessionId")
    private String httpSessionId;
    @JacksonXmlProperty(localName = "Base64Data")
    private String base64Data;
    @JacksonXmlProperty(localName = "SessionUUID")
    private String sessionUUID;
    @JacksonXmlProperty(localName = "UUID")
    private String UUID;
    @JacksonXmlProperty(localName = "URL")
    private String url;


    @JacksonXmlProperty(localName = "PublicKeyBase64")
    private String publicKeyBase64;

    @JsonIgnore private T data;
    @JsonIgnore private IdentityRequestDto identityRequest;

    public OperationDto() {}

    public OperationDto(Integer statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public OperationDto(String entityId, SystemOperation operationType) {
        this.type = operationType;
        this.entityId = entityId;
    }

    public OperationDto(String entityId, OperationType operationType, T data, LocalDateTime localDateTime) {
        date = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
        this.type = operationType;
        this.entityId = entityId;
        this.data = data;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }

    @JsonIgnore
    public String getUUID() {
        return UUID;
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

    public String getSessionUUID() {
        return sessionUUID;
    }

    public OperationDto setSessionUUID(String sessionUUID) {
        this.sessionUUID = sessionUUID;
        return this;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getDeviceUUID() {
        return deviceUUID;
    }

    public void setDeviceUUID(String deviceUUID) {
        this.deviceUUID = deviceUUID;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public <T> T getDataJson(Class<T> type) throws Exception {
        return new JSON().getMapper().readValue(Base64.getDecoder().decode(base64Data), type);
    }

    public <T> T getDataXml(Class<T> type) throws Exception {
        return new XML().getMapper().readValue(Base64.getDecoder().decode(base64Data), type);
    }

    @JsonIgnore
    public byte[] getBase64DataDecoded() {
        return Base64.getDecoder().decode(base64Data);
    }

    @JsonIgnore
    public PublicKey getRSAPublicKey() throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
        //fix qr codes replacements of '+' with spaces
        publicKeyBase64 = publicKeyBase64.replace(" ", "+");
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(java.util.Base64.getDecoder().decode(publicKeyBase64));
        return factory.generatePublic(pubKeySpec);
    }

    public void checkEntityAndType(SystemOperation type, String entityId) throws ValidationException {
        if(this.entityId == null || this.type == null || !this.entityId.equals(entityId) ||
                !this.type.toString().equals(type.toString())) {
            throw new ValidationException("Expected Operation - type: " + type + " - entityId: " + entityId +
                    " - found: " + this.type + " - " + this.entityId);
        }
    }

    public IdentityRequestDto getIdentityRequest() {
        return identityRequest;
    }

    public OperationDto setIdentityRequest(IdentityRequestDto identityRequest) {
        this.identityRequest = identityRequest;
        return this;
    }

    @JsonIgnore
    public byte[] encode(String contentType, LocalDateTime localDateTime) throws IOException {
        this.date = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
        if(data != null && base64Data == null) {
            if(contentType.contains("json"))
                this.base64Data = Base64.getEncoder().encodeToString(new JSON().getMapper().writeValueAsBytes(data));
            else
                this.base64Data = Base64.getEncoder().encodeToString(new XML().getMapper().writeValueAsBytes(data));
        }
        if(contentType.contains("json"))
            return new JSON().getMapper().writeValueAsBytes(this);
        else
            return new XML().getMapper().writeValueAsBytes(this);
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public SystemOperation getType() {
        return type;
    }

    public OperationDto setType(SystemOperation type) {
        this.type = type;
        return this;
    }

    public String getEntityId() {
        return entityId;
    }

    public OperationDto setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public OperationDto setOperationCode(String operationCode) {
        this.operationCode = operationCode;
        return this;
    }

}