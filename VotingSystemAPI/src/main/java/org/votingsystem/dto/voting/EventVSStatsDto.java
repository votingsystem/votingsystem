package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.FieldEvent;

import java.util.Date;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSStatsDto {

    private Long id;
    private Long numAccessRequests;
    private Long numVotes;
    private String subject;
    private EventVS.State eventState;
    private Date dateBegin;
    private Date dateFinish;
    private Set<FieldEvent> fieldsEventVS;

    public EventVSStatsDto() {}

    public EventVSStatsDto(EventElection eventElection) {
        this.dateBegin = eventElection.getDateBegin();
        this.dateFinish = eventElection.getDateFinish();
        this.subject = eventElection.getSubject();
        this.eventState = eventElection.getState();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumAccessRequests() {
        return numAccessRequests;
    }

    public void setNumAccessRequests(Long numAccessRequests) {
        this.numAccessRequests = numAccessRequests;
    }


    public Long getNumVotes() {
        return numVotes;
    }

    public void setNumVotes(Long numVotes) {
        this.numVotes = numVotes;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Set<FieldEvent> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public void setFieldsEventVS(Set<FieldEvent> fieldsEventVS) {
        this.fieldsEventVS = fieldsEventVS;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public EventVSStatsDto setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
        return this;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public EventVSStatsDto setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
        return this;
    }

    public EventVS.State getEventState() {
        return eventState;
    }

    public void setEventState(EventVS.State eventState) {
        this.eventState = eventState;
    }
}
