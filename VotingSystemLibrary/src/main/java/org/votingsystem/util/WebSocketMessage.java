package org.votingsystem.util;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.Encryptor;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketMessage {

    private static Logger log = Logger.getLogger(WebSocketMessage.class);

    public enum ConnectionStatus {OPEN, CLOSED}

    private String sessionId;
    private String message;
    private String URL;
    private UserVS userVS;
    private Integer statusCode;
    private TypeVS operation;
    private JSONObject messageJSON;

    public WebSocketMessage(JSONObject requestJSON) {
        this.messageJSON = requestJSON;
        setSessionId(requestJSON.getString("sessionId"));
        if(requestJSON.has("operation")) this.operation = TypeVS.valueOf(requestJSON.getString("operation"));
        if(requestJSON.has("statusCode")) this.statusCode = requestJSON.getInt("statusCode");
        if(requestJSON.has("URL")) this.URL = requestJSON.getString("URL");
        if(requestJSON.has("message")) {
            Object messageObject = requestJSON.get("message");
            if(messageObject instanceof  JSONObject) {
                JSONObject messageJSON = requestJSON.getJSONObject("message");
                this.statusCode = messageJSON.getInt("statusCode");
                this.operation = TypeVS.valueOf(messageJSON.getString("operation"));
                if(messageJSON.has("message")) this.message = messageJSON.getString("message");
                if(messageJSON.has("URL")) this.URL = messageJSON.getString("URL");
            } else this.message = requestJSON.getString("message");
        }
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getWebSocketCoreSignalJSCommand(ConnectionStatus status) {
        return getWebSocketCoreSignalJSCommand(messageJSON, status);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public SMIMEMessage getSignResponse(WebSocketUtils.RequestBundle request) throws Exception {
        if(messageJSON.has("encryptedMessage")) {
            byte[] decryptedBytes = Encryptor.decryptCMS(messageJSON.getString("encryptedMessage").getBytes(),
                    request.getKeyPair().getPrivate());
            JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(new String(decryptedBytes, "UTF-8"));
            if(ResponseVS.SC_OK == responseJSON.getInt("statusCode")) {
                byte[] smimeMessageBytes = Base64.getDecoder().decode(responseJSON.getString("smimeMessage").getBytes());
                SMIMEMessage smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
                return smimeMessage;
            } else throw new ExceptionVS(message);
        } else throw new ExceptionVS(message);
    }

    public static String getWebSocketCoreSignalJSCommand(JSONObject messageJSON, ConnectionStatus status) {
        JSONObject coreSignal = new JSONObject();
        if(messageJSON == null) messageJSON = new JSONObject();
        messageJSON.put("socketStatus", status.toString());
        //this.fire('core-signal', {name: "vs-websocket-message", data: messageJSON});
        coreSignal.put("name", "vs-websocket-message");
        coreSignal.put("data", messageJSON);
        String jsCommand = null;
        try {
            jsCommand = "fireCoreSignal('" + Base64.getEncoder().encodeToString(
                    coreSignal.toString().getBytes("UTF-8")) + "')";

        } catch (UnsupportedEncodingException ex) {
            log.error(ex.getMessage(), ex);
        }
        return jsCommand;
    }

    public JSONObject getMessageJSON() {
        return messageJSON;
    }

    public void setMessageJSON(JSONObject messageJSON) {
        this.messageJSON = messageJSON;
    }
}
