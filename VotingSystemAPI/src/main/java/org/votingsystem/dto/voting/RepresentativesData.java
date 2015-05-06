package org.votingsystem.dto.voting;

import java.util.Map;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class RepresentativesData {
    
    private Long numRepresentatives = null;
    private Long numRepresentativesWithAccessRequest = null;
    private Long numRepresentativesWithVote = null;
    private Long numRepresentedWithAccessRequest = null;
    private Long numRepresented = null;
    private Long numVotesRepresentedByRepresentatives = null;
    private Map<String, RepresentativeData> representativeMap = null;
    
    /**
     * @return the numRepresentatives
     */
    public Long getNumRepresentatives() {
        return numRepresentatives;
    }

    /**
     * @param numRepresentatives the numRepresentatives to set
     */
    public void setNumRepresentatives(Long numRepresentatives) {
        this.numRepresentatives = numRepresentatives;
    }

    /**
     * @return the numRepresentativesWithAccessRequest
     */
    public Long getNumRepresentativesWithAccessRequest() {
        return numRepresentativesWithAccessRequest;
    }

    /**
     * @param numRepresentativesWithAccessRequest the numRepresentativesWithAccessRequest to set
     */
    public void setNumRepresentativesWithAccessRequest(Long numRepresentativesWithAccessRequest) {
        this.numRepresentativesWithAccessRequest = numRepresentativesWithAccessRequest;
    }

    /**
     * @return the numRepresentativesWithVote
     */
    public Long getNumRepresentativesWithVote() {
        return numRepresentativesWithVote;
    }

    /**
     * @param numRepresentativesWithVote the numRepresentativesWithVote to set
     */
    public void setNumRepresentativesWithVote(Long numRepresentativesWithVote) {
        this.numRepresentativesWithVote = numRepresentativesWithVote;
    }

    /**
     * @return the numRepresentedWithAccessRequest
     */
    public Long getNumRepresentedWithAccessRequest() {
        return numRepresentedWithAccessRequest;
    }

    /**
     * @param numRepresentedWithAccessRequest the numRepresentedWithAccessRequest to set
     */
    public void setNumRepresentedWithAccessRequest(Long numRepresentedWithAccessRequest) {
        this.numRepresentedWithAccessRequest = numRepresentedWithAccessRequest;
    }

    /**
     * @return the numVotesRepresentedByRepresentatives
     */
    public Long getNumVotesRepresentedByRepresentatives() {
        return numVotesRepresentedByRepresentatives;
    }

    /**
     * @param numVotesRepresentedByRepresentatives the numVotesRepresentedByRepresentatives to set
     */
    public void setNumVotesRepresentedByRepresentatives(Long numVotesRepresentedByRepresentatives) {
        this.numVotesRepresentedByRepresentatives = numVotesRepresentedByRepresentatives;
    }

    /**
     * @return the numRepresented
     */
    public Long getNumRepresented() {
        return numRepresented;
    }

    /**
     * @param numRepresented the numRepresented to set
     */
    public void setNumRepresented(Long numRepresented) {
        this.numRepresented = numRepresented;
    }

    /**
     * @return the representativeMap
     */
    public Map<String, RepresentativeData> getRepresentativeMap() {
        return representativeMap;
    }

    /**
     * @param representativeMap the representativeMap to set
     */
    public void setRepresentativeMap(Map<String, RepresentativeData> representativeMap) {
        this.representativeMap = representativeMap;
    }

    public String getString() {
        return "[numRepresentatives: " + numRepresentatives + 
                " - numRepresentativesWithVote: "+ numRepresentativesWithVote + 
                " - numRepresented: " + numRepresented + 
                " - numRepresentedWithAccessRequest: " + numRepresentedWithAccessRequest +
                " - numVotesRepresentedByRepresentatives: " + numVotesRepresentedByRepresentatives
                + "]";
    }

}
