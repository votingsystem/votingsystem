package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentativesAccreditations {

    private Long numRepresentatives;
    private Long numRepresentativesWithAccessRequest;
    private Long numRepresentativesWithVote;
    private Long numRepresentedWithAccessRequest;
    private Long numRepresented;
    private Long numVotesRepresentedByRepresentatives;
    private Map options;
    private Map representatives;

    public RepresentativesAccreditations() {}


    public RepresentativesAccreditations(Long numRepresentatives, Long numRepresentativesWithAccessRequest,
            Long numRepresentativesWithVote, Long numRepresentedWithAccessRequest, Long numRepresented,
            Long numVotesRepresentedByRepresentatives, Map options, Map representatives) {
        this.numRepresentatives = numRepresentatives;
        this.numRepresentativesWithAccessRequest = numRepresentativesWithAccessRequest;
        this.numRepresentativesWithVote = numRepresentativesWithVote;
        this.numRepresentativesWithAccessRequest = numRepresentativesWithAccessRequest;
        this.numRepresented = numRepresented;
        this.numVotesRepresentedByRepresentatives = numVotesRepresentedByRepresentatives;
        this.options = options;
        this.representatives = representatives;
    }

    public Long getNumRepresentatives() {
        return numRepresentatives;
    }

    public Long getNumRepresentativesWithAccessRequest() {
        return numRepresentativesWithAccessRequest;
    }

    public Long getNumRepresentativesWithVote() {
        return numRepresentativesWithVote;
    }

    public Long getNumRepresentedWithAccessRequest() {
        return numRepresentedWithAccessRequest;
    }

    public Long getNumRepresented() {
        return numRepresented;
    }

    public Long getNumVotesRepresentedByRepresentatives() {
        return numVotesRepresentedByRepresentatives;
    }

    public Map getOptions() {
        return options;
    }

    public Map getRepresentatives() {
        return representatives;
    }

}
