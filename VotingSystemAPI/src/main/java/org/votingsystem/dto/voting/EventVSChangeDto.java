package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.util.TypeVS;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSChangeDto {

    private TypeVS operation;
    private String accessControlURL;
    private Long eventId;
    private EventVS.State state;
    private String UUID;

    public EventVSChangeDto() {}

    public EventVSChangeDto(EventVS eventVS, String serverURL, TypeVS operation, EventVS.State state) {
        this.operation = operation;
        this.accessControlURL = serverURL;
        if(eventVS.getAccessControlEventId() != null) eventId = eventVS.getAccessControlEventId();
        else eventId = eventVS.getId();
        this.state = state;
        this.UUID = java.util.UUID.randomUUID().toString();
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public EventVS.State getState() {
        return state;
    }

    public void setState(EventVS.State state) {
        this.state = state;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }


}
