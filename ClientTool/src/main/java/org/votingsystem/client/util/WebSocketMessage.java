package org.votingsystem.client.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.*;

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

    private static Logger log = Logger.getLogger(WebSocketMessage.class.getSimpleName());

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
    private Map messageMap;
    private Set<Currency> currencySet;
    private AESParams aesParams;
    private SMIMEMessage smimeMessage;
    private Date date;
    private WebSocketSession webSocketSession;


    public WebSocketMessage(Map socketMsgJSON) throws ParseException, NoSuchAlgorithmException {
        this.messageMap = socketMsgJSON;
        if(socketMsgJSON.containsKey("sessionId")) this.sessionId = (String) socketMsgJSON.get("sessionId");
        if(socketMsgJSON.containsKey("operation")) this.operation = TypeVS.valueOf((String) socketMsgJSON.get("operation"));
        if(socketMsgJSON.containsKey("timeLimited")) this.timeLimited = (boolean) socketMsgJSON.get("timeLimited");
        if(socketMsgJSON.containsKey("statusCode")) this.statusCode = ((Number) socketMsgJSON.get("statusCode")).intValue();
        if(socketMsgJSON.containsKey("deviceId")) this.deviceId = ((Number)socketMsgJSON.get("deviceId")).longValue();
        if(socketMsgJSON.containsKey("URL")) this.URL = (String) socketMsgJSON.get("URL");
        if(socketMsgJSON.containsKey("locale")) this.locale = (String) socketMsgJSON.get("locale");
        if(socketMsgJSON.containsKey("from")) this.from = (String) socketMsgJSON.get("from");
        if(socketMsgJSON.containsKey("deviceFromName")) this.deviceFromName = (String) socketMsgJSON.get("deviceFromName");
        if(socketMsgJSON.containsKey("deviceFromId")) this.deviceFromId = ((Number)socketMsgJSON.get("deviceFromId")).longValue();
        if(socketMsgJSON.containsKey("UUID")) this.setUUID((String) socketMsgJSON.get("UUID"));
        if(socketMsgJSON.containsKey("date")) this.date = DateUtils.getDateFromString((String) socketMsgJSON.get("date"));
        if(socketMsgJSON.containsKey("isEncrypted")) this.isEncrypted = (Boolean) socketMsgJSON.get("isEncrypted");
        if(socketMsgJSON.containsKey("message")) {
            Object messageObject = socketMsgJSON.get("message");
            if(messageObject instanceof  Map) {
                Map messageMap = (Map) socketMsgJSON.get("message");
                this.statusCode = ((Number) messageMap.get("statusCode")).intValue();
                this.operation = TypeVS.valueOf((String) messageMap.get("operation"));
                if(messageMap.containsKey("message")) this.message = (String) messageMap.get("message");
                if(messageMap.containsKey("URL")) this.URL = (String) messageMap.get("URL");
            } else this.message = (String) socketMsgJSON.get("message");
        }
        if(socketMsgJSON.containsKey("smimeMessage")) {
            try {
                byte[] smimeMessageBytes = Base64.getDecoder().decode(((String)socketMsgJSON.get("smimeMessage")).getBytes());
                smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
            }catch(Exception ex) {log.log(Level.SEVERE,ex.getMessage(), ex);}
        }
        if(socketMsgJSON.containsKey("currencyList")) {
            try {
                currencySet = Wallet.getCurrencySet((List<Map>) socketMsgJSON.get("currencyList"));
            }catch(Exception ex) {log.log(Level.SEVERE,ex.getMessage(), ex);}
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
        if(messageMap != null) messageMap.put("operation", operation);
    }

    public String getWebSocketCoreSignalJSCommand(ConnectionStatus status) {
        return getWebSocketCoreSignalJSCommand(messageMap, status);
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

    public Set<Currency> getCurrencySet() {
        return currencySet;
    }

    public void setCurrencySet(Set<Currency> currencySet) {
        this.currencySet = currencySet;
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

    public Map getResponse(Integer statusCode, String message) throws Exception {
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
            String dataToEncryptStr = new ObjectMapper().writeValueAsString(dataToEncrypt);
            result.put("encryptedMessage", Encryptor.encryptAES(dataToEncryptStr, aesParams));
        } else {
            result.put("operation", operation.toString());
            result.put("statusCode", statusCode);
            result.put("message", message);
        }
        return result;
    }

    public static Map getAuthenticationRequest(SMIMEMessage smimeMessage, String UUID) throws Exception {
        Map messageToServiceMap = new HashMap<>();
        messageToServiceMap.put("operation", TypeVS.INIT_VALIDATED_SESSION);
        messageToServiceMap.put("deviceFromId", SessionService.getInstance().getDeviceId());
        messageToServiceMap.put("locale", ContextVS.getInstance().getLocale().getLanguage());
        messageToServiceMap.put("smimeMessage", Base64.getEncoder().encodeToString(smimeMessage.getBytes()));
        messageToServiceMap.put("UUID", UUID);
        VotingSystemApp.getInstance().putWSSession(UUID, new WebSocketSession<>(
                null, null, null, TypeVS.INIT_VALIDATED_SESSION));
        return messageToServiceMap;
    }

    public static Map getSignRequest(DeviceVS deviceVS, String toUser, String textToSign, String subject ,
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
            List headersList = new ArrayList<>();
            for(Header header : headers) {
                if (header != null) {
                    Map headerMap = new HashMap<>();
                    headerMap.put("name", header.getName());
                    headerMap.put("value", header.getValue());
                    headersList.add(headerMap);
                }
            }
            encryptedDataMap.put("headers", headersList);
        }
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                socketSession.getAESParams().toMap().toString().getBytes(), deviceVS.getX509Certificate());
        messageToDevice.put("aesParams", new String(base64EncryptedAESDataRequestBytes));
        String encryptedDataStr = new ObjectMapper().writeValueAsString(encryptedDataMap);
        messageToDevice.put("encryptedMessage", Encryptor.encryptAES(encryptedDataStr, socketSession.getAESParams()));
        return messageToDevice;
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

    public static Map getMessageVSToDevice(DeviceVS deviceVS, String toUser, String textToEncrypt) throws Exception {
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
                socketSession.getAESParams().toMap().toString().getBytes(), deviceVS.getX509Certificate());
        messageToDevice.put("aesParams", new String(base64EncryptedAESDataRequestBytes));
        String encryptedDataStr = new ObjectMapper().writeValueAsString(encryptedDataMap);
        messageToDevice.put("encryptedMessage", Encryptor.encryptAES(encryptedDataStr, socketSession.getAESParams()));
        return messageToDevice;
    }

    public static Map getCurrencyWalletChangeRequest(DeviceVS deviceVS, List<Currency> currencyList) throws Exception {
        WebSocketSession socketSession = checkWebSocketSession(deviceVS, currencyList, TypeVS.CURRENCY_WALLET_CHANGE);
        Map messageToDevice = new HashMap<>();
        messageToDevice.put("operation", TypeVS.MESSAGEVS_TO_DEVICE.toString());
        messageToDevice.put("statusCode", ResponseVS.SC_PROCESSING);
        messageToDevice.put("timeLimited", true);
        messageToDevice.put("UUID", socketSession.getUUID());
        messageToDevice.put("deviceToId", deviceVS.getId());
        messageToDevice.put("deviceToName", deviceVS.getDeviceName());
        Map encryptedDataMap =  new HashMap<>();
        encryptedDataMap.put("operation", TypeVS.CURRENCY_WALLET_CHANGE.toString());
        encryptedDataMap.put("deviceFromName", InetAddress.getLocalHost().getHostName());
        encryptedDataMap.put("deviceFromId", VotingSystemApp.getInstance().getDeviceId());
        encryptedDataMap.put("locale", ContextVS.getInstance().getLocale().getLanguage());
        //the serialized request is with CertificationRequestVS instead of Currency
        List<Map> serializedCurrencyList = Wallet.getCertificationRequestSerialized(currencyList);
        encryptedDataMap.put("currencyList", serializedCurrencyList);
        byte[] base64EncryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                socketSession.getAESParams().toMap().toString().getBytes(), deviceVS.getX509Certificate());
        messageToDevice.put("aesParams", new String(base64EncryptedAESDataRequestBytes));
        String encryptedDataStr = new ObjectMapper().writeValueAsString(encryptedDataMap);
        
        messageToDevice.put("encryptedMessage", Encryptor.encryptAES(encryptedDataStr, socketSession.getAESParams()));
        return messageToDevice;
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        byte[] decryptedBytes = Encryptor.decryptCMS(((String)messageMap.get("aesParams")).getBytes(), privateKey);
        this.aesParams = AESParams.load(new ObjectMapper().readValue(new String(decryptedBytes) , 
                new TypeReference<HashMap<String, Object>>() {}));
        decryptMessage(this.aesParams);
    }

    public void decryptMessage(AESParams aesParams) throws Exception {
        Map<String, Object> decryptedMap = new ObjectMapper().readValue(Encryptor.decryptAES(
                (String) messageMap.get("encryptedMessage"), aesParams), new TypeReference<HashMap<String, Object>>() {});
        if(decryptedMap.containsKey("operation")) operation = TypeVS.valueOf((String) decryptedMap.get("operation"));
        if(decryptedMap.containsKey("statusCode")) statusCode = ((Number) decryptedMap.get("statusCode")).intValue();
        if(decryptedMap.containsKey("message")) message = (String) decryptedMap.get("message");
        if(decryptedMap.containsKey("deviceFromName")) deviceFromName = (String) decryptedMap.get("deviceFromName");
        if(decryptedMap.containsKey("deviceFromId")) deviceFromId = ((Number)decryptedMap.get("deviceFromId")).longValue();
        if(decryptedMap.containsKey("locale")) this.locale = (String) decryptedMap.get("locale");
        if(decryptedMap.containsKey("from")) this.from = (String) decryptedMap.get("from");
        if(decryptedMap.containsKey("smimeMessage")) {
            byte[] smimeMessageBytes = Base64.getDecoder().decode(((String)decryptedMap.get("smimeMessage")).getBytes());
            smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
        }
        if(decryptedMap.containsKey("currencyList")) {
            this.currencySet = Wallet.getCurrencySetFromCertificationRequest((List) decryptedMap.get("currencyList"));
        }
        this.isEncrypted = false;
        VotingSystemApp.getInstance().putWSSession(UUID, new WebSocketSession<>(
                aesParams, new DeviceVS(deviceFromId, deviceFromName), null, operation));
    }

    public static String getWebSocketCoreSignalJSCommand(Map messageMap, ConnectionStatus status) {
        Map coreSignal = new HashMap<>();
        if(messageMap == null) messageMap = new HashMap();
        messageMap.put("socketStatus", status.toString());
        //this.fire('core-signal', {name: "vs-websocket-message", data: messageMap});
        coreSignal.put("name", "vs-websocket-message");
        coreSignal.put("data", messageMap);
        String jsCommand = null;
        try {
            jsCommand = "fireCoreSignal('" + Base64.getEncoder().encodeToString(
                    coreSignal.toString().getBytes("UTF-8")) + "')";
        } catch (UnsupportedEncodingException ex) { log.log(Level.SEVERE,ex.getMessage(), ex); }
        return jsCommand;
    }

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public Map getMessageJSON() {
        Map result = null;
        if(isEncrypted != null && !isEncrypted) {
            result = new HashMap<>();
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
                } catch(Exception ex) {log.log(Level.SEVERE,ex.getMessage(), ex);}
            }
            if(isEncrypted != null) result.put("isEncrypted", isEncrypted);
            if(currencySet != null) {
                try {
                    List<Map> serializedCurrencyList = Wallet.getCurrencySerialized(currencySet);
                    result.put("currencyList", serializedCurrencyList);
                } catch(Exception ex) {log.log(Level.SEVERE,ex.getMessage(), ex);}
            }
        } else result = messageMap;
        if(date != null) messageMap.put("date", DateUtils.getISODateStr(date));
        return result;
    }

    public void setMessageJSON(Map messageMap) {
        this.messageMap = messageMap;
    }

}