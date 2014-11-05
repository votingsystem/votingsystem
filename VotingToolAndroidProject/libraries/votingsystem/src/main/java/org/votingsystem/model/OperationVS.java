package org.votingsystem.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.util.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.mail.Header;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OperationVS implements Parcelable {

	public static final String TAG = OperationVS.class.getSimpleName();

    private static final long serialVersionUID = 1L;
    
    private TypeVS typeVS;
    private Integer statusCode;
    private String caption;
    private String callerCallback;
    private String message;
    private String urlTimeStampServer;
    private String serviceURL;
    private String serverURL;
    private String receiverName;
    private String signedMessageSubject;
    private EventVS eventVS;
    private String sessionId;
    private String publicKeyBase64;
    private Uri uriData;
    private String[] args;
    private transient JSONArray targetCertArray;
    private transient JSONObject signedContent;
    private transient JSONObject documentToEncrypt;
    private transient JSONObject documentToDecrypt;
    private transient JSONObject document;
    private String deviceFromName;
    private String toUser;
    private String textToSign;
    private String subject;
    private List<Header> headerList;

    public OperationVS() {}

    public OperationVS(Parcel source) {
        // Must read values in the same order as they were placed in. The
        // generic 'readValues' instead of the typed vesions are for the null values
        typeVS = (TypeVS) source.readSerializable();
        statusCode = (Integer) source.readValue(Integer.class.getClassLoader());
        sessionId = (String) source.readValue(String.class.getClassLoader());
        callerCallback = (String) source.readValue(String.class.getClassLoader());
        message = (String) source.readValue(String.class.getClassLoader());
        urlTimeStampServer = (String) source.readValue(String.class.getClassLoader());
        serviceURL = (String) source.readValue(String.class.getClassLoader());
        serverURL = (String) source.readValue(String.class.getClassLoader());
        receiverName = (String) source.readValue(String.class.getClassLoader());
        signedMessageSubject = (String) source.readValue(String.class.getClassLoader());
        sessionId = (String) source.readValue(String.class.getClassLoader());
        publicKeyBase64 = (String) source.readValue(String.class.getClassLoader());
        uriData = (Uri) source.readValue(Uri.class.getClassLoader());
        deviceFromName = (String) source.readValue(String.class.getClassLoader());
        toUser = (String) source.readValue(String.class.getClassLoader());
        textToSign = (String) source.readValue(String.class.getClassLoader());
        subject = (String) source.readValue(String.class.getClassLoader());
        String headerArrayStr = (String) source.readValue(String.class.getClassLoader());
        try {
            if(headerArrayStr != null) headerList = getHeadersList(new JSONArray(headerArrayStr));
        } catch(Exception ex) {ex.printStackTrace();}
    }

    @Override public int describeContents() { return 0;  }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeSerializable(typeVS);
        parcel.writeValue(statusCode);
        parcel.writeValue(sessionId);
        parcel.writeValue(callerCallback);
        parcel.writeValue(message);
        parcel.writeValue(urlTimeStampServer);
        parcel.writeValue(serviceURL);
        parcel.writeValue(serverURL);
        parcel.writeValue(receiverName);
        parcel.writeValue(signedMessageSubject);
        parcel.writeValue(sessionId);
        parcel.writeValue(publicKeyBase64);
        parcel.writeValue(uriData);
        parcel.writeValue(deviceFromName);
        parcel.writeValue(toUser);
        parcel.writeValue(textToSign);
        parcel.writeValue(subject);
        try {
            parcel.writeValue(getHeadersJSONArray(headerList).toString());
        } catch (JSONException ex) { ex.printStackTrace(); }
    }

    public static final Parcelable.Creator<OperationVS> CREATOR =
            new Parcelable.Creator<OperationVS>() {
                @Override public OperationVS createFromParcel(Parcel source) {
                    return new OperationVS(source);
                }

                @Override public OperationVS[] newArray(int size) {
                    return new OperationVS[size];
                }

            };


    public static JSONArray getHeadersJSONArray(List<Header> headerList) throws JSONException {
        JSONArray headersArray = new JSONArray();
        if(headerList != null) {
            for(Header header : headerList) {
                if (header != null) {
                    JSONObject headerJSON = new JSONObject();
                    headerJSON.put("name", header.getName());
                    headerJSON.put("value", header.getValue());
                    headersArray.put(headerJSON);
                }
            }
        }
        return headersArray;
    }

    public static List<Header> getHeadersList(JSONArray jsonArray) throws JSONException {
        List<Header> headerList = new ArrayList<Header>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject headerJSON = (JSONObject) jsonArray.get(i);
            Header header = new Header(headerJSON.getString("name"), headerJSON.getString("value"));
            headerList.add(header);
        }
        return headerList;
    }

    public OperationVS(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public OperationVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public OperationVS(TypeVS typeVS, Uri uriData) {
        this.typeVS = typeVS;
        this.uriData = uriData;
    }

    public OperationVS(String typeVS) {
        this.typeVS = TypeVS.valueOf(typeVS);
    }
    
    public OperationVS(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
    
    public OperationVS(int statusCode, String message, TypeVS typeVS) {
        this.statusCode = statusCode;
        this.message = message;
        this.typeVS = typeVS;
    }

    public String[] getArgs() {
        return this.args;
    }
    
    public void setArgs(String[] args) {
        this.args = args;
    }

    public String getTimeStampServerURL() {
        return urlTimeStampServer;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setUrlTimeStampServer(String urlTimeStampServer) {
        this.urlTimeStampServer = urlTimeStampServer;
    }

    public TypeVS getTypeVS() {
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

    public EventVS getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public String getReceiverName() {
        return receiverName;
    }
    
    public String getNormalizedReceiverName() {
        if(receiverName == null) return null;
        return StringUtils.getNormalized(receiverName);
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public static OperationVS parse (String operationStr) throws JSONException, ParseException {
        if(operationStr == null) return null;
        OperationVS operation = new OperationVS();
        JSONObject operationJSON = new JSONObject(operationStr);
        if (operationJSON.has("operation")) {
            operation.setTypeVS(TypeVS.valueOf(operationJSON.getString("operation")));
        }
        if(operationJSON.has("deviceFromName")) operation.setDeviceFromName(
                operationJSON.getString("deviceFromName"));
        if(operationJSON.has("toUser")) operation.setToUser(operationJSON.getString("toUser"));
        if(operationJSON.has("textToSign"))
            operation.setTextToSign(operationJSON.getString("textToSign"));
        if(operationJSON.has("subject")) operation.setSubject(operationJSON.getString("subject"));
        if(operationJSON.has("headers")) operation.setHeaderList(
                getHeadersList(operationJSON.getJSONArray("headers")));
        if(operationJSON.has("publicKey")) operation.setPublicKeyBase64(
                operationJSON.getString("publicKey"));
        if (operationJSON.has("args")) {
            JSONArray arrayArgs = operationJSON.getJSONArray("args");
            String[] args = new String[arrayArgs.length()];
            for(int i = 0; i < arrayArgs.length(); i++) {
                args[i] = arrayArgs.getString(i);
            }
            operation.setArgs(args);
        }
        if (operationJSON.has("statusCode")) {
            operation.setStatusCode(operationJSON.getInt("statusCode"));
        }
        if (operationJSON.has("message")) {
            operation.setMessage(operationJSON.getString("message"));
        }   
        if (operationJSON.has("serviceURL")) {
            operation.setServiceURL(operationJSON.getString("serviceURL"));
        }
        if (operationJSON.has("urlTimeStampServer")) {
            operation.setUrlTimeStampServer(operationJSON.getString("urlTimeStampServer"));
        }  
        if (operationJSON.has("eventVS")) {
            EventVS eventVS = EventVS.parse(operationJSON.getJSONObject("eventVS"));
            operation.setEventVS(eventVS);
        }  
        if (operationJSON.has("signedContent"))
             operation.setDocumentToSignJSON(operationJSON.getJSONObject("signedContent"));
        if (operationJSON.has("documentToEncrypt"))
            operation.setDocumentToEncrypt(operationJSON.getJSONObject("documentToEncrypt"));
        if (operationJSON.has("documentToDecrypt"))
            operation.setDocumentToDecrypt(operationJSON.getJSONObject("documentToDecrypt"));
        if (operationJSON.has("document")) {
            operation.setDocument(operationJSON.getJSONObject("document"));
            operation.getDocument().put("locale", Locale.getDefault().getLanguage().toLowerCase());
        }
        if (operationJSON.has("targetCertList")) {
            operation.setTargetCertArray(operationJSON.getJSONArray("targetCertList"));
        }
        if (operationJSON.has("receiverName")) {
            operation.setReceiverName(operationJSON.getString("receiverName"));
        }
        if (operationJSON.has("callerCallback")) {
            operation.setCallerCallback(operationJSON.getString("callerCallback"));
        }

        if (operationJSON.has("serverURL")) operation.setServerURL(operationJSON.getString("serverURL"));
        if (operationJSON.has("signedMessageSubject")) {
            operation.setSignedMessageSubject(operationJSON.getString("signedMessageSubject"));
        }
        if (operationJSON.has("sessionId")) {
        	operation.setSessionId(operationJSON.getString("sessionId"));
        }
        if (operationJSON.has("caption")) operation.setCaption(operationJSON.getString("caption"));
        return operation;
    }

    public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.decode(publicKeyBase64)));
    }

    public JSONObject getJSON () throws JSONException {
    	JSONObject jsonObject = new JSONObject();
        if(statusCode != null) jsonObject.put("statusCode", statusCode);
        if(message != null) jsonObject.put("message", message);
        if(typeVS != null) jsonObject.put("operation", typeVS.toString());
        if(getServiceURL() != null) jsonObject.put("serviceURL", getServiceURL());
        if(signedMessageSubject != null) jsonObject.put("signedMessageSubject", getServiceURL());
        if(receiverName != null) jsonObject.put("receiverName", receiverName);
        if(urlTimeStampServer != null) jsonObject.put("urlTimeStampServer", urlTimeStampServer);
        if(args != null) jsonObject.put("args", args);
        if(sessionId != null) jsonObject.put("sessionId", sessionId);
        if(eventVS != null) jsonObject.put("eventVS", eventVS.toJSON());
        if(caption != null) jsonObject.put("caption", caption);
        if(signedContent != null) jsonObject.put("signedContent", signedContent);
        return jsonObject;
    }

    public String getSignedMessageSubject() {
        return signedMessageSubject;
    }

    public void setSignedMessageSubject(String signedMessageSubject) {
        this.signedMessageSubject = signedMessageSubject;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public JSONObject getDocumentToSignJSON() {
        return signedContent;
    }

    public void setDocumentToSignJSON(JSONObject signedContent) {
        this.signedContent = signedContent;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    public Uri getUriData() {
        return uriData;
    }

    public void setUriData(Uri uriData) {
        this.uriData = uriData;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(signedContent != null) s.writeObject(signedContent.toString());
            else s.writeObject(null);
            if(documentToEncrypt != null) s.writeObject(documentToEncrypt.toString());
            else s.writeObject(null);
            if(documentToDecrypt != null) s.writeObject(documentToDecrypt.toString());
            else s.writeObject(null);
            if(document != null) s.writeObject(document.toString());
            else s.writeObject(null);
            if(targetCertArray != null) s.writeObject(targetCertArray.toString());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException,
            JSONException {
        s.defaultReadObject();
        String contentStr = (String) s.readObject();
        if(contentStr != null) signedContent = new JSONObject(contentStr);
        contentStr = (String) s.readObject();
        if(contentStr != null) documentToEncrypt = new JSONObject(contentStr);
        contentStr = (String) s.readObject();
        if(contentStr != null) documentToDecrypt = new JSONObject(contentStr);
        contentStr = (String) s.readObject();
        if(contentStr != null) document = new JSONObject(contentStr);
        contentStr = (String) s.readObject();
        if(contentStr != null) targetCertArray = new JSONArray(contentStr);
    }

    public String getCallerCallback() {
        return callerCallback;
    }

    public void setCallerCallback(String callerCallback) {
        this.callerCallback = callerCallback;
    }

    public JSONObject getDocument() {
        return document;
    }

    public void setDocument(JSONObject document) {
        this.document = document;
    }

    public JSONObject getDocumentToEncrypt() {
        return documentToEncrypt;
    }

    public void setDocumentToEncrypt(JSONObject documentToEncrypt) {
        this.documentToEncrypt = documentToEncrypt;
    }

    public JSONObject getDocumentToDecrypt() {
        return documentToDecrypt;
    }

    public void setDocumentToDecrypt(JSONObject documentToDecrypt) {
        this.documentToDecrypt = documentToDecrypt;
    }

    public JSONArray getTargetCertArray() {
        return targetCertArray;
    }

    public void setTargetCertArray(JSONArray targetCertArray) {
        this.targetCertArray = targetCertArray;
    }

    public String getDeviceFromName() {
        return deviceFromName;
    }

    public void setDeviceFromName(String deviceFromName) {
        this.deviceFromName = deviceFromName;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getTextToSign() {
        return textToSign;
    }

    public void setTextToSign(String textToSign) {
        this.textToSign = textToSign;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<Header> getHeaderList() {
        return headerList;
    }

    public void setHeaderList(List<Header> headerList) {
        this.headerList = headerList;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
    }
}


