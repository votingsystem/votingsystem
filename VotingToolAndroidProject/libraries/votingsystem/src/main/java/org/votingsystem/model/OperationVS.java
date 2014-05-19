package org.votingsystem.model;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.util.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class OperationVS implements Serializable{

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
    private transient JSONObject signedContent;
    private EventVS eventVS;
    private String sessionId;
    private Uri uriData;
    private String[] args;

    
    public OperationVS() {}
    
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
             operation.setSignedContent(operationJSON.getJSONObject("signedContent"));
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

    public JSONObject getSignedContent() {
        return signedContent;
    }

    public void setSignedContent(JSONObject signedContent) {
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
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException,
            JSONException {
        s.defaultReadObject();
        String signedContentStr = (String) s.readObject();
        if(signedContentStr != null) signedContent = new JSONObject(signedContentStr);
    }


    public String getCallerCallback() {
        return callerCallback;
    }

    public void setCallerCallback(String callerCallback) {
        this.callerCallback = callerCallback;
    }
}


