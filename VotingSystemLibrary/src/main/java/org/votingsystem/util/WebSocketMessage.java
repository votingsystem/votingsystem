package org.votingsystem.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSException;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketMessage {

    private static Logger log = Logger.getLogger(WebSocketMessage.class);

    public enum ConnectionStatus {OPEN, CLOSED}

    private String sessionId;
    private String deviceFromName;
    private String message;
    private String URL;
    private UserVS userVS;
    private Integer statusCode;
    private TypeVS operation;
    private JSONObject messageJSON;
    private List<Cooin> cooinList;
    private PublicKey receiverPublic;
    private Date date;

    public WebSocketMessage(JSONObject requestJSON) throws ParseException {
        this.messageJSON = requestJSON;
        setSessionId(requestJSON.getString("sessionId"));
        if(requestJSON.has("operation")) this.operation = TypeVS.valueOf(requestJSON.getString("operation"));
        if(requestJSON.has("statusCode")) this.statusCode = requestJSON.getInt("statusCode");
        if(requestJSON.has("URL")) this.URL = requestJSON.getString("URL");
        if(requestJSON.has("date")) this.date = DateUtils.getDayWeekDate(requestJSON.getString("date"));
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
            } else throw new ExceptionVS(responseJSON.getString("message"));
        } else throw new ExceptionVS(message);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        if(messageJSON.has("encryptedMessage")) {
            byte[] decryptedBytes = Encryptor.decryptCMS(messageJSON.getString("encryptedMessage").getBytes(),
                    privateKey);
            JSONObject decryptedJSON = (JSONObject) JSONSerializer.toJSON(new String(decryptedBytes, "UTF-8"));
            if(decryptedJSON.has("operation")) operation = TypeVS.valueOf(decryptedJSON.getString("operation"));
            if(decryptedJSON.has("deviceFromName")) deviceFromName = decryptedJSON.getString("deviceFromName");
            if(decryptedJSON.has("cooinList")) {
                JSONArray cooinArray = decryptedJSON.getJSONArray("cooinList");
                cooinList = new ArrayList<Cooin>();
                for(int i = 0; i < cooinArray.size(); i ++) {
                    JSONObject cooinJSON = (JSONObject) cooinArray.get(i);
                    CertificationRequestVS certificationRequest = (CertificationRequestVS)ObjectUtils.deSerializeObject(
                            ((String) cooinJSON.get("certificationRequest")).getBytes());
                    cooinList.add(Cooin.load(certificationRequest));
                }
            }
            if(decryptedJSON.has("publicKey")) {
                byte[] decodedPK = Base64.getDecoder().decode(decryptedJSON.getString("publicKey"));
                receiverPublic = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
            }
        } else throw new ExceptionVS("missing encryptedMessage");
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

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public JSONObject getMessageJSON() {
        if(date != null) messageJSON.put("date", DateUtils.getDayWeekDateStr(date));
        return messageJSON;
    }

    public void setMessageJSON(JSONObject messageJSON) {
        this.messageJSON = messageJSON;
    }
}
