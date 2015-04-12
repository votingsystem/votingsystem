package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationVS {

    private static Logger log = Logger.getLogger(OperationVS.class.getSimpleName());

    private TypeVS operation;
    private Integer statusCode;
    @JsonProperty("objectId")
    private String callerCallback;
    private String message;
    private String nif;
    private String documentURL;
    private String timeStampServerURL;
    private String serverURL;
    private String serviceURL;
    private String receiverName;
    private String email;
    private String asciiDoc;
    @JsonIgnore private ActorVS targetServer;
    private File file;
    private String signedMessageSubject;
    @JsonProperty("signedContent") private Map documentToSignMap;
    private Map documentToEncrypt;
    private Map documentToDecrypt;
    private Map document;
    private String contentType;
    private EventVSDto eventVS;
    @JsonProperty("UUID")
    private String UUID;


    public OperationVS() {}

    public OperationVS(int statusCode) {
        this.statusCode = statusCode;
    }

    public OperationVS(TypeVS operation) {
        this.setOperation(operation);
    }

    public OperationVS(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public static OperationVS VOTING_PUBLISHING(Map documentToSignMap, AccessControlVS accessControlVS) {
        OperationVS result = new OperationVS(TypeVS.VOTING_PUBLISHING);
        result.setReceiverName(accessControlVS.getName());
        result.setSignedMessageSubject(ContextVS.getMessage("signedMessageSubject"));
        result.setServiceURL(accessControlVS.getPublishElectionURL());
        result.setTimeStampServerURL(accessControlVS.getTimeStampServerURL());
        return result;
    }

    public String getTimeStampServerURL() {
        if(timeStampServerURL == null && targetServer == null) return null;
        else if(timeStampServerURL == null && targetServer != null) return targetServer.getTimeStampServerURL();
        else return timeStampServerURL;
    }

    public void setTimeStampServerURL(String timeStampServerURL) {
        this.timeStampServerURL = timeStampServerURL;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setUrlServer(String serverURL) {
        this.serverURL = serverURL;
    }

    public TypeVS getType() {
        return getOperation();
    }

    public String getFileName() {
        if(getOperation() == null) return "DEFAULT_OPERATION_NAME";
        else return getOperation().toString();
    }

    public String getCaption() {
        return ContextVS.getInstance().getMessage(getOperation().toString());
    }

    public void setType(TypeVS operation) {
        this.setOperation(operation);
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

    public EventVSDto getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVSDto eventVS) {
        this.eventVS = eventVS;
    }

    public Map getDocumentToSignMap() {
        if(documentToSignMap != null && !documentToSignMap.containsKey("UUID")) {
            documentToSignMap.put("UUID", java.util.UUID.randomUUID().toString());
        }
        return documentToSignMap;
    }

    public void setDocumentToSignMap(Map documentToSignMap) {
        this.documentToSignMap = documentToSignMap;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public String getReceiverName() {
        if(receiverName != null) return receiverName;
        if(targetServer != null) return targetServer.getName();
        return null;
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
        switch(getOperation()) {
            case ANONYMOUS_REPRESENTATIVE_SELECTION:
                TypeVS receiptTypeVS = TypeVS.valueOf((String) receiptDataMap.get("operation"));
                if(!documentToSignMap.get("weeksOperationActive").equals(receiptDataMap.get("weeksOperationActive")) ||
                   !documentToSignMap.get("UUID").equals(receiptDataMap.get("UUID")) ||
                   !documentToSignMap.get("representativeNif").equals(receiptDataMap.get("representativeNif")) ||
                   TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION != receiptTypeVS) {
                    return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("receiptDataMismatchErrorMsg"));
                } else return new ResponseVS(ResponseVS.SC_OK);
            default:
                return new ResponseVS(ResponseVS.SC_ERROR,
                        ContextVS.getMessage("serviceNotAvailableForOperationMsg", getOperation()));
        }
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
        if(document != null && !document.containsKey("locale")) document.put("locale",
                ContextVS.getInstance().getLocale().getLanguage());
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

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}

