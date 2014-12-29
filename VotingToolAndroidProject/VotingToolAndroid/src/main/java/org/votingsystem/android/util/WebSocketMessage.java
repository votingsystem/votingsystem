package org.votingsystem.android.util;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.DeviceUtils;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.ResponseVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketMessage implements Parcelable {

    private static final String TAG = WebSocketMessage.class.getSimpleName();

    public static final int TIME_LIMITED_MESSAGE_LIVE = 30; //seconds
    public static final int TRUNCATED_MSG_SIZE = 80; //chars

    private TypeVS operation;
    private Integer statusCode = ResponseVS.SC_PROCESSING;
    private Long deviceId;
    private Long deviceFromId;
    private String UUID;
    private String sessionId;
    private String from;
    private String caption;
    private String message;
    private String deviceFromName;
    private SMIMEMessage smimeMessage;
    private String url;
    private AESParams aesParams;
    private String serviceCaller = ContextVS.WEB_SOCKET_BROADCAST_ID;
    private OperationVS operationVS;
    private JSONObject messageJSON;

    public WebSocketMessage() {}

    public WebSocketMessage(Integer statusCode, String message, TypeVS operation) {
        this.statusCode = statusCode;
        this.message = message;
        this.operation = operation;
    }

    public static final Parcelable.Creator<WebSocketMessage> CREATOR =
        new Parcelable.Creator<WebSocketMessage>() {
            @Override public WebSocketMessage createFromParcel(Parcel source) {
                return new WebSocketMessage(source);
            }
            @Override public WebSocketMessage[] newArray(int size) {
                return new WebSocketMessage[size];
            }
        };

    @Override public int describeContents() {
        return 0;
    }

    public WebSocketMessage(Parcel source) {
        // Must read values in the same order as they were placed in. The
        // generic 'readValues' instead of the typed vesions are for the null values
        operation = (TypeVS) source.readSerializable();
        operationVS = (OperationVS) source.readParcelable(OperationVS.class.getClassLoader());
        statusCode = (Integer) source.readValue(Integer.class.getClassLoader());
        deviceId = (Long) source.readValue(Long.class.getClassLoader());
        deviceFromId = (Long) source.readValue(Long.class.getClassLoader());
        sessionId = (String) source.readValue(String.class.getClassLoader());
        caption = (String) source.readValue(String.class.getClassLoader());
        message = (String) source.readValue(String.class.getClassLoader());
        url = (String) source.readValue(String.class.getClassLoader());
        serviceCaller = (String) source.readValue(String.class.getClassLoader());
        String aesParamsStr =  (String) source.readValue(String.class.getClassLoader());
        if(aesParamsStr != null) {
            try {
                AESParams.load(new JSONObject(aesParamsStr));
            } catch (Exception ex) {ex.printStackTrace();}
        }
    }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeSerializable(operation);
        parcel.writeParcelable(operationVS, flags);
        parcel.writeValue(statusCode);
        parcel.writeValue(deviceId);
        parcel.writeValue(deviceFromId);
        parcel.writeValue(sessionId);
        parcel.writeValue(caption);
        parcel.writeValue(message);
        parcel.writeValue(url);
        parcel.writeValue(serviceCaller);
        try {
            if(aesParams != null) parcel.writeValue(aesParams.toJSON().toString());
            else parcel.writeValue(null);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public static WebSocketMessage load(ResponseVS responseVS) {
        WebSocketMessage result = new WebSocketMessage(responseVS.getStatusCode(),
                responseVS.getMessage(), responseVS.getTypeVS());
        result.setServiceCaller(responseVS.getServiceCaller());
        result.setCaption(responseVS.getCaption());
        return result;
    }

    public static WebSocketMessage load(Integer statusCode, String serviceCaller, String caption,
            String message, TypeVS typeVS) {
        WebSocketMessage result = new WebSocketMessage(statusCode, message, typeVS);
        result.setServiceCaller(serviceCaller);
        result.setCaption(caption);
        return result;
    }

    public void setAESParams(AESParams aesParams) {
        this.aesParams = aesParams;
    }

    public AESParams getAESParams() {
        return aesParams;
    }

    public JSONObject getMessageJSON() {
        return messageJSON;
    }

    public void setMessageJSON(JSONObject messageJSON) {
        this.messageJSON = messageJSON;
    }

    public boolean isEncrypted() {
        return messageJSON.has("encryptedMessage");
    }

    public String getUUID() {
        return UUID;
    }

    public  WebSocketMessage (String message) {
        try {
            messageJSON = new JSONObject(message);
            if(messageJSON.has("UUID")) UUID = messageJSON.getString("UUID");
            if(messageJSON.has("operation"))
                setOperation(TypeVS.valueOf(messageJSON.getString("operation")));
            if(messageJSON.has("statusCode")) setStatusCode(messageJSON.getInt("statusCode"));
            if(messageJSON.has("deviceId")) deviceId = messageJSON.getLong("deviceId");
            if(messageJSON.has("sessionId")) setSessionId(messageJSON.getString("sessionId"));
            if(messageJSON.has("message")) setMessage(messageJSON.getString("message"));
            if(messageJSON.has("URL")) setUrl(messageJSON.getString("URL"));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void decryptMessage(AESParams aesParams) throws Exception {
        if(messageJSON.has("encryptedMessage")) {
            loadDecryptedJSON(new JSONObject(Encryptor.decryptAES(
                    messageJSON.getString("encryptedMessage"), aesParams)));
        } else throw new ExceptionVS("encryptedMessage");
    }

    public byte[] getEncryptedMessage() throws JSONException {
        return messageJSON.getString("encryptedMessage").getBytes();
    }

    public void loadDecryptedJSON(JSONObject decryptedJSON) throws Exception {
        this.operationVS =  OperationVS.parse(decryptedJSON).setSessionId(
                getSessionId()).setUUID(UUID);
        if(decryptedJSON.has("statusCode")) statusCode = decryptedJSON.getInt("statusCode");
        if(decryptedJSON.has("message")) message = decryptedJSON.getString("message");
        if(decryptedJSON.has("from")) this.from = decryptedJSON.getString("from");
        if(decryptedJSON.has("deviceFromName")) deviceFromName = decryptedJSON.getString("deviceFromName");
        if(decryptedJSON.has("deviceFromId")) deviceFromId = decryptedJSON.getLong("deviceFromId");
        if(decryptedJSON.has("smimeMessage")) {
            byte[] smimeMessageBytes = Base64.decode(decryptedJSON.getString("smimeMessage").getBytes());
            setSmimeMessage(new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes)));
        }
    }

    public OperationVS getOperationVS() {
        return operationVS;
    }

    public void setOperationVS(OperationVS operationVS) {
        this.operationVS = operationVS;
    }

    public TypeVS getOperation() {
        if(operationVS != null) return operationVS.getTypeVS();
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public WebSocketMessage setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageTruncated() {
        if(message != null && message.length() > TRUNCATED_MSG_SIZE)
            return message.substring(0, TRUNCATED_MSG_SIZE) + "...";
        else return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceCaller() {
        return serviceCaller;
    }

    public void setServiceCaller(String serviceCaller) {
        this.serviceCaller = serviceCaller;
    }

    public String getCaption() {
        return caption;
    }

    public WebSocketMessage setCaption(String caption) {
        this.caption = caption;
        return this;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override public String toString() {
        return this.getClass().getSimpleName() + " - statusCode:" + statusCode + " - " +
                getOperation() + " - " + UUID;
    }

    public ResponseVS getNotificationResponse(Context context) {
        ResponseVS responseVS = new ResponseVS(statusCode, message);
        if(ResponseVS.SC_OK == statusCode) {
            if(TypeVS.MESSAGEVS_FROM_DEVICE == operation) {
                responseVS.setIconId(R.drawable.fa_cert_22);
                responseVS.setCaption(context.getString(R.string.sign_document_lbl));
                responseVS.setMessage(context.getString(R.string.sign_document_result_ok_msg));
            }
        } else responseVS.setIconId( R.drawable.fa_times_32);
        return responseVS;
    }


    public static JSONObject getMessageJSON(TypeVS operation, String message, Map data,
            SMIMEMessage smimeMessage, String UUID, Context context) throws IOException,
            MessagingException {
        Map messageToServiceMap = new HashMap();
        messageToServiceMap.put("locale", context.getResources().getConfiguration().locale.getLanguage());
        messageToServiceMap.put("operation", operation.toString());
        messageToServiceMap.put("UUID", UUID);
        if(message != null) messageToServiceMap.put("message", message);
        if(data != null) messageToServiceMap.put("data", data);
        if(smimeMessage != null) messageToServiceMap.put("smimeMessage", new String(
                Base64.encode(smimeMessage.getBytes())));
        return new JSONObject(messageToServiceMap);
    }


    public JSONObject getResponse(Integer statusCode, String message, Context context)
            throws Exception {
        Map result = new HashMap();
        result.put("sessionId", operationVS.getSessionId());
        result.put("locale", context.getResources().getConfiguration().locale.getLanguage());
        if(operationVS.getPublicKey() != null) {
            result.put("operation", TypeVS.MESSAGEVS_FROM_DEVICE.toString());
            Map encryptedDataMap = new HashMap();
            encryptedDataMap.put("statusCode", statusCode);
            encryptedDataMap.put("message", message);
            encryptedDataMap.put("operation", operationVS.getTypeVS().toString());
            result.put("encryptedMessage", Encryptor.encryptAES(
                    new JSONObject(encryptedDataMap).toString(), aesParams));
        } else {
            result.put("operation", operationVS.getTypeVS().toString());
            result.put("statusCode", statusCode);
            result.put("message", message);
        }
        return new JSONObject(result);
    }

    public JSONObject getBanResponse(Context context) throws Exception {
        Map result = new HashMap();
        result.put("sessionId", operationVS.getSessionId());
        result.put("locale", context.getResources().getConfiguration().locale.getLanguage());
        result.put("operation", TypeVS.WEB_SOCKET_BAN_SESSION);
        return new JSONObject(result);
    }

    public static JSONObject getSignResponse(Integer statusCode, String message,
            OperationVS  operationVS, SMIMEMessage smimeMessage, AppContextVS contextVS) throws Exception {
        Map result = new HashMap();
        result.put("sessionId", operationVS.getSessionId());
        result.put("operation", TypeVS.MESSAGEVS_FROM_DEVICE.toString());
        result.put("UUID", operationVS.getUUID());
        result.put("locale", contextVS.getResources().getConfiguration().locale.getLanguage());
        Map encryptedDataMap = new HashMap();
        encryptedDataMap.put("statusCode", statusCode);
        encryptedDataMap.put("message", message);
        encryptedDataMap.put("operation", TypeVS.MESSAGEVS_SIGN.toString());
        encryptedDataMap.put("smimeMessage", new String(Base64.encode(smimeMessage.getBytes())));
        AESParams aesParams = contextVS.getSessionKey(operationVS.getUUID());
        result.put("encryptedMessage", Encryptor.encryptAES(
                new JSONObject(encryptedDataMap).toString(), aesParams));
        return new JSONObject(result);
    }

    //Base64.DEFAULT -> problems with Java 8 with
    public static JSONObject getCooinWalletChangeRequest(Long deviceToId, String deviceToName,
            List<Cooin> cooinList,  String locale, X509Certificate deviceToCert,
            AppContextVS contextVS) throws Exception {
        Map messageToDevice = new HashMap<>();
        String randomUUID = java.util.UUID.randomUUID().toString();
        messageToDevice.put("operation", TypeVS.MESSAGEVS_TO_DEVICE.toString());
        messageToDevice.put("statusCode", ResponseVS.SC_PROCESSING);
        messageToDevice.put("timeLimited", true);
        messageToDevice.put("UUID", randomUUID);
        messageToDevice.put("deviceToId", deviceToId);
        messageToDevice.put("deviceToName", deviceToName);
        messageToDevice.put("locale", locale);
        Map encryptedDataMap =  new HashMap<>();
        encryptedDataMap.put("operation", TypeVS.COOIN_WALLET_CHANGE.toString());
        encryptedDataMap.put("deviceFromName", DeviceUtils.getDeviceName());
        encryptedDataMap.put("deviceFromId", contextVS.getDeviceId());
        List<Map> serializedCooinList = Wallet.getSerializedCertificationRequestList(cooinList);
        encryptedDataMap.put("cooinList", serializedCooinList);
        AESParams aesParams = new AESParams();
        contextVS.putWSSession(randomUUID, new WebSocketSession(aesParams, null, cooinList,
                TypeVS.COOIN_WALLET_CHANGE));
        encryptedDataMap.put("aesParams", aesParams.toJSON());
        byte[] encryptedRequestBytes = Encryptor.encryptToCMS(
                new JSONObject(encryptedDataMap).toString().getBytes(), deviceToCert);
        messageToDevice.put("encryptedMessage", new String(Base64.encode(encryptedRequestBytes)));
        return new JSONObject(messageToDevice);
    }

    public static JSONObject getMessageVSToDevice(DeviceVS deviceVS, String toUser,
          String textToEncrypt, AppContextVS contextVS) throws Exception {
        String randomUUID = null;
        AESParams aesParams = null;
        WebSocketSession webSocketSession = contextVS.getWSSession(deviceVS.getId());
        if(webSocketSession != null) {
            randomUUID = webSocketSession.getUUID();
            aesParams = webSocketSession.getAESParams();
        } else {
            randomUUID = java.util.UUID.randomUUID().toString();
            aesParams = new AESParams();
            contextVS.putWSSession(randomUUID, new WebSocketSession(aesParams, deviceVS, null,
                    TypeVS.MESSAGEVS));
        }
        Map messageToDevice = new HashMap<>();
        messageToDevice.put("operation", TypeVS.MESSAGEVS_TO_DEVICE.toString());
        messageToDevice.put("statusCode", ResponseVS.SC_WS_MESSAGE_ENCRYPTED);
        messageToDevice.put("deviceToId", deviceVS.getId());
        messageToDevice.put("deviceToName", deviceVS.getDeviceName());
        messageToDevice.put("UUID", randomUUID);
        Map encryptedDataMap =  new HashMap<>();
        encryptedDataMap.put("operation", TypeVS.MESSAGEVS.toString());
        encryptedDataMap.put("from", contextVS.getUserVS().getName());
        encryptedDataMap.put("deviceFromName", DeviceUtils.getDeviceName());
        encryptedDataMap.put("deviceFromId", contextVS.getDeviceId());
        encryptedDataMap.put("toUser", toUser);
        encryptedDataMap.put("message", textToEncrypt);
        encryptedDataMap.put("locale", contextVS.getResources().getConfiguration().locale.getLanguage());
        byte[] encryptedAESDataRequestBytes = Encryptor.encryptToCMS(
                aesParams.toJSON().toString().getBytes(), deviceVS.getX509Certificate());
        messageToDevice.put("aesParams", new String(Base64.encode(encryptedAESDataRequestBytes)));
        messageToDevice.put("encryptedMessage", Encryptor.encryptAES(
                new JSONObject(encryptedDataMap).toString(), aesParams));
        return new JSONObject(messageToDevice);
    }

    public static JSONObject getCooinWalletChangeRequest(JSONObject deviceToJSON,
        AppContextVS contextVS, Cooin... cooins) throws Exception {
        X509Certificate deviceToCert = CertUtils.fromPEMToX509CertCollection(
                deviceToJSON.getString("certPEM").getBytes()).iterator().next();
        return WebSocketMessage.getCooinWalletChangeRequest(deviceToJSON.getLong("id"),
                deviceToJSON.getString("deviceName"), Arrays.asList(cooins),
                contextVS.getResources().getConfiguration().locale.getLanguage(),
                deviceToCert, contextVS);
    }

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public SMIMEMessage getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(SMIMEMessage smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}