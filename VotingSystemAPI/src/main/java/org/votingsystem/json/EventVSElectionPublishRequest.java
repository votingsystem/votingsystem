package org.votingsystem.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.EventVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSElectionPublishRequest {

    private TypeVS operation;
    private Long id;
    private Long eventId;
    private EventVS.State state;
    private String certCAVotacion;
    private String userVS;
    private String URL;
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

    public EventVSElectionPublishRequest() {}

    public EventVSElectionPublishRequest(String serverURL) {
        this.serverURL = serverURL;
    }

    public void validate() throws ValidationExceptionVS {
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
}
