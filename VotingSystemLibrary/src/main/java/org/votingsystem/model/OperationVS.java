package org.votingsystem.model;

import org.apache.log4j.Logger;
import org.votingsystem.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author jgzornoza
 */
public class OperationVS {
    
    private static Logger log = Logger.getLogger(OperationVS.class);

    private TypeVS typeVS;
    private Integer statusCode;
    private String callerCallback;
    private String message;
    private String nif;
    private String documentURL;
    private String urlTimeStampServer;
    private String serverURL;
    private String serviceURL;
    private String receiverName;
    private String email;
    private String asciiDoc;
    private ActorVS targetServer;
    private File file;
    private String signedMessageSubject;
    private Map documentToSign;
    private Map documentToEncrypt;
    private Map documentToDecrypt;
    private Map document;
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

    public String getTimeStampServerURL() {
        if(urlTimeStampServer == null && targetServer == null) return null;
        else if(urlTimeStampServer == null && targetServer != null) return targetServer.getTimeStampServerURL();
        else return urlTimeStampServer;
    }

    public void setUrlTimeStampServer(String urlTimeStampServer) {
        this.urlTimeStampServer = urlTimeStampServer;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setUrlServer(String serverURL) {
        this.serverURL = serverURL;
    }

    public TypeVS getType() {
        return typeVS;
    }

    public String getFileName() {
        if(typeVS == null) return "DEFAULT_OPERATION_NAME";
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
        if(receiverName == null && targetServer == null) return null;
        if(receiverName == null) return targetServer.getNameNormalized();
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
                   !documentToSign.get("UUID").equals(receiptDataMap.get("UUID")) ||
                   !documentToSign.get("representativeNif").equals(receiptDataMap.get("representativeNif")) ||
                   TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION != receiptTypeVS) {
                    return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("receiptDataMismatchErrorMsg"));
                } else return new ResponseVS(ResponseVS.SC_OK);
            default:
                return new ResponseVS(ResponseVS.SC_ERROR,
                        ContextVS.getMessage("serviceNotAvailableForOperationMsg", typeVS));
        }
    }

    public static OperationVS parse (Map dataMap) throws Exception {
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
        if (dataMap.containsKey("nif")) operationVS.setNif((String)dataMap.get("nif"));
        if (dataMap.containsKey("serviceURL")) operationVS.setServiceURL((String)dataMap.get("serviceURL"));
        if (dataMap.containsKey("documentURL")) operationVS.setDocumentURL((String)dataMap.get("documentURL"));
        if (dataMap.containsKey("urlTimeStampServer")) operationVS.setUrlTimeStampServer((String)dataMap.get("urlTimeStampServer"));
        if (dataMap.containsKey("serverURL")) {
            String serverURL = StringUtils.checkURL((String)dataMap.get("serverURL"));
            operationVS.setUrlServer(serverURL);
        }
        if (dataMap.containsKey("objectId")) operationVS.setCallerCallback((String)dataMap.get("objectId"));
        if (dataMap.containsKey("eventVS")) {
            EventVS eventVS = EventVS.parse((Map) dataMap.get("eventVS"));
            operationVS.setEventVS(eventVS);
        }
        if (dataMap.containsKey("signedContent")) {
            Map documentToSignMap = (Map) dataMap.get("signedContent");
            //to avoid process repeated messages on servers
            documentToSignMap.put("UUID", UUID.randomUUID().toString());
            operationVS.setDocumentToSignMap(documentToSignMap);
        }
        if (dataMap.containsKey("asciiDoc")) operationVS.setAsciiDoc((String) dataMap.get("asciiDoc"));
        if (dataMap.containsKey("documentToEncrypt")) {
            Map documentToEncrypt = (Map) dataMap.get("documentToEncrypt");
            operationVS.setDocumentToEncrypt(documentToEncrypt);
        }
        if (dataMap.containsKey("documentToDecrypt")) {
            Map documentToDecrypt = (Map) dataMap.get("documentToDecrypt");
            operationVS.setDocumentToDecrypt(documentToDecrypt);
        }
        if (dataMap.containsKey("document")) {
            Map documentMap = (Map) dataMap.get("document");
            documentMap.put("locale", ContextVS.getInstance().getLocale().getLanguage());
            operationVS.setDocument(documentMap);
        }
        if(dataMap.containsKey("contentType")) operationVS.setContentType((String)dataMap.get("contentType"));
        if (dataMap.containsKey("receiverName")) operationVS.setReceiverName((String)dataMap.get("receiverName"));
        if (dataMap.containsKey("signedMessageSubject"))
            operationVS.setSignedMessageSubject((String)dataMap.get("signedMessageSubject"));
        if (dataMap.containsKey("filePath")) operationVS.setFile(new File((String)dataMap.get("filePath")));
        if (dataMap.containsKey("email")) operationVS.setEmail((String)dataMap.get("email"));
        return operationVS;
    }

    public Map getDataMap () {
        log.debug("getDataMap");
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

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ActorVS getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(ActorVS targetServer) {
        this.targetServer = targetServer;
    }

    public Map getDocumentToEncrypt() {
        return documentToEncrypt;
    }

    public void setDocumentToEncrypt(Map documentToEncrypt) {
        this.documentToEncrypt = documentToEncrypt;
    }

    public Map getDocumentToDecrypt() {
        return documentToDecrypt;
    }

    public void setDocumentToDecrypt(Map documentToDecrypt) {
        this.documentToDecrypt = documentToDecrypt;
    }

    public Map getDocument() {
        return document;
    }

    public void setDocument(Map document) {
        this.document = document;
    }

    public String getAsciiDoc() {
        return asciiDoc;
    }

    public void setAsciiDoc(String asciiDoc) {
        this.asciiDoc = asciiDoc;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }
}

