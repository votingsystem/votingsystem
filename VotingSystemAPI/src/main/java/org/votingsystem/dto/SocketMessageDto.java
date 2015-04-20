package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.SessionVS;
import org.votingsystem.util.TypeVS;

import javax.websocket.Session;
import java.io.ByteArrayInputStream;
import java.util.Base64;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SocketMessageDto {

    private TypeVS operation;
    private Integer statusCode;
    private String deviceFromId;
    private String deviceToId;
    private String sessionId;
    private String message;
    private String UUID;
    private String locale;
    private String remoteAddress;
    private String smimeMessage;

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

    public static SocketMessageDto INIT_SESSION_REQUEST(String deviceFromId) {
        SocketMessageDto messageDto = new SocketMessageDto();
        messageDto.setUUID(java.util.UUID.randomUUID().toString());
        messageDto.setOperation(TypeVS.INIT_VALIDATED_SESSION);
        messageDto.setDeviceFromId(deviceFromId);
        return messageDto;
    }

    public SocketMessageDto getInitConnectionMsg(SMIMEMessage smimeMessage) throws Exception {
        setLocale(ContextVS.getInstance().getLocale().getLanguage());
        setSmimeMessage(Base64.getEncoder().encodeToString(smimeMessage.getBytes()));
        return this;
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

    public SMIMEMessage getSmime() throws Exception {
        if(smime == null) smime = new SMIMEMessage(new ByteArrayInputStream(Base64.getDecoder().decode(smimeMessage)));
        return smime;
    }

    public void setSmime(SMIMEMessage smime) {
        this.smime = smime;
    }
}
