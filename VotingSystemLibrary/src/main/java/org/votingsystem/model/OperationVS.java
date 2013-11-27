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
    private String urlDocumento;
    private String urlTimeStampServer;
    private String serverURL;
    private String receiverSignServiceURL;
    private String receiverName;
    private String emailSolicitante;
    private String signedMessageSubject;
    private Boolean isResponseWithReceipt = false;
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
        switch(typeVS) {
            case ACCESS_REQUEST: return ContextVS.getInstance().getMessage("CANCEL_VOTE");
            case CANCEL_VOTE: return ContextVS.getInstance().getMessage("ACCESS_REQUEST");
            case CONTROL_CENTER_ASSOCIATION: return ContextVS.getInstance().getMessage("CONTROL_CENTER_ASSOCIATION");
            case CONTROL_CENTER_STATE_CHANGE_SMIME: return ContextVS.getInstance().getMessage("CONTROL_CENTER_STATE_CHANGE_SMIME");
            case BACKUP_REQUEST: return ContextVS.getInstance().getMessage("BACKUP_REQUEST");
            case MANIFEST_PUBLISHING: return ContextVS.getInstance().getMessage("MANIFEST_PUBLISHING");
            case MANIFEST_SIGN: return ContextVS.getInstance().getMessage("MANIFEST_SIGN");
            case CLAIM_PUBLISHING: return ContextVS.getInstance().getMessage("CLAIM_PUBLISHING");
            case SMIME_CLAIM_SIGNATURE: return ContextVS.getInstance().getMessage("SMIME_CLAIM_SIGNATURE");
            case VOTING_PUBLISHING: return ContextVS.getInstance().getMessage("VOTING_PUBLISHING");
            case SEND_SMIME_VOTE: return ContextVS.getInstance().getMessage("SEND_SMIME_VOTE");
            case VOTE_CANCELLATION: return ContextVS.getInstance().getMessage("VOTE_CANCELLATION");
            case ACCESS_REQUEST_CANCELLATION: return ContextVS.getInstance().getMessage("ACCESS_REQUEST_CANCELLATION");
            case EVENT_CANCELLATION: return ContextVS.getInstance().getMessage("EVENT_CANCELLATION");
            case SAVE_VOTE_RECEIPT: return ContextVS.getInstance().getMessage("SAVE_VOTE_RECEIPT");
            case NEW_REPRESENTATIVE: return ContextVS.getInstance().getMessage("NEW_REPRESENTATIVE");
            case REPRESENTATIVE_VOTING_HISTORY_REQUEST: return ContextVS.getInstance().getMessage("REPRESENTATIVE_VOTING_HISTORY_REQUEST");
            case REPRESENTATIVE_SELECTION: return ContextVS.getInstance().getMessage("REPRESENTATIVE_SELECTION");
            case REPRESENTATIVE_ACCREDITATIONS_REQUEST: return ContextVS.getInstance().getMessage("REPRESENTATIVE_ACCREDITATIONS_REQUEST");
            case REPRESENTATIVE_REVOKE: return ContextVS.getInstance().getMessage("REPRESENTATIVE_REVOKE");
            default: return "NULL_APPLET_OPERATION_CAPTION";
        }
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

    public String getUrlDocumento() {
        return urlDocumento;
    }

    public void setUrlDocumento(String urlDocumento) {
        this.urlDocumento = urlDocumento;
    }

    public String getUrlEnvioDocumento() {
        return receiverSignServiceURL;
    }

    public void setUrlEnvioDocumento(String receiverSignServiceURL) {
        this.receiverSignServiceURL = receiverSignServiceURL;
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
        return StringUtils.getCadenaNormalizada(receiverName);
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

    public boolean isRespuestaConRecibo() {
        return isResponseWithReceipt;
    }

    public void setRespuestaConRecibo(boolean isResponseWithReceipt) {
        this.isResponseWithReceipt = isResponseWithReceipt;
    }

    public String getEmailSolicitante() {
        return emailSolicitante;
    }

    public void setEmailSolicitante(String emailSolicitante) {
        this.emailSolicitante = emailSolicitante;
    }

    public String getCallerCallback() {
        return callerCallback;
    }

    public void setCallerCallback(String callerCallback) {
        this.callerCallback = callerCallback;
    }


    public static OperationVS populate (Map dataMap) {
        logger.debug("- populate ");
        if(dataMap == null) return null;
        OperationVS operationVS = new OperationVS();
        if (dataMap.containsKey("operacion")) operationVS.setType(TypeVS.valueOf((String) dataMap.get("operacion")));
        if (dataMap.containsKey("args")) {
            List<String> argList = (List<String>) dataMap.get("args");
            if(argList != null) {
                String[] args = argList.toArray(new String[argList.size()]);
                operationVS.setArgs(args);
            }
        }
        if (dataMap.containsKey("statusCode")) operationVS.setStatusCode((Integer)dataMap.get("statusCode"));
        if (dataMap.containsKey("message")) operationVS.setMessage((String)dataMap.get("message"));
        if (dataMap.containsKey("receiverSignServiceURL"))
            operationVS.setUrlEnvioDocumento((String)dataMap.get("receiverSignServiceURL"));
        if (dataMap.containsKey("urlDocumento")) operationVS.setUrlDocumento((String)dataMap.get("urlDocumento"));
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

        if (dataMap.containsKey("isResponseWithReceipt"))
            operationVS.setRespuestaConRecibo((Boolean)dataMap.get("isResponseWithReceipt"));

        if (dataMap.containsKey("emailSolicitante"))
            operationVS.setEmailSolicitante((String)dataMap.get("emailSolicitante"));
        return operationVS;
    }

    public Map getDataMap () {
        logger.debug("getDataMap");
        Map dataMap = new HashMap();
        if(statusCode != null) dataMap.put("statusCode", statusCode);
        if(message != null) dataMap.put("message", message);
        if(typeVS != null) dataMap.put("operacion", typeVS.toString());
        if(urlDocumento != null) dataMap.put("urlDocumento", urlDocumento);
        if(receiverSignServiceURL != null) dataMap.put("receiverSignServiceURL", receiverSignServiceURL);
        if(signedMessageSubject != null) dataMap.put("signedMessageSubject", signedMessageSubject);
        if(receiverName != null) dataMap.put("receiverName", receiverName);
        if(isResponseWithReceipt != null) dataMap.put("isResponseWithReceipt", isResponseWithReceipt);
        if(urlTimeStampServer != null) dataMap.put("urlTimeStampServer", urlTimeStampServer);
        if(args != null) dataMap.put("args", args);
        if(eventVS != null) dataMap.put("eventVS", eventVS.getDataMap());
        return dataMap;
    }
}

