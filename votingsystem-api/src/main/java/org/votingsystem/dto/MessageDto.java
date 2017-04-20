package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.votingsystem.AppContext;
import org.votingsystem.crypto.Encryptor;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.socket.Step;
import org.votingsystem.socket.WebSocketSession;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;
import org.votingsystem.xml.XML;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDto {

    private static Logger log = Logger.getLogger(MessageDto.class.getName());

    @JacksonXmlProperty(localName = "Operation")
    private OperationTypeDto operation;
    private Step step;
    private Integer statusCode;
    private String operationCode;
    private String deviceFromUUID;
    private String deviceToUUID;
    private String userFromName;
    private String userToName;
    private String deviceFromName;
    private String deviceToName;
    private String message;
    private String encryptedMessage;
    private String base64Data;
    private String subject;
    private String publicKeyPEM;
    private String certificatePEM;
    private boolean timeLimited = false;
    @JacksonXmlProperty(localName = "CurrencySet")
    private Set<CurrencyDto> currencyDtoSet;
    private ZonedDateTime date;
    private DeviceDto device;
    private String UUID;
    private String locale = Locale.getDefault().toLanguageTag();

    @JsonIgnore
    private User user;
    @JsonIgnore
    private Set<Currency> currencySet;


    public MessageDto() {}

    public MessageDto(String deviceFromName, String deviceFromUUID, String deviceToName, String deviceToUUID,
                      String message) {
        this.deviceFromName = deviceFromName;
        this.deviceFromUUID = deviceFromUUID;
        this.deviceToName = deviceToName;
        this.deviceFromUUID = deviceToUUID;
        this.message = message;
    }

    public Set<CurrencyDto> getCurrencyDtoSet() {
        return currencyDtoSet;
    }

    public void setCurrencyDtoSet(Set<CurrencyDto> currencyDtoSet) {
        this.currencyDtoSet = currencyDtoSet;
    }

    @JsonIgnore
    public boolean isEncrypted() {
        return encryptedMessage != null;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    public Step getStep() {
        return step;
    }

    public MessageDto setStep(Step step) {
        this.step = step;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public MessageDto setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getDeviceFromUUID() {
        return deviceFromUUID;
    }

    public MessageDto setDeviceFromUUID(String deviceFromUUID) {
        this.deviceFromUUID = deviceFromUUID;
        return this;
    }

    public String getDeviceToUUID() {
        return deviceToUUID;
    }

    public MessageDto setDeviceToUUID(String deviceToUUID) {
        this.deviceToUUID = deviceToUUID;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public MessageDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Currency> getCurrencySet() throws Exception {
        if(currencySet == null && currencyDtoSet != null)
            currencySet = CurrencyDto.deSerialize(currencyDtoSet);
        return currencySet;
    }

    public void setCurrencySet(Set<Currency> currencySet) {
        this.currencySet = currencySet;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public String getCertificatePEM() {
        return certificatePEM;
    }

    public void setCertificatePEM(String certificatePEM) {
        this.certificatePEM = certificatePEM;
    }

    public String getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setEncryptedMessage(String encryptedMessage) {
        this.encryptedMessage = encryptedMessage;
    }

    public String getDeviceToName() {
        return deviceToName;
    }

    public void setDeviceToName(String deviceToName) {
        this.deviceToName = deviceToName;
    }

    public OperationTypeDto getOperation() {
        return operation;
    }

    public MessageDto setOperation(OperationTypeDto operation) {
        this.operation = operation;
        return this;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public MessageDto setBase64Data(String base64Data) {
        this.base64Data = base64Data;
        return this;
    }

    public String getUserFromName() {
        return userFromName;
    }

    public MessageDto setUserFromName(String userFromName) {
        this.userFromName = userFromName;
        return this;
    }

    public String getUserToName() {
        return userToName;
    }

    public MessageDto setUserToName(String userToName) {
        this.userToName = userToName;
        return this;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public DeviceDto getDevice() {
        return device;
    }

    public void setDevice(DeviceDto device) {
        this.device = device;
    }

    private static String encryptMessage(MessageDto msgToEncrypt, DeviceDto targetDevice) throws Exception {
        if(targetDevice.getX509Certificate() != null) {
            byte[] encryptedMsg = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(msgToEncrypt), targetDevice.getX509Certificate());
            return new String(encryptedMsg);
        } else if(targetDevice.getPublicKeyPEM() != null) {
            byte[] encryptedCMS_PEM = Encryptor.encryptToCMS(JSON.getMapper().writeValueAsBytes(msgToEncrypt),
                    PEMUtils.fromPEMToRSAPublicKey(targetDevice.getPublicKeyPEM()));
            return new String(encryptedCMS_PEM);
        } else throw new IllegalArgumentException("Imposible to encrypt message, target device without certificate");
    }

    @JsonIgnore
    public void decryptMessage(PrivateKey privateKey) throws Exception {
        byte[] decryptedBytes = Encryptor.decryptCMS(encryptedMessage.getBytes(), privateKey);
        MessageDto decryptedDto = XML.getMapper().readValue(decryptedBytes, MessageDto.class);
        this.operation = decryptedDto.getOperation();
        if(decryptedDto.getOperationCode() != null)
            operationCode = decryptedDto.getOperationCode();
        if(decryptedDto.getStatusCode() != null)
            statusCode = decryptedDto.getStatusCode();
        if(decryptedDto.getStep() != null)
            step = decryptedDto.getStep();
        if(decryptedDto.getDeviceFromName() != null)
            deviceFromName = decryptedDto.getDeviceFromName();
        if(decryptedDto.getDeviceFromUUID() != null)
            deviceFromUUID = decryptedDto.getDeviceFromUUID();
        if(decryptedDto.getBase64Data() != null)
            base64Data = decryptedDto.getBase64Data();
        if(decryptedDto.getCurrencySet() != null)
            currencySet = CurrencyDto.deSerialize(decryptedDto.getCurrencyDtoSet());
        if(decryptedDto.getSubject() != null)
            subject = decryptedDto.getSubject();
        if(decryptedDto.getMessage() != null)
            message = decryptedDto.getMessage();
        if(decryptedDto.getDeviceToName() != null)
            deviceToName = decryptedDto.getDeviceToName();
        if(decryptedDto.getLocale() != null)
            locale = decryptedDto.getLocale();
        if(decryptedDto.getCertificatePEM() != null)
            certificatePEM = decryptedDto.getCertificatePEM();
        if(decryptedDto.getPublicKeyPEM() != null)
            publicKeyPEM = decryptedDto.getPublicKeyPEM();
        if(decryptedDto.getUUID() != null)
            UUID = decryptedDto.getUUID();
        timeLimited = decryptedDto.isTimeLimited();
        this.encryptedMessage = null;
    }

    public MessageDto getServerResponse(Integer statusCode, String message, String entityId){
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setStatusCode(statusCode);
        socketMessageDto.setOperation(new OperationTypeDto(CurrencyOperation.MSG_TO_DEVICE, entityId));
        socketMessageDto.setOperation(this.operation);
        socketMessageDto.setMessage(message);
        socketMessageDto.setDate(ZonedDateTime.now());
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public MessageDto getResponse(Integer statusCode, String message, String deviceFromUUID,
                                  String base64Data, String entityId) throws Exception {
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setOperation(new OperationTypeDto(CurrencyOperation.MSG_TO_DEVICE, entityId));
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setDeviceToUUID(this.deviceFromUUID);
        MessageDto msgToEncrypt = new MessageDto();
        msgToEncrypt.setStatusCode(statusCode);
        msgToEncrypt.setMessage(message);
        msgToEncrypt.setBase64Data(base64Data);
        msgToEncrypt.setDeviceFromUUID(deviceFromUUID);
        msgToEncrypt.setOperation(operation);
        if(certificatePEM != null) {
            X509Certificate targetDeviceCert = PEMUtils.fromPEMToX509Cert(certificatePEM.getBytes());
            byte[] encryptedMessageBytes = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(msgToEncrypt), targetDeviceCert);
            encryptedMessage = new String(encryptedMessageBytes);
        } else if(publicKeyPEM != null) {
            PublicKey publicKey = PEMUtils.fromPEMToRSAPublicKey(publicKeyPEM);
            byte[] encryptedMessageBytes = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(msgToEncrypt), publicKey);
            encryptedMessage = new String(encryptedMessageBytes);
        } else log.log(Level.SEVERE, "Missing target public key info");
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public static MessageDto buildMessageToDevice(String deviceFromName, String deviceFromUUID, DeviceDto deviceTo,
                                                String message, String entityId) throws Exception {
        WebSocketSession socketSession = AppContext.getInstance().checkWebSocketSession(deviceTo, null,
                new OperationTypeDto(CurrencyOperation.MSG_TO_DEVICE, null));
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setOperation(new OperationTypeDto(CurrencyOperation.MSG_TO_DEVICE, entityId));
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setDeviceToUUID(deviceTo.getUUID());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        MessageDto msgToEncrypt = new MessageDto(deviceFromName, deviceFromUUID, deviceTo.getName(), deviceTo.getUUID(),
                message).setUserToName(deviceTo.getUserFullName()).setUserFromName(deviceFromUUID);
        socketMessageDto.setEncryptedMessage(encryptMessage(msgToEncrypt, deviceTo));
        return socketMessageDto;
    }

    public static MessageDto getCurrencyWalletChangeRequest(String userFromName, String deviceFromName,
            String deviceFromUUID, DeviceDto deviceTo, List<Currency> currencyList, String entityId) throws Exception {
        WebSocketSession socketSession = AppContext.getInstance().checkWebSocketSession(deviceTo, currencyList,
                new OperationTypeDto(CurrencyOperation.CURRENCY_WALLET_CHANGE, entityId));
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setOperation(new OperationTypeDto(CurrencyOperation.MSG_TO_DEVICE, entityId));
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setTimeLimited(true);
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setDeviceToUUID(deviceTo.getUUID());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        MessageDto msgToEncrypt = new MessageDto(deviceFromName, deviceFromUUID, deviceTo.getName(), deviceTo.getUUID(),
                null).setUserToName(deviceTo.getUserFullName()).setUserFromName(userFromName);
        msgToEncrypt.setCurrencyDtoSet(CurrencyDto.serializeCollection(currencyList));
        socketMessageDto.setEncryptedMessage(encryptMessage(msgToEncrypt, deviceTo));
        return socketMessageDto;
    }

}