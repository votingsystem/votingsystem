package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.ValidationException;
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
    private String operationCode;
    private TypeVS messageType;
    private TypeVS step;
    private State state = State.PENDING;
    private Integer statusCode;
    private Long deviceFromId;
    private Long deviceToId;
    private String message;
    private String encryptedMessage;
    private String UUID;
    private String remoteAddress;
    private String cmsMessagePEM;
    private String subject;
    private String toUser;
    private String contentToSign;
    private String from;
    private String deviceFromName;
    private String deviceToName;
    private String deviceId;//hardware id
    private String publicKeyPEM;
    private String x509CertificatePEM;
    private boolean timeLimited = false;
    private String URL;
    private List<CurrencyDto> currencyDtoList;
    private Date date;
    private DeviceDto connectedDevice;

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
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_FROM_VS);
        socketMessageDto.setMessageType(this.messageType);
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public SocketMessageDto getResponse(Integer statusCode, String message, Long deviceFromId,
                            CMSSignedMessage cmsMessage, TypeVS operation) throws Exception {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(this.deviceFromId);
        EncryptedContentDto encryptedDto = new EncryptedContentDto();
        encryptedDto.setStatusCode(statusCode);
        encryptedDto.setMessage(message);
        if(cmsMessage != null) encryptedDto.setCMSMessage(cmsMessage.toPEMStr());
        encryptedDto.setDeviceFromId(deviceFromId);
        encryptedDto.setOperation(operation);
        encryptMessage(encryptedDto);
        socketMessageDto.setUUID(UUID);
        return socketMessageDto;
    }

    public static SocketMessageDto INIT_AUTHENTICATED_SESSION_REQUEST(String deviceId) throws NoSuchAlgorithmException {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.INIT_SIGNED_SESSION);
        socketMessageDto.setDeviceId(deviceId);
        socketMessageDto.setUUID(java.util.UUID.randomUUID().toString());
        return socketMessageDto;
    }

    public static SocketMessageDto INIT_SESSION_RESPONSE(String sessionId) {
        return null;
    }

    public static SocketMessageDto INIT_SESSION_RESPONSE(Long deviceId) {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setStatusCode(ResponseVS.SC_WS_CONNECTION_INIT_OK);
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_FROM_VS);
        socketMessageDto.setMessageType(TypeVS.INIT_SESSION);
        socketMessageDto.setDeviceToId(deviceId);
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

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    public TypeVS getStep() {
        return step;
    }

    public SocketMessageDto setStep(TypeVS step) {
        this.step = step;
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

    public String getContentToSign() {
        return contentToSign;
    }

    public void setContentToSign(String contentToSign) {
        this.contentToSign = contentToSign;
    }


    public void setSession(Session session) throws ValidationException {
        this.session = session;
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

    public SocketMessageDto setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
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

    public String getX509CertificatePEM() {
        return x509CertificatePEM;
    }

    public void setX509CertificatePEM(String x509CertificatePEM) {
        this.x509CertificatePEM = x509CertificatePEM;
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

    public String getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }

    public String getCmsMessagePEM() {
        return cmsMessagePEM;
    }

    public void setCmsMessagePEM(String cmsMessagePEM) {
        this.cmsMessagePEM = cmsMessagePEM;
    }

    public static SocketMessageDto getCurrencyWalletChangeRequest(
            DeviceDto deviceTo, List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, currencyList, TypeVS.CURRENCY_WALLET_CHANGE);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setTimeLimited(true);
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setDeviceToId(deviceTo.getId());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        EncryptedContentDto encryptedDto = EncryptedContentDto.getCurrencyWalletChangeRequest(currencyList);
        encryptMessage(socketMessageDto, encryptedDto, deviceTo);
        return socketMessageDto;
    }

    public static SocketMessageDto getMessageToDevice(User user, DeviceDto deviceTo, String toUser,
                          String textToEncrypt) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, null, TypeVS.MESSAGEVS);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(deviceTo.getId());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        EncryptedContentDto encryptedDto = EncryptedContentDto.getMessageVSToDevice(user, toUser, textToEncrypt);
        encryptMessage(socketMessageDto, encryptedDto, deviceTo);
        return socketMessageDto;
    }

    //method to response a message previously received
    public SocketMessageDto getMessageResponse(User user, String textToEncrypt) throws Exception {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MSG_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        WebSocketSession socketSession = ContextVS.getInstance().getWSSession(UUID);
        socketMessageDto.setDeviceToId(deviceFromId);
        socketMessageDto.setDeviceToName(from);
        socketMessageDto.setUUID(socketSession.getUUID());
        EncryptedContentDto encryptedDto = EncryptedContentDto.getMessageVSToDevice(user, from, textToEncrypt);
        encryptMessage(encryptedDto);
        return socketMessageDto;
    }

    private static void encryptMessage(SocketMessageDto socketMessageDto,
                                       EncryptedContentDto encryptedDto, DeviceDto device) throws Exception {
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

    @JsonIgnore
    private void encryptMessage(EncryptedContentDto encryptedDto) throws Exception {
        if(x509CertificatePEM != null) {
            X509Certificate targetDeviceCert = PEMUtils.fromPEMToX509Cert(x509CertificatePEM.getBytes());
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
        EncryptedContentDto encryptedDto = JSON.getMapper().readValue(decryptedBytes, EncryptedContentDto.class);
        if(encryptedDto.getOperation() != null) {
            this.messageType = operation;
            operation = encryptedDto.getOperation();
        }
        if(encryptedDto.getOperationCode() != null) operationCode = encryptedDto.getOperationCode();
        if(encryptedDto.getStatusCode() != null) statusCode = encryptedDto.getStatusCode();
        if(encryptedDto.getStep() != null) step = encryptedDto.getStep();
        if(encryptedDto.getDeviceFromName() != null) deviceFromName = encryptedDto.getDeviceFromName();
        if(encryptedDto.getFrom() != null) from = encryptedDto.getFrom();
        if(encryptedDto.getDeviceFromId() != null) deviceFromId = encryptedDto.getDeviceFromId();
        if(encryptedDto.getCmsMessage() != null) cms = encryptedDto.getCMS();
        if(encryptedDto.getCurrencyList() != null) currencySet = CurrencyDto.deSerialize(encryptedDto.getCurrencyList());
        if(encryptedDto.getSubject() != null) subject = encryptedDto.getSubject();
        if(encryptedDto.getMessage() != null) message = encryptedDto.getMessage();
        if(encryptedDto.getToUser() != null) toUser = encryptedDto.getToUser();
        if(encryptedDto.getDeviceToName() != null) deviceToName = encryptedDto.getDeviceToName();
        if(encryptedDto.getURL()!= null) URL = encryptedDto.getURL();
        if(encryptedDto.getContentToSign() != null) contentToSign = encryptedDto.getContentToSign();
        if(encryptedDto.getLocale() != null) locale = encryptedDto.getLocale();
        if(encryptedDto.getX509CertificatePEM() != null) x509CertificatePEM = encryptedDto.getX509CertificatePEM();
        if(encryptedDto.getPublicKeyPEM() != null) publicKeyPEM = encryptedDto.getPublicKeyPEM();
        if(encryptedDto.getUUID() != null) UUID = encryptedDto.getUUID();
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