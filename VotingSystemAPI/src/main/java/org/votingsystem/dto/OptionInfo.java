package org.votingsystem.dto;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OptionInfo {

    private String content;
    private Long numVoteRequests;
    private Long numUsersWithVote;
    private Long numRepresentativesWithVote;
    private Long numVotesResult;
    
    public OptionInfo() {}
    
    public OptionInfo(String content, Long numVoteRequests, Long numUsersWithVote, 
                      Long numRepresentativesWithVote, Long numVotesResult) {
        this.content = content;
        this.numVoteRequests = numVoteRequests;
        this.numUsersWithVote = numUsersWithVote;
        this.numRepresentativesWithVote = numRepresentativesWithVote;
        this.numVotesResult = numVotesResult;
    }


    public String getContent() {
        return content;
    }

    public Long getNumVoteRequests() {
        return numVoteRequests;
    }

    public Long getNumUsersWithVote() {
        return numUsersWithVote;
    }

    public Long getNumRepresentativesWithVote() {
        return numRepresentativesWithVote;
    }

    public Long getNumVotesResult() {
        return numVotesResult;
    }

    public void addNumVotesResult(Long votesAddded) {
        numVotesResult = numVotesResult + votesAddded;
    }

}
