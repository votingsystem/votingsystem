package org.votingsystem.android.util;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.ResponseVS;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketRequest implements Parcelable {

    enum MessageVSState {PENDING}

    private TypeVS typeVS;
    private MessageVSBundle messageVSBundle;
    private Integer statusCode;
    private Long userId;
    private String sessionId;
    private String caption;
    private String message;
    private String url;
    private String serviceCaller = ContextVS.WEB_SOCKET_BROADCAST_ID;
    private OperationVS operationVS;

    public WebSocketRequest() {}

    public WebSocketRequest(Integer statusCode, String message, TypeVS typeVS) {
        this.statusCode = statusCode;
        this.message = message;
        this.typeVS = typeVS;
    }

    public static final Parcelable.Creator<WebSocketRequest> CREATOR =
            new Parcelable.Creator<WebSocketRequest>() {
                @Override public WebSocketRequest createFromParcel(Parcel source) {
                    return new WebSocketRequest(source);
                }
                @Override public WebSocketRequest[] newArray(int size) {
                    return new WebSocketRequest[size];
                }
            };

    @Override public int describeContents() {
        return 0;
    }

    public WebSocketRequest(Parcel source) {
        // Must read values in the same order as they were placed in. The
        // generic 'readValues' instead of the typed vesions are for the null values
        typeVS = (TypeVS) source.readSerializable();
        operationVS = (OperationVS) source.readParcelable(OperationVS.class.getClassLoader());
        String messageVSBundleStr =  (String) source.readValue(String.class.getClassLoader());
        try {
            if(messageVSBundleStr != null) setMessageVSBundle(
                    new MessageVSBundle(new JSONObject(messageVSBundleStr)));
        } catch(Exception ex) {ex.printStackTrace();}
        statusCode = (Integer) source.readValue(Integer.class.getClassLoader());
        userId = (Long) source.readValue(Long.class.getClassLoader());
        sessionId = (String) source.readValue(String.class.getClassLoader());
        caption = (String) source.readValue(String.class.getClassLoader());
        message = (String) source.readValue(String.class.getClassLoader());
        url = (String) source.readValue(String.class.getClassLoader());
        serviceCaller = (String) source.readValue(String.class.getClassLoader());
    }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeSerializable(typeVS);
        parcel.writeParcelable(operationVS, flags);
        if(getMessageVSBundle() != null) parcel.writeValue(getMessageVSBundle().messageVSDataMap.toString());
        else parcel.writeValue(null);
        parcel.writeValue(statusCode);
        parcel.writeValue(userId);
        parcel.writeValue(sessionId);
        parcel.writeValue(caption);
        parcel.writeValue(message);
        parcel.writeValue(url);
        parcel.writeValue(serviceCaller);
    }

    public static WebSocketRequest load(ResponseVS responseVS) {
        WebSocketRequest result = new WebSocketRequest(responseVS.getStatusCode(),
                responseVS.getMessage(), responseVS.getTypeVS());
        result.setServiceCaller(responseVS.getServiceCaller());
        result.setCaption(responseVS.getCaption());
        return result;
    }

    public static WebSocketRequest load(Integer statusCode, String serviceCaller, String caption,
            String message, TypeVS typeVS) {
        WebSocketRequest result = new WebSocketRequest(statusCode, message, typeVS);
        result.setServiceCaller(serviceCaller);
        result.setCaption(caption);
        return result;
    }

    public static WebSocketRequest parse(String message, AppContextVS appContextVS) {
        try {
            JSONObject messageJSON = new JSONObject(message);
            WebSocketRequest result = new WebSocketRequest();
            if(messageJSON.has("operation"))
                result.setTypeVS(TypeVS.valueOf(messageJSON.getString("operation")));
            if(messageJSON.has("statusCode"))
                result.setStatusCode(messageJSON.getInt("statusCode"));
            if(messageJSON.has("userId")) result.setUserId(messageJSON.getLong("userId"));
            if(messageJSON.has("sessionId")) result.setSessionId(messageJSON.getString("sessionId"));
            if(messageJSON.has("messageVSDataMap"))
                result.setMessageVSBundle(new MessageVSBundle(messageJSON.getJSONObject("messageVSDataMap")));
            if(messageJSON.has("message")) result.setMessage(messageJSON.getString("message"));
            if(messageJSON.has("URL")) result.setUrl(messageJSON.getString("URL"));
            if(messageJSON.has("messageVSDataMap")) result.setMessageVSBundle(
                    new MessageVSBundle(messageJSON.getJSONObject("messageVSDataMap")));
            if(messageJSON.has("encryptedMessage")) {
                byte[] decryptedBytes = appContextVS.decryptMessage(
                        messageJSON.getString("encryptedMessage").getBytes());
                OperationVS operationVS =  OperationVS.parse(new String(decryptedBytes, "UTF-8"));
                operationVS.setSessionId(result.getSessionId());
                result.setOperationVS(operationVS);
            }
            return result;
        } catch(Exception ex) {
            ex.printStackTrace();
            return new WebSocketRequest(ResponseVS.SC_ERROR, ex.getMessage(), null);
        }
    }

    public OperationVS getOperationVS() {
        return operationVS;
    }

    public void setOperationVS(OperationVS operationVS) {
        this.operationVS = operationVS;
    }

    public static JSONObject getResponseFromDevice() {
        //request.messageJSON.sessionId
        return null;
    }

    public TypeVS getTypeVS() {
        if(operationVS != null) return operationVS.getTypeVS();
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
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

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public MessageVSBundle getMessageVSBundle() {
        return messageVSBundle;
    }

    public void setMessageVSBundle(MessageVSBundle messageVSBundle) {
        this.messageVSBundle = messageVSBundle;
    }

    public static class MessageVSBundle {
        JSONObject messageVSDataMap;
        public MessageVSBundle(JSONObject messageVSDataMap) {
            this.messageVSDataMap = messageVSDataMap;
        }
        public JSONArray getPendingMessages() throws JSONException {
            if(messageVSDataMap.has(MessageVSState.PENDING.toString()))
                return messageVSDataMap.getJSONArray(MessageVSState.PENDING.toString());
            else return new JSONArray();
        }
    }

    public ResponseVS getNotificationResponse(Context context) {
        ResponseVS responseVS = new ResponseVS(statusCode, message);
        if(ResponseVS.SC_OK == statusCode) {
            if(TypeVS.MESSAGEVS_FROM_DEVICE == typeVS) {
                responseVS.setIconId(R.drawable.signature_ok_22);
                responseVS.setCaption(context.getString(R.string.sign_document_lbl));
                responseVS.setMessage(context.getString(R.string.sign_document_result_ok_msg));
            }
        } else responseVS.setIconId( R.drawable.fa_times_32);
        return responseVS;
    }

    public static JSONObject getSignResponse(Integer statusCode, String message, String sessionId,
             PublicKey publicKey, SMIMEMessage smimeMessage, String locale) throws Exception {
        Map result = new HashMap();
        result.put("sessionId", sessionId);
        result.put("operation", TypeVS.MESSAGEVS_FROM_DEVICE.toString());
        result.put("locale", locale);
        Map encryptedDataMap = new HashMap();
        encryptedDataMap.put("statusCode", statusCode);
        encryptedDataMap.put("message", message);
        encryptedDataMap.put("operation", TypeVS.MESSAGEVS_SIGN.toString());
        encryptedDataMap.put("smimeMessage", new String(Base64.encode(smimeMessage.getBytes())));
        byte[] encryptedData = Encryptor.encryptToCMS(
                new JSONObject(encryptedDataMap).toString().getBytes(), publicKey);
        result.put("encryptedMessage", new String(encryptedData, "UTF_8"));
        return new JSONObject(result);
    }
}