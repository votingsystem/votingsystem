package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationVS {

    private static Logger log = Logger.getLogger(OperationVS.class.getSimpleName());

    private TypeVS operation;
    private Integer statusCode;
    private String message;
    private String nif;
    private String documentURL;
    private String serverURL;
    private String serviceURL;
    private String receiverName;
    private String email;
    private File file;
    private String signedMessageSubject;
    private String jsonStr;
    private String contentType;
    private EventVSDto eventVS;
    private VoteVSDto voteVS;
    private String UUID;

    @JsonProperty("objectId") private String callerCallback;
    @JsonIgnore private ActorVS targetServer;

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

    public String getServerURL() {
        return serverURL;
    }

    public TypeVS getType() {
        return getOperation();
    }

    public OperationVS updateUUID() throws IOException {
        Map<String, Object> dataMap = JSON.getMapper().readValue(jsonStr, new TypeReference<HashMap<String, Object>>() {});
        dataMap.put("uuid", java.util.UUID.randomUUID().toString());
        jsonStr = JSON.getMapper().writeValueAsString(dataMap);
        return this;
    }

    public String getFileName() {
        if(operation == null) return "DEFAULT_OPERATION_NAME";
        else return operation.toString();
    }

    public String getCaption() {
        return ContextVS.getInstance().getMessage(operation.toString());
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

    public Map getDocumentToSign() {
        Map documentToSignMap = null;
        try {
            documentToSignMap = JSON.getMapper().readValue(jsonStr, new TypeReference<Map<String, String>>() {});
            documentToSignMap.put("UUID", java.util.UUID.randomUUID().toString());
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return documentToSignMap;
    }

    public <T> T getDocumentToSign(Class<T> type) throws Exception {
        return JSON.getMapper().readValue(jsonStr, type);
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

    public <T> T getData(Class<T> type) throws IOException {
        return JSON.getMapper().readValue(jsonStr, type);
    }

    public String getJsonStr() {
        return jsonStr;
    }

    public void setJsonStr(String jsonStr) {
        this.jsonStr = jsonStr;
    }

    public VoteVSDto getVoteVS() {
        return voteVS;
    }

    public void setVoteVS(VoteVSDto voteVS) {
        this.voteVS = voteVS;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

}

