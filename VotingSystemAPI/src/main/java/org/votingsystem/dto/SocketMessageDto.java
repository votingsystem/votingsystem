package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.WebSocketSession;
import org.votingsystem.util.crypto.Encryptor;
import org.votingsystem.util.crypto.PEMUtils;

import javax.websocket.Session;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageDto {

    private static Logger log = Logger.getLogger(SocketMessageDto.class.getName());

    public enum State {PENDING, PROCESSED, LAPSED, REMOVED}
    public enum ConnectionStatus {OPEN, CLOSED}

    private TypeVS operation;
    private TypeVS messageType;
    private TypeVS messageSubType;
    private State state = State.PENDING;
    private Integer statusCode;
    private Long deviceFromId;
    private Long deviceToId;
    private String sessionId;
    private String message;
    private String encryptedMessage;
    private String UUID;
    private String remoteAddress;
    private String cmsMessagePEM;
    private String subject;
    private String toUser;
    private String textToSign;
    private String from;
    private String deviceFromName;
    private String deviceToName;
    private String deviceId;//hardware id
    private String pemPublicKey;
    private String pemCert;
    private boolean timeLimited = false;
    private String URL;
    private List<CurrencyDto> currencyDtoList;
    private Date date;
    private DeviceDto connectedDevice;

    private SocketMessageEncryptedDto encryptedDto;
    @JsonIgnore private User user;
    @JsonIgnore private Set<Currency> currencySet;
    @JsonIgnore private WebSocketSession webSocketSession;
    @JsonIgnore private Session session;
    @JsonIgnore private CMSSignedMessage cms;
    private String locale = ContextVS.getInstance().getLocale().getLanguage();

    public SocketMessageDto () {}

    public SocketMessageDto getServerResponse(Integer statusCode, String message){
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setStatusCode(statusCode);
        socketMessageDto.setMessage(message);
        socketMessageDto.setSessionId(session.getId());
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_FROM_VS);
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public SocketMessageDto getResponse(Integer statusCode, String message, Long deviceFromId,
                            CMSSignedMessage cmsMessage, TypeVS operation) throws Exception {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_SESSION_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setSessionId(sessionId);
        SocketMessageEncryptedDto encryptedDto = new SocketMessageEncryptedDto();
        encryptedDto.setStatusCode(statusCode);
        encryptedDto.setMessage(message);
        if(cmsMessage != null) encryptedDto.setCMSMessage(cmsMessage.toPEMStr());
        encryptedDto.setDeviceFromId(deviceFromId);
        encryptedDto.setOperation(operation);
        setEncryptedMessage(encryptedDto);
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public static SocketMessageDto INIT_SESSION_REQUEST(String deviceId) throws NoSuchAlgorithmException {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.INIT_SIGNED_SESSION);
        socketMessageDto.setDeviceId(deviceId);
        socketMessageDto.setUUID(java.util.UUID.randomUUID().toString());
        return socketMessageDto;
    }

    public static SocketMessageDto INIT_SESSION_RESPONSE(String sessionId) {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_FROM_VS);
        socketMessageDto.setMessageType(TypeVS.INIT_SESSION);
        socketMessageDto.setSessionId(sessionId);
        socketMessageDto.setUUID(java.util.UUID.randomUUID().toString());
        return socketMessageDto;
    }

    public List<CurrencyDto> getCurrencyDtoList() {
        return currencyDtoList;
    }

    public void setCurrencyDtoList(List<CurrencyDto> currencyDtoList) {
        this.currencyDtoList = currencyDtoList;
    }

    @JsonIgnore public boolean isEncrypted() {
        return encryptedMessage != null;
    }

    public TypeVS getMessageType() {
        return messageType;
    }

    public SocketMessageDto setMessageType(TypeVS messageType) {
        this.messageType = messageType;
        return this;
    }

    public String getPemPublicKey() {
        return pemPublicKey;
    }

    public void setPemPublicKey(String pemPublicKey) {
        this.pemPublicKey = pemPublicKey;
    }

    public TypeVS getMessageSubType() {
        return messageSubType;
    }

    public SocketMessageDto setMessageSubType(TypeVS messageSubType) {
        this.messageSubType = messageSubType;
        return this;
    }

    public Long getDeviceFromId() {
        return deviceFromId;
    }

    public void setDeviceFromId(Long deviceFromId) {
        this.deviceFromId = deviceFromId;
    }

    public Long getDeviceToId() {
        return deviceToId;
    }

    public void setDeviceToId(Long deviceToId) {
        this.deviceToId = deviceToId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public SocketMessageDto setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public SocketMessageDto setOperation(TypeVS operation) {
        this.operation = operation;
        return this;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public Session getSession() {
        return session;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getTextToSign() {
        return textToSign;
    }

    public void setTextToSign(String textToSign) {
        this.textToSign = textToSign;
    }


    public void setSession(Session session) throws ValidationExceptionVS {
        this.session = session;
        //if sessionId isn't null is because it's a MSG_TO_DEVICE_BY_TARGET_SESSION_ID
        if(sessionId == null) this.sessionId = session.getId();
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

    public SocketMessageDto setMessage(String message) {
        this.message = message;
        return this;
    }

    public <S> S getMessage(Class<S> type) throws Exception {
        return JSON.getMapper().readValue(message, type);
    }

    public <T> T getMessage(TypeReference<T> type) throws Exception {
        return JSON.getMapper().readValue(message, type);
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCmsMessage() {
        return cmsMessagePEM;
    }

    public void setCMSMessage(String cmsMessage) {
        this.cmsMessagePEM = cmsMessage;
    }

    @JsonIgnore
    public CMSSignedMessage getCMS() throws Exception {
        if(cms == null) cms = CMSSignedMessage.FROM_PEM(cmsMessagePEM);
        return cms;
    }

    public SocketMessageDto setCMS(CMSSignedMessage cmsMessage) throws Exception {
        this.cms = cmsMessage;
        this.cmsMessagePEM = cmsMessage.toPEMStr();
        return this;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public SocketMessageEncryptedDto getContent() {
        return encryptedDto;
    }

    public void setContent(SocketMessageEncryptedDto content) {
        this.encryptedDto = content;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public SocketMessageDto setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
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

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Currency> getCurrencySet() throws Exception {
        if(currencySet == null && currencyDtoList != null) currencySet = CurrencyDto.deSerialize(currencyDtoList);
        return currencySet;
    }

    public void setCurrencySet(Set<Currency> currencySet) {
        this.currencySet = currencySet;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public void setWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
        this.UUID = webSocketSession.getUUID();
    }

    public String getPemCert() {
        return pemCert;
    }

    public void setPemCert(String pemCert) {
        this.pemCert = pemCert;
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

    public DeviceDto getConnectedDevice() {
        return connectedDevice;
    }

    public void setConnectedDevice(DeviceDto connectedDevice) {
        this.connectedDevice = connectedDevice;
    }

    public static SocketMessageDto getSignRequest(DeviceDto deviceTo, String toUser, String textToSign, String subject)
            throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, null, TypeVS.MESSAGEVS_SIGN);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_DEVICE_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(deviceTo.getId());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        SocketMessageEncryptedDto encryptedDto = SocketMessageEncryptedDto.getSignRequest(toUser, textToSign, subject);
        setEncryptedMessage(socketMessageDto, encryptedDto, deviceTo);
        return socketMessageDto;
    }

    public static SocketMessageDto getCurrencyWalletChangeRequest(
            DeviceDto deviceTo, List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, currencyList, TypeVS.CURRENCY_WALLET_CHANGE);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_DEVICE_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setTimeLimited(true);
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setDeviceToId(deviceTo.getId());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        SocketMessageEncryptedDto encryptedDto = SocketMessageEncryptedDto.getCurrencyWalletChangeRequest(currencyList);
        setEncryptedMessage(socketMessageDto, encryptedDto, deviceTo);
        return socketMessageDto;
    }

    public static SocketMessageDto getMessageToDevice(User user, DeviceDto deviceTo, String toUser,
                          String textToEncrypt) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, null, TypeVS.MESSAGEVS);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_DEVICE_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(deviceTo.getId());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        SocketMessageEncryptedDto encryptedDto = SocketMessageEncryptedDto.getMessageVSToDevice(user, toUser, textToEncrypt);
        setEncryptedMessage(socketMessageDto, encryptedDto, deviceTo);
        return socketMessageDto;
    }

    //method to response a message previously received
    public SocketMessageDto getMessageResponse(User user, String textToEncrypt) throws Exception {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE_BY_TARGET_DEVICE_ID);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        WebSocketSession socketSession = ContextVS.getInstance().getWSSession(UUID);
        socketMessageDto.setDeviceToId(deviceFromId);
        socketMessageDto.setDeviceToName(from);
        socketMessageDto.setUUID(socketSession.getUUID());
        SocketMessageEncryptedDto encryptedDto = SocketMessageEncryptedDto.getMessageVSToDevice(user, from, textToEncrypt);
        setEncryptedMessage(encryptedDto);
        return socketMessageDto;
    }

    private static void setEncryptedMessage(SocketMessageDto socketMessageDto,
            SocketMessageEncryptedDto encryptedDto, DeviceDto device) throws Exception {
        if(device.getX509Certificate() != null) {
            byte[] encryptedCMS_PEM = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), device.getX509Certificate());
            socketMessageDto.setEncryptedMessage(new String(encryptedCMS_PEM));
        } else if(device.getPublicKey() != null) {
            byte[] encryptedCMS_PEM = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), device.getPublicKey());
            socketMessageDto.setEncryptedMessage(new String(encryptedCMS_PEM));
        } else log.log(Level.SEVERE, "Missing target public key info");
    }

    private void setEncryptedMessage(SocketMessageEncryptedDto encryptedDto) throws Exception {
        if(pemCert != null) {
            X509Certificate targetDeviceCert = PEMUtils.fromPEMToX509Cert(pemCert.getBytes());
            byte[] encryptedMessageBytes = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), targetDeviceCert);
            encryptedMessage = new String(encryptedMessageBytes);
        } else if(pemPublicKey != null) {
            PublicKey publicKey = PEMUtils.fromPEMToRSAPublicKey(pemPublicKey);
            byte[] encryptedMessageBytes = Encryptor.encryptToCMS(
                    JSON.getMapper().writeValueAsBytes(encryptedDto), publicKey);
            encryptedMessage = new String(encryptedMessageBytes);
        } else log.log(Level.SEVERE, "Missing target public key info");
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        byte[] decryptedBytes = Encryptor.decryptCMS(encryptedMessage.getBytes(), privateKey);
        SocketMessageEncryptedDto encryptedDto =
                JSON.getMapper().readValue(decryptedBytes, SocketMessageEncryptedDto.class);
        if(encryptedDto.getOperation() != null) {
            this.messageType = operation;
            operation = encryptedDto.getOperation();
        }
        if(encryptedDto.getStatusCode() != null) statusCode = encryptedDto.getStatusCode();
        if(encryptedDto.getDeviceFromName() != null) deviceFromName = encryptedDto.getDeviceFromName();
        if(encryptedDto.getFrom() != null) from = encryptedDto.getFrom();
        if(encryptedDto.getDeviceFromId() != null) deviceFromId = encryptedDto.getDeviceFromId();
        if(encryptedDto.getCmsMessage() != null) cms = encryptedDto.getCMS();
        if(encryptedDto.getCurrencyList() != null) currencySet = CurrencyDto.deSerialize(
                encryptedDto.getCurrencyList());
        if(encryptedDto.getSubject() != null) subject = encryptedDto.getSubject();
        if(encryptedDto.getMessage() != null) message = encryptedDto.getMessage();
        if(encryptedDto.getToUser() != null) toUser = encryptedDto.getToUser();
        if(encryptedDto.getDeviceToName() != null) deviceToName = encryptedDto.getDeviceToName();
        if(encryptedDto.getURL()!= null) URL = encryptedDto.getURL();
        if(encryptedDto.getTextToSign() != null) textToSign = encryptedDto.getTextToSign();
        if(encryptedDto.getLocale() != null) locale = encryptedDto.getLocale();
        if(encryptedDto.getPemCert() != null) pemCert = encryptedDto.getPemCert();
        if(encryptedDto.getPemPublicKey() != null) pemPublicKey = encryptedDto.getPemPublicKey();
        timeLimited = encryptedDto.isTimeLimited();
        this.encryptedMessage = null;
    }

    public static <T> WebSocketSession checkWebSocketSession (DeviceDto deviceTo, T data, TypeVS typeVS)
            throws NoSuchAlgorithmException {
        WebSocketSession webSocketSession = null;
        if(deviceTo != null) webSocketSession = ContextVS.getInstance().getWSSession(deviceTo.getId());
        if(webSocketSession == null) {
            webSocketSession = new WebSocketSession(deviceTo).setUUID(
                    java.util.UUID.randomUUID().toString());
            ContextVS.getInstance().putWSSession(webSocketSession.getUUID(), webSocketSession);
        }
        webSocketSession.setData(data);
        webSocketSession.setTypeVS(typeVS);
        return webSocketSession;
    }

}