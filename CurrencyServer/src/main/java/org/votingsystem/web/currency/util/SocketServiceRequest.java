package org.votingsystem.web.currency.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.currency.websocket.SessionVS;
import org.votingsystem.web.currency.websocket.SessionVSManager;

import javax.websocket.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SocketServiceRequest {

    private static Logger log = Logger.getLogger(SocketServiceRequest.class.getSimpleName());

    private long deviceFromId = -1L;
    private long deviceToId;
    private Session session;
    private String sessionId;
    private ObjectNode dataJSON;
    private SessionVS sessionVS;
    private Locale locale;
    private TypeVS operation;
    private SMIMEMessage smimeMessage;
    private String remoteAddress;

    public SocketServiceRequest(Session session, String msg, boolean last) throws IOException, ExceptionVS {
        /*this.remoteAddress = ((String)((AbstractServletOutputStream)((WsRemoteEndpointImplServer)((WsRemoteEndpointAsync)
                ((WsSession)session).remoteEndpointAsync).base).sos).socketWrapper.getRemoteAddr());*/
        this.setSession(session);
        setJsonData((ObjectNode) new ObjectMapper().readTree(msg));
        setDeviceFromId(dataJSON.get("deviceFromId").asLong());
        if(dataJSON.get("deviceToId") != null)  setDeviceToId(dataJSON.get("deviceToId").asLong());
        if(dataJSON.get("sessionId") == null)  dataJSON.put("sessionId", session.getId());
        sessionId = dataJSON.get("sessionId").asText();
        if(dataJSON.get("locale") != null) setLocale(Locale.forLanguageTag(dataJSON.get("locale").asText()));
        if(dataJSON.get("operation") == null)  throw new ExceptionVS("missing message 'operation'");
        setOperation(TypeVS.valueOf(dataJSON.get("operation").asText()));
        if(TypeVS.MESSAGEVS_SIGN == getOperation()) {
            if(dataJSON.get("deviceId") == null) throw new ExceptionVS("missing message 'deviceId'");
        }
        setSessionVS(SessionVSManager.getInstance().getAuthenticatedSession(session));
        log.info("session id:" + session.getId()  + " - operation: " + dataJSON.get("operation").asText() +
                "remoteIp:");
    }

    public Map getResponse(Integer statusCode, String message){
        Map result = new HashMap<>();
        result.put("statusCode", statusCode);
        result.put("message", message);
        result.put("sessionId", getSession().getId());
        result.put("operation", TypeVS.MESSAGEVS_FROM_VS.toString());
        result.put("UUID", dataJSON.get("UUID").asText());
        return result;
    }

    public static Map getResponse(Integer statusCode, String message, Session session, String requestMsg){
        String requestUUID = null;
        try {
            requestUUID = new ObjectMapper().readTree(requestMsg).get("UUID").asText();
        } catch(Exception ex) {log.info("requestMsg not in JSON format");}
        Map result = new HashMap<>();
        result.put("operation", TypeVS.MESSAGEVS_FROM_VS.toString());
        if(requestUUID != null) result.put("UUID", requestUUID);
        if(statusCode != null) result.put("statusCode", statusCode);
        if(message != null) result.put("message", message);
        if(session.getId() != null) result.put("sessionId", session.getId());
        return result;
    }

    public Map getResponse(Integer statusCode, String message, Long deviceId){
        Map result = new HashMap<>();
        result.put("statusCode", statusCode);
        result.put("message", message);
        result.put("deviceId", deviceId);
        result.put("operation", TypeVS.MESSAGEVS_FROM_VS.toString());
        result.put("sessionId", dataJSON.get("sessionId").asLong());
        result.put("UUID", dataJSON.get("UUID").asText());
        return result;
    }

    public SMIMEMessage getSMIME() throws Exception {
        if(getSmimeMessage() == null) setSmimeMessage(new SMIMEMessage(new ByteArrayInputStream(
                Base64.getDecoder().decode(dataJSON.get("smimeMessage").asText()))));
        return getSmimeMessage();
    }

    public long getDeviceFromId() {
        return deviceFromId;
    }

    public void setDeviceFromId(long deviceFromId) {
        this.deviceFromId = deviceFromId;
    }

    public long getDeviceToId() {
        return deviceToId;
    }

    public void setDeviceToId(long deviceToId) {
        this.deviceToId = deviceToId;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public ObjectNode getJsonData() {
        return dataJSON;
    }

    public void setJsonData(ObjectNode dataJSON) {
        this.dataJSON = dataJSON;
    }

    public SessionVS getSessionVS() {
        return sessionVS;
    }

    public void setSessionVS(SessionVS sessionVS) {
        this.sessionVS = sessionVS;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public SMIMEMessage getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(SMIMEMessage smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
