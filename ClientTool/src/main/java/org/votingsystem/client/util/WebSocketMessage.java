package org.votingsystem.client.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSONUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.Wallet;

import javax.mail.Header;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.text.ParseException;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketMessage {

    private static Logger log = Logger.getLogger(WebSocketMessage.class);

    public enum State {PENDING, PROCESSED, LAPSED, REMOVED}
    public enum ConnectionStatus {OPEN, CLOSED}

    private String sessionId;
    private String from;
    private Long deviceId;
    private Long deviceFromId;
    private boolean timeLimited = false;
    private Boolean isEncrypted;
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
    private Set<Cooin> cooinSet;
    private AESParams aesParams;
    private SMIMEMessage smimeMessage;
    private Date date;
    private WebSocketSession webSocketSession;


    public WebSocketMessage(JSONObject socketMsgJSON) throws ParseException, NoSuchAlgorithmException {
        this.messageJSON = socketMsgJSON;
        if(socketMsgJSON.has("sessionId")) this.sessionId = socketMsgJSON.getString("sessionId");
        if(socketMsgJSON.has("operation")) this.operation = TypeVS.valueOf(socketMsgJSON.getString("operation"));
        if(socketMsgJSON.has("timeLimited")) this.timeLimited = socketMsgJSON.getBoolean("timeLimited");
        if(socketMsgJSON.has("statusCode")) this.statusCode = socketMsgJSON.getInt("statusCode");
        if(!JSONUtils.isEmpty("deviceId", socketMsgJSON)) this.deviceId = socketMsgJSON.getLong("deviceId");
        if(socketMsgJSON.has("URL")) this.URL = socketMsgJSON.getString("URL");
        if(socketMsgJSON.has("locale")) this.locale = socketMsgJSON.getString("locale");
        if(socketMsgJSON.has("from")) this.from = socketMsgJSON.getString("from");
        if(socketMsgJSON.has("deviceFromName")) this.deviceFromName = socketMsgJSON.getString("deviceFromName");
        if(socketMsgJSON.has("deviceFromId")) this.deviceFromId = socketMsgJSON.getLong("deviceFromId");
        if(socketMsgJSON.has("UUID")) this.setUUID(socketMsgJSON.getString("UUID"));
        if(socketMsgJSON.has("date")) this.date = DateUtils.getDateFromString(socketMsgJSON.getString("date"));
        if(socketMsgJSON.has("isEncrypted")) this.isEncrypted = socketMsgJSON.getBoolean("isEncrypted");
        if(socketMsgJSON.has("message")) {
            Object messageObject = socketMsgJSON.get("message");
            if(messageObject instanceof  JSONObject) {
                JSONObject messageJSON = socketMsgJSON.getJSONObject("message");
                this.statusCode = messageJSON.getInt("statusCode");
                this.operation = TypeVS.valueOf(messageJSON.getString("operation"));
                if(messageJSON.has("message")) this.message = messageJSON.getString("message");
                if(messageJSON.has("URL")) this.URL = messageJSON.getString("URL");
            } else this.message = socketMsgJSON.getString("message");
        }
        if(socketMsgJSON.has("smimeMessage")) {
            try {
                byte[] smimeMessageBytes = Base64.getDecoder().decode(socketMsgJSON.getString("smimeMessage").getBytes());
                smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
            }catch(Exception ex) {log.error(ex.getMessage(), ex);}
        }
        if(socketMsgJSON.has("cooinList")) {
            try {
                cooinSet = Wallet.getCooinSet(socketMsgJSON.getJSONArray("cooinList"));
            }catch(Exception ex) {log.error(ex.getMessage(), ex);}
        }
    }


    public void setWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public boolean isEncrypted() {
        if(isEncrypted != null) return isEncrypted;
        switch (statusCode) {
            case ResponseVS.SC_WS_CONNECTION_INIT_ERROR: return false;
            case ResponseVS.SC_WS_CONNECTION_INIT_OK: return false;
            default:
                switch(operation) {
                    case MESSAGEVS_FROM_VS: return false;
                    default: return true;
                }

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
        if(messageJSON != null) messageJSON.put("operation", operation);
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

    public Set<Cooin> getCooinSet() {
        return cooinSet;
    }

    public void setCooinSet(Set<Cooin> cooinSet) {
        this.cooinSet = cooinSet;
    }

    public State getState() {
        return state;
    }

    public WebSocketMessage setState(State state) {
        this.state = state;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
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

    public AESParams getAESParams() {
        return  aesParams;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getDeviceFromId() {
        return deviceFromId;
    }

    public void setDeviceFromId(Long deviceFromId) {
        this.deviceFromId = deviceFromId;
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

    public static JSONObject getAuthenticationRequest(SMIMEMessage smimeMessage, String UUID) throws Exception {
        Map messageToServiceMap = new HashMap<>();
        messageToServiceMap.put("operation", TypeVS.INIT_VALIDATED_SESSION);
        messageToServiceMap.put("deviceFromId", SessionService.getInstance().getDeviceId());
        messageToServiceMap.put("locale", ContextVS.getInstance().getLocale().getLanguage());
        messageToServiceMap.put("smimeMessage", Base64.getEncoder().encodeToString(smimeMessage.getBytes()));
        messageToServiceMap.put("UUID", UUID);
        VotingSystemApp.getInstance().putWSSession(UUID, new WebSocketSession<>(
                null, null, null, TypeVS.INIT_VALIDATED_SESSION));
        return (JSONObject) JSONSerializer.toJSON(messageToServiceMap);
    }

    public static JSONObject getSignRequest(DeviceVS deviceVS, String toUser, String textToSign, String subject ,
            Header... headers) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, null, TypeVS.MESSAGEVS_SIGN);
        Map messageToDevice = new HashMap<>();
        messageToDevice.put("operation", TypeVS.MESSAGEVS_TO_DEVICE.toString());
        messageToDevice.put("statusCode", ResponseVS.SC_PROCESSING);
        messageToDevice.put("deviceToId", deviceVS.getId());
        messageToDevice.put("deviceToName", deviceVS.getDeviceName());
        messageToDevice.put("UUID", socketSession.getUUID());
        Map encryptedDataMap =  new HashMap<>();
        encryptedDataMap.put("operation", TypeVS.MESSAGEVS_SIGN.toString());
        encryptedDataMap.put("deviceFromName", InetAddress.getLocalHost().getHostName());
        encryptedDataMap.put("toUser", toUser);
        encryptedDataMap.put("textToSign", textToSign);
        encryptedDataMap.put("subject", subject);
        encryptedDataMap.put("locale", ContextVS.getInstance().getLocale().getLanguage());
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
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                socketSession.getAESParams().toJSON().toString().getBytes(), deviceVS.getX509Certificate());
        messageToDevice.put("aesParams", new String(base64EncryptedAESDataRequestBytes));
        messageToDevice.put("encryptedMessage", Encryptor.encryptAES(
                JSONSerializer.toJSON(encryptedDataMap).toString(), socketSession.getAESParams()));
        return (JSONObject) JSONSerializer.toJSON(messageToDevice);
    }

    private static <T> WebSocketSession checkWebSocketSession (DeviceVS deviceVS, T data, TypeVS typeVS)
            throws NoSuchAlgorithmException {
        WebSocketSession webSocketSession = VotingSystemApp.getInstance().getWSSession(deviceVS.getId());
        if(webSocketSession == null) {
            String randomUUID = java.util.UUID.randomUUID().toString();
            AESParams aesParams = new AESParams();
            webSocketSession = new WebSocketSession(aesParams, deviceVS, null, null);
            VotingSystemApp.getInstance().putWSSession(randomUUID, webSocketSession);
        }
        webSocketSession.setData(data);
        webSocketSession.setTypeVS(typeVS);
        return webSocketSession;
    }

    public static JSONObject getMessageVSToDevice(DeviceVS deviceVS, String toUser, String textToEncrypt) throws Exception {
        Map messageToDevice = new HashMap<>();
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, null, TypeVS.MESSAGEVS);
        messageToDevice.put("operation", TypeVS.MESSAGEVS_TO_DEVICE.toString());
        messageToDevice.put("statusCode", ResponseVS.SC_PROCESSING);
        messageToDevice.put("deviceToId", deviceVS.getId());
        messageToDevice.put("deviceToName", deviceVS.getDeviceName());
        messageToDevice.put("UUID", socketSession.getUUID());
        Map encryptedDataMap =  new HashMap<>();
        encryptedDataMap.put("operation", TypeVS.MESSAGEVS.toString());
        messageToDevice.put("statusCode", ResponseVS.SC_OK);
        encryptedDataMap.put("from", SessionService.getInstance().getUserVS().getFullName());
        encryptedDataMap.put("deviceFromName", InetAddress.getLocalHost().getHostName());
        encryptedDataMap.put("deviceFromId", VotingSystemApp.getInstance().getDeviceId());
        encryptedDataMap.put("toUser", toUser);
        encryptedDataMap.put("message", textToEncrypt);
        messageToDevice.put("locale", ContextVS.getInstance().getLocale().getLanguage());
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                socketSession.getAESParams().toJSON().toString().getBytes(), deviceVS.getX509Certificate());
        messageToDevice.put("aesParams", new String(base64EncryptedAESDataRequestBytes));
        messageToDevice.put("encryptedMessage", Encryptor.encryptAES(
                JSONSerializer.toJSON(encryptedDataMap).toString(), socketSession.getAESParams()));
        return (JSONObject) JSONSerializer.toJSON(messageToDevice);
    }

    public static JSONObject getCooinWalletChangeRequest(DeviceVS deviceVS, List<Cooin> cooinList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, cooinList, TypeVS.COOIN_WALLET_CHANGE);
        Map messageToDevice = new HashMap<>();
        messageToDevice.put("operation", TypeVS.MESSAGEVS_TO_DEVICE.toString());
        messageToDevice.put("statusCode", ResponseVS.SC_PROCESSING);
        messageToDevice.put("timeLimited", true);
        messageToDevice.put("UUID", socketSession.getUUID());
        messageToDevice.put("deviceToId", deviceVS.getId());
        messageToDevice.put("deviceToName", deviceVS.getDeviceName());
        Map encryptedDataMap =  new HashMap<>();
        encryptedDataMap.put("operation", TypeVS.COOIN_WALLET_CHANGE.toString());
        encryptedDataMap.put("deviceFromName", InetAddress.getLocalHost().getHostName());
        encryptedDataMap.put("deviceFromId", VotingSystemApp.getInstance().getDeviceId());
        encryptedDataMap.put("locale", ContextVS.getInstance().getLocale().getLanguage());
        //the serialized request is with CertificationRequestVS instead of Cooins
        List<Map> serializedCooinList = Wallet.getCertificationRequestSerialized(cooinList);
        encryptedDataMap.put("cooinList", serializedCooinList);
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                socketSession.getAESParams().toJSON().toString().getBytes(), deviceVS.getX509Certificate());
        messageToDevice.put("aesParams", new String(base64EncryptedAESDataRequestBytes));
        messageToDevice.put("encryptedMessage", Encryptor.encryptAES(
                JSONSerializer.toJSON(encryptedDataMap).toString(), socketSession.getAESParams()));
        return (JSONObject) JSONSerializer.toJSON(messageToDevice);
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        byte[] decryptedBytes = Encryptor.decryptCMS(messageJSON.getString("aesParams").getBytes(), privateKey);
        this.aesParams = AESParams.load((JSONObject) JSONSerializer.toJSON(new String(decryptedBytes)));
        decryptMessage(this.aesParams);
    }

    public void decryptMessage(AESParams aesParams) throws Exception {
        JSONObject decryptedJSON = (JSONObject) JSONSerializer.toJSON(Encryptor.decryptAES(
                messageJSON.getString("encryptedMessage"), aesParams));
        if(decryptedJSON.has("operation")) operation = TypeVS.valueOf(decryptedJSON.getString("operation"));
        if(decryptedJSON.has("statusCode")) statusCode = decryptedJSON.getInt("statusCode");
        if(decryptedJSON.has("message")) message = decryptedJSON.getString("message");
        if(decryptedJSON.has("deviceFromName")) deviceFromName = decryptedJSON.getString("deviceFromName");
        if(decryptedJSON.has("deviceFromId")) deviceFromId = decryptedJSON.getLong("deviceFromId");
        if(decryptedJSON.has("locale")) this.locale = decryptedJSON.getString("locale");
        if(decryptedJSON.has("from")) this.from = decryptedJSON.getString("from");
        if(decryptedJSON.has("smimeMessage")) {
            byte[] smimeMessageBytes = Base64.getDecoder().decode(decryptedJSON.getString("smimeMessage").getBytes());
            smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
        }
        if(decryptedJSON.has("cooinList")) {
            this.cooinSet = Wallet.getCooinSetFromCertificationRequest(decryptedJSON.getJSONArray("cooinList"));
        }
        this.isEncrypted = false;
        VotingSystemApp.getInstance().putWSSession(UUID, new WebSocketSession<>(
                aesParams, new DeviceVS(deviceFromId, deviceFromName), null, operation));
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
        JSONObject result = null;
        if(isEncrypted != null && !isEncrypted) {
            result = new JSONObject();
            if(operation != null) result.put("operation", operation.toString());
            result.put("statusCode", statusCode);
            result.put("timeLimited", timeLimited);
            result.put("message", message);
            result.put("deviceId", deviceId);
            result.put("URL", URL);
            result.put("deviceFromName", deviceFromName);
            result.put("deviceFromId", deviceFromId);
            result.put("locale", locale);
            result.put("from", from);
            result.put("UUID", UUID);
            if(date != null) result.put("date", DateUtils.getISODateStr(date));
            if(smimeMessage != null) {
                try {
                    result.put("smimeMessage", Base64.getEncoder().encodeToString(smimeMessage.getBytes()));
                } catch(Exception ex) {log.error(ex.getMessage(), ex);}
            }
            if(isEncrypted != null) result.put("isEncrypted", isEncrypted);
            if(cooinSet != null) {
                try {
                    List<Map> serializedCooinList = Wallet.getCooinSerialized(cooinSet);
                    result.put("cooinList", serializedCooinList);
                } catch(Exception ex) {log.error(ex.getMessage(), ex);}
            }
        } else result = messageJSON;
        if(date != null) messageJSON.put("date", DateUtils.getISODateStr(date));
        return result;
    }

    public void setMessageJSON(JSONObject messageJSON) {
        this.messageJSON = messageJSON;
    }

}