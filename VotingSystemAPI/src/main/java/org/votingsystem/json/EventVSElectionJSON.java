package org.votingsystem.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSElectionJSON {

    private TypeVS operation;
    private EventVS.Type type;
    private Long id;
    private Long eventId;
    private Long accessControlEventVSId;
    private EventVS.State state;
    private String certCAVotacion;
    private String userVS;
    private String URL;
    private String UUID;
    private String controlCenterURL;
    private String subject;
    private String content;
    private String certChain;
    private String accessControlURL;
    private String serverURL;
    private List<Map> fieldsEventVS;
    private List<String> tags;
    private Date dateFinish;
    private Date dateBegin;
    private Date dateCreated;
    private boolean backupAvailable;
    private Map voteVS;
    private Map controlCenter;
    private EventVS.Cardinality cardinality;


    public EventVSElectionJSON() {}

    public EventVSElectionJSON(EventVS eventVS) {
        subject = eventVS.getSubject();
        content = eventVS.getContent();
        dateBegin = eventVS.getDateBegin();
        dateFinish = eventVS.getDateFinish();
        URL = eventVS.getUrl();
        setBackupAvailable(eventVS.getBackupAvailable());
        if(eventVS.getUserVS() != null) userVS = eventVS.getUserVS().getNif();
        if(eventVS.getVoteVS() != null) voteVS = eventVS.getVoteVS().getDataMap();
        accessControlEventVSId = eventVS.getAccessControlEventVSId();
        type = eventVS.getType();
        id = eventVS.getId();
        UUID =  java.util.UUID.randomUUID().toString();
        if(eventVS.getTagVSSet() != null) {
            tags = new ArrayList<String>();
            for (TagVS tag : eventVS.getTagVSSet()) {
                tags.add(tag.getName());
            }
        }
        if(eventVS.getControlCenterVS() != null) {
            controlCenter = new HashMap();
            controlCenter.put("id", eventVS.getControlCenterVS().getId());
            controlCenter.put("name", eventVS.getControlCenterVS().getName());
            controlCenter.put("serverURL", eventVS.getControlCenterVS().getServerURL());
        }
        if (eventVS.getFieldsEventVS() != null) {
            fieldsEventVS = new ArrayList<>();
            for (FieldEventVS opcion : eventVS.getFieldsEventVS()) {
                Map field = new HashMap();
                field.put("content", opcion.getContent());
                field.put("value", opcion.getValue());
                field.put("id", opcion.getId());
                fieldsEventVS.add(field);
            }
        }
        cardinality = eventVS.getCardinality();
    }

    public EventVSElectionJSON(String serverURL) {
        this.serverURL = serverURL;
    }

    public void validate(String serverURL) throws ValidationExceptionVS {
        if(id == null) throw new ValidationExceptionVS("ERROR - missing param - 'id'");
        if(certCAVotacion == null) throw new ValidationExceptionVS("ERROR - missing param - 'certCAVotacion'");
        if(userVS == null) throw new ValidationExceptionVS("ERROR - missing param - 'userVS'");
        if(fieldsEventVS == null) throw new ValidationExceptionVS("ERROR - missing param - 'fieldsEventVS'");
        if(URL == null) throw new ValidationExceptionVS("ERROR - missing param - 'fieldsEventVS'");
        if(controlCenterURL == null) throw new ValidationExceptionVS("ERROR - missing param - 'controlCenterURL'");
        controlCenterURL = StringUtils.checkURL(controlCenterURL);
        if (!controlCenterURL.equals(serverURL)) throw new ValidationExceptionVS("ERROR - expected controlCenterURL:" +
                serverURL + " found: " + controlCenterURL);
    }

    //{"operation":"EVENT_CANCELLATION","accessControlURL":"...","eventId":"..","state":"CANCELLED","UUID":"..."}
    public void validateCancelation() throws ValidationExceptionVS {
        if(operation == null || TypeVS.EVENT_CANCELLATION != operation) throw new ValidationExceptionVS(
                "ERROR - operation expected 'EVENT_CANCELLATION' found: " + operation);
        if(accessControlURL == null) throw new ValidationExceptionVS("ERROR - missing param 'accessControlURL'");
        if(eventId == null) throw new ValidationExceptionVS("ERROR - missing param 'eventId'");
        if(state == null || EventVS.State.DELETED_FROM_SYSTEM != state || EventVS.State.CANCELED != state)
            throw new ValidationExceptionVS("ERROR - expected state 'DELETED_FROM_SYSTEM' found: " + state);
    }

    public TypeVS getOperation() {
        return operation;
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public EventVS.State getState() {
        return state;
    }

    public String getCertCAVotacion() {
        return certCAVotacion;
    }

    public String getUserVS() {
        return userVS;
    }

    public String getURL() {
        return URL;
    }

    public String getControlCenterURL() {
        return controlCenterURL;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getCertChain() {
        return certChain;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public String getServerURL() {
        return serverURL;
    }

    public List<Map> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public List<String> getTags() {
        return tags;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public void setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }

    public void setControlCenterURL(String controlCenterURL) {
        this.controlCenterURL = controlCenterURL;
    }

    public void setFieldsEventVS(List<Map> fieldsEventVS) {
        this.fieldsEventVS = fieldsEventVS;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public void setState(EventVS.State state) {
        this.state = state;
    }

    public void setCertCAVotacion(String certCAVotacion) {
        this.certCAVotacion = certCAVotacion;
    }

    public void setUserVS(String userVS) {
        this.userVS = userVS;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCertChain(String certChain) {
        this.certChain = certChain;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public EventVS.Type getType() {
        return type;
    }

    public void setType(EventVS.Type type) {
        this.type = type;
    }

    public boolean isBackupAvailable() {
        return backupAvailable;
    }

    public void setBackupAvailable(boolean backupAvailable) {
        this.backupAvailable = backupAvailable;
    }
}
