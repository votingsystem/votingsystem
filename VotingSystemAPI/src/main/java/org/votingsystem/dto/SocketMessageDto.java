package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.WebSocketSession;

import javax.websocket.Session;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageDto {

    public enum State {PENDING, PROCESSED, LAPSED, REMOVED}
    public enum ConnectionStatus {OPEN, CLOSED}

    private TypeVS operation;
    private TypeVS messageType;
    private State state = State.PENDING;
    private Integer statusCode;
    private Long deviceFromId;
    private Long deviceToId;
    private String sessionId;
    private String message;
    private String encryptedMessage;
    private String UUID;
    private String remoteAddress;
    private String smimeMessage;
    private String aesParams;
    private String subject;
    private String toUser;
    private String textToSign;
    private String from;
    private String deviceFromName;
    private String deviceToName;
    private String deviceId;//hardware id
    private boolean timeLimited = false;
    private String URL;
    private List<CurrencyDto> currencyDtoList;
    private Date date;
    private DeviceVSDto connectedDevice;

    private SocketMessageContentDto content;
    @JsonIgnore private UserVS userVS;
    @JsonIgnore private Set<Currency> currencySet;
    @JsonIgnore private AESParams aesEncryptParams;
    @JsonIgnore private WebSocketSession webSocketSession;
    @JsonIgnore private Session session;
    @JsonIgnore private SMIMEMessage smime;
    private String locale = ContextVS.getInstance().getLocale().getLanguage();

    public SocketMessageDto () {}

    public SocketMessageDto getServerResponse(Integer statusCode, String message){
        SocketMessageDto responseDto = new SocketMessageDto();
        responseDto.setStatusCode(statusCode);
        responseDto.setMessage(message);
        responseDto.setSessionId(session.getId());
        responseDto.setOperation(TypeVS.MESSAGEVS_FROM_VS);
        responseDto.setUUID(UUID);
        return responseDto;
    }

    public SocketMessageDto getResponse(Integer statusCode, String message, Long deviceFromId,
            SMIMEMessage smimeMessage, TypeVS operation) throws Exception {
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(TypeVS.MESSAGEVS_FROM_DEVICE);
        messageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        messageDto.setSessionId(sessionId);
        SocketMessageContentDto messageContentDto = new SocketMessageContentDto();
        messageContentDto.setStatusCode(statusCode);
        messageContentDto.setMessage(message);
        if(smimeMessage != null) messageContentDto.setSmimeMessage(
                Base64.getEncoder().encodeToString(smimeMessage.getBytes()));
        messageContentDto.setDeviceFromId(deviceFromId);
        messageContentDto.setOperation(operation);
        messageDto.setEncryptedMessage(Encryptor.encryptAES(
                JSON.getMapper().writeValueAsString(messageContentDto), aesEncryptParams));
        messageDto.setUUID(UUID);
        return messageDto;
    }

    public static SocketMessageDto INIT_SESSION_REQUEST(String deviceId) throws NoSuchAlgorithmException {
        WebSocketSession socketSession = checkWebSocketSession(null, null, TypeVS.INIT_SIGNED_SESSION);
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setOperation(socketSession.getTypeVS());
        messageDto.setDeviceId(deviceId);
        messageDto.setUUID(socketSession.getUUID());
        return messageDto;
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

    public void setMessageType(TypeVS messageType) {
        this.messageType = messageType;
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

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
        /*this.remoteAddress = ((String)((AbstractServletOutputStream)((WsRemoteEndpointImplServer)((WsRemoteEndpointAsync)
                ((WsSession)session).remoteEndpointAsync).base).sos).socketWrapper.getRemoteAddr());*/
        this.session = session;
        //if sessionId isn't null is because it's a MESSAGEVS_FROM_DEVICE
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

    public void setMessage(String message) {
        this.message = message;
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

    public String getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(String smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    @JsonIgnore
    public SMIMEMessage getSMIME() throws Exception {
        if(smime == null) smime = new SMIMEMessage(Base64.getDecoder().decode(smimeMessage));
        return smime;
    }

    public SocketMessageDto setSMIME(SMIMEMessage smimeMessage) throws Exception {
        this.smime = smimeMessage;
        this.smimeMessage = Base64.getEncoder().encodeToString(smimeMessage.getBytes());
        return this;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public SocketMessageContentDto getContent() {
        return content;
    }

    public void setContent(SocketMessageContentDto content) {
        this.content = content;
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

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public Set<Currency> getCurrencySet() throws Exception {
        if(currencySet == null && currencyDtoList != null) currencySet = CurrencyDto.deSerialize(currencyDtoList);
        return currencySet;
    }

    public void setCurrencySet(Set<Currency> currencySet) {
        this.currencySet = currencySet;
    }

    public String getAesParams() {
        return aesParams;
    }

    public void setAesParams(String aesParams) {
        this.aesParams = aesParams;
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

    public AESParams getAesEncryptParams() {
        return aesEncryptParams;
    }

    public void setAesEncryptParams(AESParams aesEncryptParams) {
        this.aesEncryptParams = aesEncryptParams;
    }

    public DeviceVSDto getConnectedDevice() {
        return connectedDevice;
    }

    public void setConnectedDevice(DeviceVSDto connectedDevice) {
        this.connectedDevice = connectedDevice;
    }

    public static SocketMessageDto getSignRequest(DeviceVS deviceTo, String toUser, String textToSign, String subject)
            throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, null, TypeVS.MESSAGEVS_SIGN);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(deviceTo.getId());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getSignRequest(toUser, textToSign, subject);
        String aesParams = JSON.getMapper().writeValueAsString(socketSession.getAESParams().getDto());
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(aesParams.getBytes(), deviceTo.getX509Certificate());
        socketMessageDto.setAesParams(new String(base64EncryptedAESDataRequestBytes));
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(JSON.getMapper().writeValueAsString(messageContentDto),
                socketSession.getAESParams()));
        return socketMessageDto;
    }

    public static SocketMessageDto getCurrencyWalletChangeRequest(DeviceVS deviceTo, List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, currencyList, TypeVS.CURRENCY_WALLET_CHANGE);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setTimeLimited(true);
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setDeviceToId(deviceTo.getId());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getCurrencyWalletChangeRequest(currencyList);
        String aesParams = JSON.getMapper().writeValueAsString(socketSession.getAESParams().getDto());
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(aesParams.getBytes(), deviceTo.getX509Certificate());
        socketMessageDto.setAesParams(new String(base64EncryptedAESDataRequestBytes));
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(JSON.getMapper().writeValueAsString(messageContentDto),
                socketSession.getAESParams()));
        return socketMessageDto;
    }

    public static SocketMessageDto getMessageVSToDevice(UserVS userVS, DeviceVS deviceTo, String toUser,
                            String textToEncrypt) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceTo, null, TypeVS.MESSAGEVS);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(deviceTo.getId());
        socketMessageDto.setDeviceToName(deviceTo.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());

        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getMessageVSToDevice(userVS, toUser, textToEncrypt);
        String aesParams = JSON.getMapper().writeValueAsString(socketSession.getAESParams().getDto());
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(aesParams.getBytes(), deviceTo.getX509Certificate());
        socketMessageDto.setAesParams(new String(base64EncryptedAESDataRequestBytes));
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(
                JSON.getMapper().writeValueAsString(messageContentDto), socketSession.getAESParams()));
        return socketMessageDto;
    }

    //method to response a message previously received
    public SocketMessageDto getMessageVSResponse(UserVS userVS, String textToEncrypt) throws Exception {
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        WebSocketSession socketSession = ContextVS.getInstance().getWSSession(UUID);
        socketMessageDto.setDeviceToId(deviceFromId);
        socketMessageDto.setDeviceToName(from);
        socketMessageDto.setUUID(socketSession.getUUID());

        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getMessageVSToDevice(userVS, from, textToEncrypt);
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(
                JSON.getMapper().writeValueAsString(messageContentDto), socketSession.getAESParams()));
        return socketMessageDto;
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        byte[] decryptedBytes = Encryptor.decryptCMS(aesParams.getBytes(), privateKey);
        decryptMessage(AESParams.fromDto(JSON.getMapper().readValue(new String(decryptedBytes), AESParamsDto.class)));
    }

    public void decryptMessage(AESParams aesParams) throws Exception {
        this.aesEncryptParams = aesParams;
        content = JSON.getMapper().readValue(
                Encryptor.decryptAES(encryptedMessage, aesParams), SocketMessageContentDto.class);
        if(content.getOperation() != null) {
            this.messageType = operation;
            operation = content.getOperation();
        }
        if(content.getStatusCode() != null) statusCode = content.getStatusCode();
        if(content.getDeviceFromName() != null) deviceFromName = content.getDeviceFromName();
        if(content.getFrom() != null) from = content.getFrom();
        if(content.getDeviceFromId() != null) deviceFromId = content.getDeviceFromId();
        if(content.getSmimeMessage() != null) smime = content.getSMIME();
        if(content.getCurrencyList() != null) currencySet = CurrencyDto.deSerialize(
                content.getCurrencyList());
        if(content.getSubject() != null) subject = content.getSubject();
        if(content.getMessage() != null) message = content.getMessage();
        if(content.getToUser() != null) toUser = content.getToUser();
        if(content.getDeviceToName() != null) deviceToName = content.getDeviceToName();
        if(content.getURL()!= null) URL = content.getURL();
        if(content.getTextToSign() != null) textToSign = content.getTextToSign();
        if(content.getLocale() != null) locale = content.getLocale();
        this.encryptedMessage = null;
    }

    public static <T> WebSocketSession checkWebSocketSession (DeviceVS deviceTo, T data, TypeVS typeVS)
            throws NoSuchAlgorithmException {
        WebSocketSession webSocketSession = null;
        if(deviceTo != null) webSocketSession = ContextVS.getInstance().getWSSession(deviceTo.getId());
        if(webSocketSession == null) {
            String randomUUID = java.util.UUID.randomUUID().toString();
            AESParams aesParams = new AESParams();
            webSocketSession = new WebSocketSession(aesParams, deviceTo, null, null);
            ContextVS.getInstance().putWSSession(randomUUID, webSocketSession);
        }
        webSocketSession.setData(data);
        webSocketSession.setTypeVS(typeVS);
        return webSocketSession;
    }
}