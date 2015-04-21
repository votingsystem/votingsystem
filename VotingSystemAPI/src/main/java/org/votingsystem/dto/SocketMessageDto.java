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
import org.votingsystem.util.*;

import javax.mail.Header;
import javax.websocket.Session;
import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageDto {

    public List<CurrencyDto> getCurrencyDtoList() {
        return currencyDtoList;
    }

    public void setCurrencyDtoList(List<CurrencyDto> currencyDtoList) {
        this.currencyDtoList = currencyDtoList;
    }

    public enum State {PENDING, PROCESSED, LAPSED, REMOVED}
    public enum ConnectionStatus {OPEN, CLOSED}

    private TypeVS operation;
    private State state = State.PENDING;
    private Integer statusCode;
    private String deviceFromId;
    private String deviceToId;
    private String sessionId;
    private String message;
    private String encryptedMessage;
    private String UUID;
    private String locale;
    private String remoteAddress;
    private String smimeMessage;
    private String aesParams;

    private SocketMessageContentDto content;

    private String from;
    private Long deviceId;
    private boolean timeLimited = false;
    private Boolean isEncrypted;
    private String deviceFromName;
    private String deviceToName;
    private String URL;
    private List<CurrencyDto> currencyDtoList;
    private Date date;
    @JsonIgnore private UserVS userVS;
    @JsonIgnore private Set<Currency> currencySet;
    @JsonIgnore private AESParams aesEncryptParams;
    @JsonIgnore private WebSocketSession webSocketSession;
    @JsonIgnore private Session session;
    @JsonIgnore private SessionVS sessionVS;
    @JsonIgnore private SMIMEMessage smime;

    public SocketMessageDto () {}

    public SocketMessageDto getResponse(Integer statusCode, String message){
        SocketMessageDto responseDto = new SocketMessageDto();
        responseDto.setStatusCode(statusCode);
        responseDto.setMessage(message);
        responseDto.setSessionId(session.getId());
        responseDto.setOperation(TypeVS.MESSAGEVS_FROM_VS);
        responseDto.setUUID(UUID);
        return responseDto;
    }

    public SocketMessageDto getErrorResponse(String message){
        this.statusCode = ResponseVS.SC_ERROR;
        this.message = message;
        return this;
    }

    public static SocketMessageDto INIT_SESSION_REQUEST(String deviceFromId) throws NoSuchAlgorithmException {
        WebSocketSession socketSession = checkWebSocketSession(null, null, TypeVS.INIT_VALIDATED_SESSION);
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setUUID(socketSession.getUUID());
        messageDto.setLocale(ContextVS.getInstance().getLocale().getLanguage());
        messageDto.setOperation(socketSession.getTypeVS());
        messageDto.setDeviceFromId(deviceFromId);
        return messageDto;
    }

    public SocketMessageDto clone() {
        SocketMessageDto dto = new SocketMessageDto();
        dto.setOperation(operation);
        dto.setDeviceFromId(deviceFromId);
        dto.setSmimeMessage(smimeMessage);
        dto.setLocale(locale);
        dto.setUUID(UUID);
        return dto;
    }

    public boolean isEncrypted() {
        return encryptedMessage != null;
    }

    public String getDeviceFromId() {
        return deviceFromId;
    }

    public void setDeviceFromId(String deviceFromId) {
        this.deviceFromId = deviceFromId;
    }

    public String getDeviceToId() {
        return deviceToId;
    }

    public void setDeviceToId(String deviceToId) {
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

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public SessionVS getSessionVS() {
        return sessionVS;
    }

    public void setSessionVS(SessionVS sessionVS) {
        this.sessionVS = sessionVS;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session, SessionVS sessionVS) throws ValidationExceptionVS {
        if(operation == null) throw new ValidationExceptionVS("missing param 'operation'");
        /*if(TypeVS.MESSAGEVS_SIGN == operation && deviceId == null) {
            throw new ValidationExceptionVS("missing message 'deviceId'");
        }*/
        /*this.remoteAddress = ((String)((AbstractServletOutputStream)((WsRemoteEndpointImplServer)((WsRemoteEndpointAsync)
                ((WsSession)session).remoteEndpointAsync).base).sos).socketWrapper.getRemoteAddr());*/
        this.session = session;
        this.sessionVS = sessionVS;
        if(sessionId == null) sessionId = session.getId();
        //Locale.forLanguageTag(locale)
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
        if(smime == null) smime = new SMIMEMessage(new ByteArrayInputStream(Base64.getDecoder().decode(smimeMessage)));
        return smime;
    }

    public SocketMessageDto setSMIME(SMIMEMessage smimeMessage) throws Exception {
        this.smime = smimeMessage;
        setSmimeMessage(Base64.getEncoder().encodeToString(smimeMessage.getBytes()));
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

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
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
        if(currencySet == null && currencyDtoList != null) currencySet = CurrencyDto.deSerializeCollection(currencyDtoList);
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
    }

    public void setSession(Session session) {
        this.session = session;
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

    public SocketMessageDto createResponseFromDevice(Integer statusCode, String message) throws Exception {
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setSessionId(sessionId);
        messageDto.setOperation(TypeVS.MESSAGEVS_FROM_DEVICE);
        messageDto.setLocale(ContextVS.getInstance().getLocale().getLanguage());
        messageDto.setUUID(UUID);
        if(aesParams != null) {
            SocketMessageContentDto socketMessageContentDto = new SocketMessageContentDto(operation, statusCode, message, null);
            messageDto.setEncryptedMessage(Encryptor.encryptAES(
                    JSON.getMapper().writeValueAsString(socketMessageContentDto), aesEncryptParams));
        } else {
            messageDto.setOperation(operation);
            messageDto.setStatusCode(statusCode);
            messageDto.setMessage(message);
        }
        return messageDto;
    }

    public static SocketMessageDto getSignRequest(DeviceVS deviceVS, String toUser, String textToSign, String subject ,
                     Header... headers) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, null, TypeVS.MESSAGEVS_SIGN);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(deviceVS.getId().toString());
        socketMessageDto.setDeviceToName(deviceVS.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getSignRequest(deviceVS, toUser, textToSign,
                subject, headers);
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                socketSession.getAESParams().toMap().toString().getBytes(), deviceVS.getX509Certificate());
        socketMessageDto.setAesParams(new String(base64EncryptedAESDataRequestBytes));
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(JSON.getMapper().writeValueAsString(messageContentDto),
                socketSession.getAESParams()));
        return socketMessageDto;
    }

    public static SocketMessageDto getCurrencyWalletChangeRequest(DeviceVS deviceVS, List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, currencyList, TypeVS.CURRENCY_WALLET_CHANGE);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setTimeLimited(true);
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setDeviceToId(deviceVS.getId().toString());
        socketMessageDto.setDeviceToName(deviceVS.getDeviceName());
        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getCurrencyWalletChangeRequest(currencyList);

        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                socketSession.getAESParams().toMap().toString().getBytes(), deviceVS.getX509Certificate());
        socketMessageDto.setAesParams(new String(base64EncryptedAESDataRequestBytes));
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(JSON.getMapper().writeValueAsString(messageContentDto),
                socketSession.getAESParams()));
        return socketMessageDto;
    }

    public static SocketMessageDto getMessageVSToDevice(UserVS userVS, DeviceVS deviceVS, String toUser,
                            String textToEncrypt) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, null, TypeVS.MESSAGEVS);
        SocketMessageDto socketMessageDto = new SocketMessageDto();
        socketMessageDto.setOperation(TypeVS.MESSAGEVS_TO_DEVICE);
        socketMessageDto.setStatusCode(ResponseVS.SC_PROCESSING);
        socketMessageDto.setDeviceToId(deviceVS.getId().toString());
        socketMessageDto.setDeviceToName(deviceVS.getDeviceName());
        socketMessageDto.setUUID(socketSession.getUUID());
        socketMessageDto.setLocale(ContextVS.getInstance().getLocale().getLanguage());

        SocketMessageContentDto messageContentDto = SocketMessageContentDto.getMessageVSToDevice(userVS, toUser, textToEncrypt);
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                socketSession.getAESParams().toMap().toString().getBytes(), deviceVS.getX509Certificate());
        socketMessageDto.setAesParams(new String(base64EncryptedAESDataRequestBytes));
        socketMessageDto.setEncryptedMessage(Encryptor.encryptAES(
                JSON.getMapper().writeValueAsString(messageContentDto), socketSession.getAESParams()));
        return socketMessageDto;
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        byte[] decryptedBytes = Encryptor.decryptCMS(aesParams.getBytes(), privateKey);
        this.aesEncryptParams = AESParams.load(JSON.getMapper().readValue(new String(decryptedBytes),
                new TypeReference<HashMap<String, Object>>() { }));
        decryptMessage(this.aesEncryptParams);
    }

    public void decryptMessage(AESParams aesParams) throws Exception {
        SocketMessageContentDto messageContentDto = JSON.getMapper().readValue(
                Encryptor.decryptAES(encryptedMessage, aesParams), SocketMessageContentDto.class);
        if(messageContentDto.getSmimeMessage() != null) smime = messageContentDto.getSMIME();
        if(messageContentDto.getCurrencyList() != null) currencySet = CurrencyDto.deSerializeCollection(
                messageContentDto.getCurrencyList());
        this.isEncrypted = false;
        ContextVS.getInstance().putWSSession(UUID, new WebSocketSession<>(
                aesParams, new DeviceVS(Long.valueOf(deviceFromId), deviceFromName), null, operation));
    }

    private static <T> WebSocketSession checkWebSocketSession (DeviceVS deviceVS, T data, TypeVS typeVS)
            throws NoSuchAlgorithmException {
        WebSocketSession webSocketSession = null;
        if(deviceVS != null) webSocketSession = ContextVS.getInstance().getWSSession(deviceVS.getId());
        if(webSocketSession == null) {
            String randomUUID = java.util.UUID.randomUUID().toString();
            AESParams aesParams = new AESParams();
            webSocketSession = new WebSocketSession(aesParams, deviceVS, null, null);
            ContextVS.getInstance().putWSSession(randomUUID, webSocketSession);
        }
        webSocketSession.setData(data);
        webSocketSession.setTypeVS(typeVS);
        return webSocketSession;
    }
}