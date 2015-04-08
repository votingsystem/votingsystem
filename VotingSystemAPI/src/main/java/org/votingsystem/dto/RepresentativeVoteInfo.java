package org.votingsystem.dto;


public class RepresentativeVoteInfo {

    private Long id;
    private Long optionSelectedId;
    private Long numRepresentations;
    private Long numRepresentedWithVote;
    private Long numVotesRepresented;

    public RepresentativeVoteInfo() {}

    public RepresentativeVoteInfo(Long id, Long optionSelectedId, Long numRepresentations, Long numRepresentedWithVote,
                                  Long numVotesRepresented) {
        this.id = id;
        this.optionSelectedId = optionSelectedId;
        this.numRepresentations = numRepresentations;
        this.numRepresentedWithVote = numRepresentedWithVote;
        this.numVotesRepresented = numVotesRepresented;
    }


    public Long getId() {
        return id;
    }

    public Long getOptionSelectedId() {
        return optionSelectedId;
    }

    public Long getNumRepresentations() {
        return numRepresentations;
    }

    public Long getNumRepresentedWithVote() {
        return numRepresentedWithVote;
    }

    public Long getNumVotesRepresented() {
        return numVotesRepresented;
    }
}
