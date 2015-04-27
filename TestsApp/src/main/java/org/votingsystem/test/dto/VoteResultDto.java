package org.votingsystem.test.dto;

import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.model.voting.VoteVS;

public class VoteResultDto {

    private VoteVSDto voteVS;
    private String electorNIF;

    public VoteResultDto() {}

    public VoteResultDto(VoteVSDto voteVS, String electorNIF) {
        this.voteVS = voteVS;
        this.electorNIF = electorNIF;
    }


    public VoteVSDto getVoteVS() {
        return voteVS;
    }

    public void setVoteVS(VoteVSDto voteVS) {
        this.voteVS = voteVS;
    }

    public String getElectorNIF() {
        return electorNIF;
    }

    public void setElectorNIF(String electorNIF) {
        this.electorNIF = electorNIF;
    }
}
