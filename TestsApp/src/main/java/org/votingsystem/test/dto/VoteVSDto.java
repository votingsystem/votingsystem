package org.votingsystem.test.dto;

import org.votingsystem.model.VoteVS;

public class VoteVSDto {

    private VoteVS voteVS;
    private String electorNIF;

    public VoteVSDto() {}

    public VoteVSDto(VoteVS voteVS, String electorNIF) {
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
