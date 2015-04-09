package org.votingsystem.test.dto;

import org.votingsystem.model.VoteVS;

public class VoteResultDto {

    private VoteVS voteVS;
    private String electorNIF;

    public VoteResultDto() {}

    public VoteResultDto(VoteVS voteVS, String electorNIF) {
        this.voteVS = voteVS;
        this.electorNIF = electorNIF;
    }


    public VoteVS getVoteVS() {
        return voteVS;
    }

    public void setVoteVS(VoteVS voteVS) {
        this.voteVS = voteVS;
    }

    public String getElectorNIF() {
        return electorNIF;
    }

    public void setElectorNIF(String electorNIF) {
        this.electorNIF = electorNIF;
    }
}
