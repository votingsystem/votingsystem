package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.socket.SocketOperation;
import org.votingsystem.socket.Step;
import org.votingsystem.util.JSON;
import org.votingsystem.AppContext;
import org.votingsystem.crypto.Encryptor;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.socket.WebSocketSession;
import org.votingsystem.util.CurrencyOperation;
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

    private SocketOperation socketOperation;

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
    private AESParamsDto aesParams;
    private String certificatePEM;
    private boolean timeLimited = false;
    @JacksonXmlProperty(localName = "CurrencySet")
    private Set<CurrencyDto> currencyDtoSet;
    private ZonedDateTime date;
    private DeviceDto device;
    private String UUID;
    private String locale = Locale.getDefault().getDisplayLanguage();

    @JsonIgnore
    private User user;
    @JsonIgnore
    private Set<Currency> currencySet;


    public MessageDto() {}

    public MessageDto getServerResponse(Integer statusCode, String message){
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setStatusCode(statusCode);
        socketMessageDto.setSocketOperation(SocketOperation.MSG_FROM_SERVER);
        socketMessageDto.setOperation(this.operation);
        socketMessageDto.setMessage(message);
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public MessageDto getResponse(Integer statusCode, String message, String deviceFromUUID,
                  String base64Data, OperationTypeDto operation) throws Exception {
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setDeviceToUUID(this.deviceFromUUID);
        MessageDto encryptedDto = new MessageDto();
        encryptedDto.setStatusCode(statusCode);
        encryptedDto.setMessage(message);
        encryptedDto.setBase64Data(base64Data);
        encryptedDto.setDeviceFromUUID(deviceFromUUID);
        encryptedDto.setOperation(operation);
        encryptMessage(encryptedDto);
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
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

    public SocketOperation getSocketOperation() {
        return socketOperation;
    }

    public MessageDto setSocketOperation(SocketOperation socketOperation) {
        this.socketOperation = socketOperation;
        return this;
    }

    public AESParamsDto getAesParams() {
        return aesParams;
    }

    public void setAesParams(AESParamsDto aesParams) {
        this.aesParams = aesParams;
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
        if(currencySet == null && currencyDtoSet != null) currencySet = CurrencyDto.deSerialize(currencyDtoSet);
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

    public static MessageDto getCurrencyWalletChangeRequest(String deviceFromName, String deviceFromUUID,
            String userFromName, DeviceDto deviceTo, List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = AppContext.getInstance().checkWebSocketSession(deviceTo, currencyList,
                new OperationTypeDto(CurrencyOperation.CURRENCY_WALLET_CHANGE, null));
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setTimeLimited(true);
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setDeviceToUUID(deviceTo.getUUID());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        MessageDto encryptedDto = new MessageDto(deviceFromName, deviceFromUUID, deviceTo.getName(), deviceTo.getUUID(),
                null).setUserToName(deviceTo.getUserFullName()).setUserFromName(userFromName);
        encryptedDto.setCurrencyDtoSet(CurrencyDto.serializeCollection(currencyList));
        encryptMessage(socketMessageDto, encryptedDto, deviceTo);
        return socketMessageDto;
    }

    public static MessageDto getMessageToDevice(String deviceFromName, String deviceFromUUID, DeviceDto deviceTo,
                                                String message) throws Exception {
        WebSocketSession socketSession = AppContext.getInstance().checkWebSocketSession(deviceTo, null,
                new OperationTypeDto(CurrencyOperation.MSG_TO_DEVICE, null));
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        socketMessageDto.setDeviceToUUID(deviceTo.getUUID());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        MessageDto encryptedDto = new MessageDto(deviceFromName, deviceFromUUID, deviceTo.getName(), deviceTo.getUUID(),
                message).setUserToName(deviceTo.getUserFullName()).setUserFromName(deviceFromUUID);
        encryptMessage(socketMessageDto, encryptedDto, deviceTo);
        return socketMessageDto;
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

    public MessageDto(String deviceFromName, String deviceFromUUID, String deviceToName, String deviceToUUID, String message) {
        this.deviceFromName = deviceFromName;
        this.deviceFromUUID = deviceFromUUID;
        this.deviceToName = deviceToName;
        this.deviceFromUUID = deviceToUUID;
        this.message = message;
    }
    //    public static EncryptedContentDto getMessageToDevice(String deviceFromName, String deviceFromUUID, String deviceToName, String message)

    //method to respond a received message
    public MessageDto getMessageResponse(String deviceFromName, String deviceFromUUID, String deviceToName,
                                         String message) throws Exception {
        MessageDto socketMessageDto = new MessageDto();
        socketMessageDto.setSocketOperation(SocketOperation.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseDto.SC_PROCESSING);
        WebSocketSession socketSession = AppContext.getInstance().getWSSession(UUID);
        socketMessageDto.setDeviceToUUID(deviceFromUUID);
        socketMessageDto.setDeviceToName(deviceFromName);
        socketMessageDto.setUUID(socketSession.getUUID());
        MessageDto encryptedDto = new MessageDto(deviceFromName, deviceFromUUID, deviceToName, this.getUUID(), message);
        encryptMessage(encryptedDto);
        return socketMessageDto;
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

    private static void encryptMessage(MessageDto socketMessageDto,
                                       MessageDto encryptedDto, DeviceDto device) throws Exception {
        if(device.getX509Certificate() != null) {
            byte[] encryptedCMS_PEM = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), device.getX509Certificate());
            socketMessageDto.setEncryptedMessage(new String(encryptedCMS_PEM));
        } else if(device.getPublicKeyPEM() != null) {
            byte[] encryptedCMS_PEM = Encryptor.encryptToCMS(JSON.getMapper().writeValueAsBytes(encryptedDto),
                    PEMUtils.fromPEMToRSAPublicKey(device.getPublicKeyPEM()));
            socketMessageDto.setEncryptedMessage(new String(encryptedCMS_PEM));
        } else log.log(Level.SEVERE, "Missing target public key info");
    }

    @JsonIgnore
    private void encryptMessage(MessageDto encryptedDto) throws Exception {
        if(certificatePEM != null) {
            X509Certificate targetDeviceCert = PEMUtils.fromPEMToX509Cert(certificatePEM.getBytes());
            byte[] encryptedMessageBytes = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), targetDeviceCert);
            encryptedMessage = new String(encryptedMessageBytes);
        } else if(publicKeyPEM != null) {
            PublicKey publicKey = PEMUtils.fromPEMToRSAPublicKey(publicKeyPEM);
            byte[] encryptedMessageBytes = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), publicKey);
            encryptedMessage = new String(encryptedMessageBytes);
        } else log.log(Level.SEVERE, "Missing target public key info");
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
        if(decryptedDto.getAesParams() != null)
            aesParams = decryptedDto.getAesParams();
        if(decryptedDto.getCertificatePEM() != null)
            certificatePEM = decryptedDto.getCertificatePEM();
        if(decryptedDto.getPublicKeyPEM() != null)
            publicKeyPEM = decryptedDto.getPublicKeyPEM();
        if(decryptedDto.getUUID() != null)
            UUID = decryptedDto.getUUID();
        timeLimited = decryptedDto.isTimeLimited();
        this.encryptedMessage = null;
    }

}