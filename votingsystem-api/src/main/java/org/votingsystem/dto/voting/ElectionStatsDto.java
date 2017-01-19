package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.Election;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElectionStatsDto {

    private String electionUUID;
    private Long numVotes;
    private Long numIdentityRequests;
    private String subject;
    private Election.State electionState;
    private ZonedDateTime dateBegin;
    private ZonedDateTime dateFinish;
    private Set<ElectionOptionDto> electionOptions;

    public ElectionStatsDto() {}

    public ElectionStatsDto(Election election) {
        this.electionUUID = election.getUUID();
        this.dateBegin = ZonedDateTime.of(election.getDateBegin(), ZoneId.systemDefault());
        this.dateFinish = ZonedDateTime.of(election.getDateFinish(), ZoneId.systemDefault());
        this.subject = election.getSubject();
        this.electionState = election.getState();
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

    public Set<ElectionOptionDto> getElectionOptions() {
        return electionOptions;
    }

    public ElectionStatsDto setElectionOptions(Set<ElectionOptionDto> electionOptions) {
        this.electionOptions = electionOptions;
        return this;
    }

    public ZonedDateTime getDateBegin() {
        return dateBegin;
    }

    public ElectionStatsDto setDateBegin(ZonedDateTime dateBegin) {
        this.dateBegin = dateBegin;
        return this;
    }

    public ZonedDateTime getDateFinish() {
        return dateFinish;
    }

    public ElectionStatsDto setDateFinish(ZonedDateTime dateFinish) {
        this.dateFinish = dateFinish;
        return this;
    }

    public Election.State getElectionState() {
        return electionState;
    }

    public ElectionStatsDto setElectionState(Election.State electionState) {
        this.electionState = electionState;
        return this;
    }

    public String getElectionUUID() {
        return electionUUID;
    }

    public void setElectionUUID(String electionUUID) {
        this.electionUUID = electionUUID;
    }

    public Long getNumIdentityRequests() {
        return numIdentityRequests;
    }

    public ElectionStatsDto setNumIdentityRequests(Long numIdentityRequests) {
        this.numIdentityRequests = numIdentityRequests;
        return this;
    }

}
