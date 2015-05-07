package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.FieldEventVS;

import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventVSStatsDto {

    private Long id;
    private Long numAccessRequests;
    private Long numAccessRequestsOK;
    private Long numAccessRequestsCancelled;
    private Long numVotesVS;
    private Long numVotesVSOK;
    private Long numVotesVSVotesVSCANCELED;
    private String subject;
    private Set<FieldEventVS> fieldsEventVS;

    public EventVSStatsDto() {}


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

    public Long getNumAccessRequestsOK() {
        return numAccessRequestsOK;
    }

    public void setNumAccessRequestsOK(Long numAccessRequestsOK) {
        this.numAccessRequestsOK = numAccessRequestsOK;
    }

    public Long getNumAccessRequestsCancelled() {
        return numAccessRequestsCancelled;
    }

    public void setNumAccessRequestsCancelled(Long numAccessRequestsCancelled) {
        this.numAccessRequestsCancelled = numAccessRequestsCancelled;
    }

    public Long getNumVotesVS() {
        return numVotesVS;
    }

    public void setNumVotesVS(Long numVotesVS) {
        this.numVotesVS = numVotesVS;
    }

    public Long getNumVotesVSOK() {
        return numVotesVSOK;
    }

    public void setNumVotesVSOK(Long numVotesVSOK) {
        this.numVotesVSOK = numVotesVSOK;
    }

    public Long getNumVotesVSVotesVSCANCELED() {
        return numVotesVSVotesVSCANCELED;
    }

    public void setNumVotesVSVotesVSCANCELED(Long numVotesVSVotesVSCANCELED) {
        this.numVotesVSVotesVSCANCELED = numVotesVSVotesVSCANCELED;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Set<FieldEventVS> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public void setFieldsEventVS(Set<FieldEventVS> fieldsEventVS) {
        this.fieldsEventVS = fieldsEventVS;
    }

}
