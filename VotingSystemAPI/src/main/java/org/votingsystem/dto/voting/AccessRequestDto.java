package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.AccessRequestVS;
import org.votingsystem.model.voting.EventVSElection;
import org.votingsystem.util.TypeVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessRequestDto {

    private TypeVS operation = TypeVS.ACCESS_REQUEST;
    private String eventURL;
    private Long eventId;
    private String hashAccessRequestBase64;
    private String UUID;

    @JsonIgnore private EventVSElection eventVS;
    @JsonIgnore private AccessRequestVS accessRequestVS;

    public AccessRequestDto() {}


    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getEventURL() {
        return eventURL;
    }

    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public EventVSElection getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVSElection eventVS) {
        this.eventVS = eventVS;
    }

    public AccessRequestVS getAccessRequestVS() {
        return accessRequestVS;
    }

    public void setAccessRequestVS(AccessRequestVS accessRequestVS) {
        this.accessRequestVS = accessRequestVS;
    }
}
