package org.votingsystem.model;

import org.apache.log4j.Logger;
import org.votingsystem.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author jgzornoza
 */
public class OperationVS {
    
    private static Logger logger = Logger.getLogger(OperationVS.class);

    private TypeVS typeVS;
    private Integer statusCode;
    private String callerCallback;
    private String message;
    private String documentURL;
    private String urlTimeStampServer;
    private String serverURL;
    private String serviceURL;
    private String receiverName;
    private String email;
    private String signedMessageSubject;
    private Map documentToSign;
    private String contentType;
    private EventVS eventVS;
    private String[] args;

    
    public OperationVS() {}
    
    public OperationVS(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public OperationVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }
    
    public OperationVS(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public String[] getArgs() {
        return this.args;
    }
    
    public void setArgs(String[] args) {
        this.args = args;
    }

    public String getUrlTimeStampServer() {
        return urlTimeStampServer;
    }

    public void setUrlTimeStampServer(String urlTimeStampServer) {
        this.urlTimeStampServer = urlTimeStampServer;
    }

    public String getUrlServer() {
        return serverURL;
    }

    public void setUrlServer(String serverURL) {
        this.serverURL = serverURL;
    }

    public TypeVS getType() {
        return typeVS;
    }

    public String getFileName() {
        if(typeVS == null) return "NULL_APPLET_OPERATION";
        else return typeVS.toString();
    }

    public String getCaption() {
        return ContextVS.getMessage(typeVS.toString());
    }

    public void setType(TypeVS typeVS) {
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

    public String getDocumentURL() {
        return documentURL;
    }

    public void setDocumentURL(String documentURL) {
        this.documentURL = documentURL;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    public EventVS getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public Map getDocumentToSignMap() { return documentToSign;  }

    public void setDocumentToSignMap(Map documentToSign) {
        this.documentToSign = documentToSign;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
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

    public String getSignedMessageSubject() {
        return signedMessageSubject;
    }

    public void setSignedMessageSubject(String signedMessageSubject) {
        this.signedMessageSubject = signedMessageSubject;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCallerCallback() {
        return callerCallback;
    }

    public void setCallerCallback(String callerCallback) {
        this.callerCallback = callerCallback;
    }

    public ResponseVS validateReceiptDataMap(Map receiptDataMap) {
        switch(typeVS) {
            case ANONYMOUS_REPRESENTATIVE_SELECTION:
                TypeVS receiptTypeVS = TypeVS.valueOf((String) receiptDataMap.get("operation"));
                if(!documentToSign.get("weeksOperationActive").equals(receiptDataMap.get("weeksOperationActive")) ||
                   !documentToSign.get("accessControlURL").equals(receiptDataMap.get("accessControlURL")) ||
                   !documentToSign.get("representativeNif").equals(receiptDataMap.get("representativeNif")) ||
                   TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION != receiptTypeVS) {
                    return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("receiptDataMismatchErrorMsg"));
                } else return new ResponseVS(ResponseVS.SC_OK);
            default:
                return new ResponseVS(ResponseVS.SC_ERROR,
                        ContextVS.getMessage("serviceNotAvailableForOperationMsg", typeVS));
        }
    }

    public static OperationVS populate (Map dataMap) {
        logger.debug("- populate ");
        if(dataMap == null) return null;
        OperationVS operationVS = new OperationVS();
        if (dataMap.containsKey("operation")) operationVS.setType(TypeVS.valueOf((String) dataMap.get("operation")));
        if (dataMap.containsKey("args")) {
            List<String> argList = (List<String>) dataMap.get("args");
            if(argList != null) {
                String[] args = argList.toArray(new String[argList.size()]);
                operationVS.setArgs(args);
            }
        }
        if (dataMap.containsKey("statusCode")) operationVS.setStatusCode((Integer)dataMap.get("statusCode"));
        if (dataMap.containsKey("message")) operationVS.setMessage((String)dataMap.get("message"));
        if (dataMap.containsKey("serviceURL"))
            operationVS.setServiceURL((String)dataMap.get("serviceURL"));
        if (dataMap.containsKey("documentURL")) operationVS.setDocumentURL((String)dataMap.get("documentURL"));
        if (dataMap.containsKey("urlTimeStampServer")) operationVS.setUrlTimeStampServer((String)dataMap.get("urlTimeStampServer"));
        if (dataMap.containsKey("serverURL")) operationVS.setUrlServer((String)dataMap.get("serverURL"));
        if (dataMap.containsKey("callerCallback")) operationVS.setCallerCallback((String)dataMap.get("callerCallback"));
        if (dataMap.containsKey("eventVS")) {
            EventVS eventVS = EventVS.populate((Map) dataMap.get("eventVS"));
            operationVS.setEventVS(eventVS);
        }
        if (dataMap.containsKey("signedContent")) {
            Map documentToSignMap = (Map) dataMap.get("signedContent");
            //to avoid process repeated messages on servers
            documentToSignMap.put("UUID", UUID.randomUUID().toString());
            operationVS.setDocumentToSignMap(documentToSignMap);
        }

        if(dataMap.containsKey("contentType")) operationVS.setContentType((String)dataMap.get("contentType"));

        if (dataMap.containsKey("receiverName"))
            operationVS.setReceiverName((String)dataMap.get("receiverName"));

        if (dataMap.containsKey("signedMessageSubject"))
            operationVS.setSignedMessageSubject((String)dataMap.get("signedMessageSubject"));

        if (dataMap.containsKey("email"))
            operationVS.setEmail((String)dataMap.get("email"));
        return operationVS;
    }

    public Map getDataMap () {
        logger.debug("getDataMap");
        Map dataMap = new HashMap();
        if(statusCode != null) dataMap.put("statusCode", statusCode);
        if(message != null) dataMap.put("message", message);
        if(typeVS != null) dataMap.put("operation", typeVS.toString());
        if(documentURL != null) dataMap.put("documentURL", documentURL);
        if(serviceURL != null) dataMap.put("serviceURL", serviceURL);
        if(signedMessageSubject != null) dataMap.put("signedMessageSubject", signedMessageSubject);
        if(receiverName != null) dataMap.put("receiverName", receiverName);
        if(urlTimeStampServer != null) dataMap.put("urlTimeStampServer", urlTimeStampServer);
        if(args != null) dataMap.put("args", args);
        if(eventVS != null) dataMap.put("eventVS", eventVS.getDataMap());
        return dataMap;
    }
}

