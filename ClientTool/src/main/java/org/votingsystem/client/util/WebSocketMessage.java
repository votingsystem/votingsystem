package org.votingsystem.client.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyGeneratorVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;

import javax.mail.Header;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketMessage {

    private static Logger log = Logger.getLogger(WebSocketMessage.class);

    public static final int TIME_LIMITED_MESSAGE_LIVE = 30; //seconds

    public enum State {PENDING, PROCESSED, LAPSED}
    public enum ConnectionStatus {OPEN, CLOSED}

    private String sessionId;
    private boolean timeLimited = false;
    private State state = State.PENDING;
    private String locale;
    private String UUID;
    private String deviceFromName;
    private String message;
    private String URL;
    private UserVS userVS;
    private Integer statusCode;
    private TypeVS operation;
    private JSONObject messageJSON;
    private List<Cooin> cooinList;
    private AESParams aesParams;
    private SMIMEMessage smimeMessage;
    private Date date;


    public WebSocketMessage(JSONObject requestJSON) throws ParseException, NoSuchAlgorithmException {
        this.messageJSON = requestJSON;
        setSessionId(requestJSON.getString("sessionId"));
        if(requestJSON.has("operation")) this.operation = TypeVS.valueOf(requestJSON.getString("operation"));
        if(requestJSON.has("timeLimited")) this.timeLimited = requestJSON.getBoolean("timeLimited");
        if(requestJSON.has("statusCode")) this.statusCode = requestJSON.getInt("statusCode");
        if(requestJSON.has("URL")) this.URL = requestJSON.getString("URL");
        if(requestJSON.has("locale")) this.locale = requestJSON.getString("locale");
        if(requestJSON.has("UUID")) this.setUUID(requestJSON.getString("UUID"));
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
        if(messageJSON.has("aesParams")) {
            aesParams = AESParams.load(messageJSON.getJSONObject("aesParams"));
        }
    }

    public boolean isEncrypted() {
        return messageJSON.has("encryptedMessage");
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<Cooin> getCooinList() {
        return cooinList;
    }

    public void setCooinList(List<Cooin> cooinList) {
        this.cooinList = cooinList;
    }

    public State getState() {
        return state;
    }

    public WebSocketMessage setState(State state) {
        this.state = state;
        return this;
    }

    public SMIMEMessage getSMIME() {
        return smimeMessage;
    }

    public String getLocale() {
        return locale;
    }

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public void setAESParams(AESParams aesParams) {
        this.aesParams = aesParams;
    }

    public JSONObject getResponse(Integer statusCode, String message) throws Exception {
        Map result = new HashMap();
        result.put("sessionId", sessionId);
        result.put("operation", TypeVS.MESSAGEVS_FROM_DEVICE.toString());
        result.put("locale", ContextVS.getInstance().getLocale().getLanguage());
        result.put("UUID", UUID);
        if(aesParams != null) {
            Map dataToEncrypt = new HashMap();
            dataToEncrypt.put("statusCode", statusCode);
            dataToEncrypt.put("message", message);
            dataToEncrypt.put("operation", operation.toString());
            result.put("encryptedMessage", Encryptor.encryptAES(
                    JSONSerializer.toJSON(dataToEncrypt).toString(), aesParams));
        } else {
            result.put("operation", operation.toString());
            result.put("statusCode", statusCode);
            result.put("message", message);
        }
        return (JSONObject) JSONSerializer.toJSON(result);
    }

    public static JSONObject getSignRequest(Long deviceIdTo, String deviceIdFrom, String deviceName,
                        byte[] encryptedBytes, String locale) {
        Map messageToServiceMap = new HashMap<>();
        messageToServiceMap.put("locale", locale);
        messageToServiceMap.put("operation", TypeVS.MESSAGEVS_SIGN.toString());
        messageToServiceMap.put("deviceIdFrom", deviceIdFrom);
        messageToServiceMap.put("deviceIdTo", deviceIdTo);
        messageToServiceMap.put("deviceName", deviceName);
        messageToServiceMap.put("encryptedMessage", Base64.getEncoder().encodeToString(encryptedBytes));
        JSONObject messageToServiceJSON = (JSONObject) JSONSerializer.toJSON(messageToServiceMap);
        return messageToServiceJSON;
    }

    public static JSONObject getSignRequest(Long deviceToId, String deviceToName, String deviceFromName,
               String toUser, String textToSign, String subject, String locale, X509Certificate deviceToCert,
               Header... headers) throws Exception {
        Map messageToDevice = new HashMap<>();
        String randomUUID = java.util.UUID.randomUUID().toString();
        messageToDevice.put("operation", TypeVS.MESSAGEVS_TO_DEVICE.toString());
        messageToDevice.put("deviceToId", deviceToId);
        messageToDevice.put("deviceToName", deviceToName);
        messageToDevice.put("locale", locale);
        Map encryptedDataMap =  new HashMap<>();
        encryptedDataMap.put("operation", TypeVS.MESSAGEVS_SIGN.toString());
        encryptedDataMap.put("deviceFromName", deviceFromName);
        encryptedDataMap.put("toUser", toUser);
        encryptedDataMap.put("textToSign", textToSign);
        encryptedDataMap.put("subject", subject);
        messageToDevice.put("UUID", randomUUID);
        if(headers != null) {
            JSONArray headersArray = new JSONArray();
            for(Header header : headers) {
                if (header != null) {
                    JSONObject headerJSON = new JSONObject();
                    headerJSON.put("name", header.getName());
                    headerJSON.put("value", header.getValue());
                    headersArray.add(headerJSON);
                }
            }
            encryptedDataMap.put("headers", headersArray);
        }
        AESParams aesParams = new AESParams();
        VotingSystemApp.getInstance().putSessionKey(randomUUID, aesParams);
        encryptedDataMap.put("aesParams", aesParams.toJSON());
        byte[] encryptedRequestBytes = Encryptor.encryptToCMS(
                JSONSerializer.toJSON(encryptedDataMap).toString().getBytes(), deviceToCert);
        messageToDevice.put("encryptedMessage", new String(encryptedRequestBytes,"UTF-8"));
        return (JSONObject) JSONSerializer.toJSON(messageToDevice);
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        if(messageJSON.has("encryptedMessage")) {
            byte[] decryptedBytes = Encryptor.decryptCMS(messageJSON.getString("encryptedMessage").getBytes(),
                    privateKey);
            loadDecryptedContent((JSONObject) JSONSerializer.toJSON(new String(decryptedBytes, "UTF-8")));
        } else throw new ExceptionVS("missing encryptedMessage");
    }

    public void decryptMessage(AESParams aesParams) throws Exception {
        if(messageJSON.has("encryptedMessage")) {
            loadDecryptedContent((JSONObject) JSONSerializer.toJSON(Encryptor.decryptAES(
                    messageJSON.getString("encryptedMessage"), aesParams)));
        } else throw new ExceptionVS("missing encryptedMessage");
    }

    private void loadDecryptedContent(JSONObject decryptedJSON) throws Exception {
        if(decryptedJSON.has("operation")) operation = TypeVS.valueOf(decryptedJSON.getString("operation"));
        if(decryptedJSON.has("statusCode")) statusCode = decryptedJSON.getInt("statusCode");
        if(decryptedJSON.has("deviceFromName")) deviceFromName = decryptedJSON.getString("deviceFromName");
        if(decryptedJSON.has("smimeMessage")) {
            byte[] smimeMessageBytes = Base64.getDecoder().decode(decryptedJSON.getString("smimeMessage").getBytes());
            smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
        }
        if(decryptedJSON.has("cooinList")) {
            JSONArray cooinArray = decryptedJSON.getJSONArray("cooinList");
            setCooinList(new ArrayList<Cooin>());
            for(int i = 0; i < cooinArray.size(); i ++) {
                JSONObject cooinJSON = (JSONObject) cooinArray.get(i);
                CertificationRequestVS certificationRequest = (CertificationRequestVS) ObjectUtils.deSerializeObject(
                        ((String) cooinJSON.get("certificationRequest")).getBytes());
                getCooinList().add(Cooin.load(certificationRequest));
            }
        }
        if(decryptedJSON.has("aesParams")) {
            this.aesParams = AESParams.load(decryptedJSON.getJSONObject("aesParams"));
        }
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
        } catch (UnsupportedEncodingException ex) { log.error(ex.getMessage(), ex); }
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