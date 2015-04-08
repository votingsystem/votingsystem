package org.votingsystem.dto;

import org.votingsystem.model.VoteVS;
import org.votingsystem.model.VoteVSCanceler;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteVSDto {

    Long id, cancelerId, optionSelectedId, eventVSId;
    String certificateURL, hashCertVSBase64, messageSMIMEURL, cancelationMessageSMIMEURL, eventVSURL;
    VoteVS.State state;


    public VoteVSDto() {}


    public VoteVSDto(VoteVSCanceler canceler, String contextURL) {
        this.id = canceler.getVoteVS().getId();
        this.cancelerId = canceler.getId();
        this.state = canceler.getVoteVS().getState();
        this.cancelationMessageSMIMEURL = contextURL + "/voteVS/id/" + canceler.getVoteVS().getId() + "/cancelation";
    }

    public VoteVSDto(VoteVS voteVS, String contextURL) {
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        this.id = voteVS.getId();
        this.state = voteVS.getState();
        if(VoteVS.State.CANCELED == this.state) {
            this.cancelationMessageSMIMEURL = contextURL + "/voteVS/id/" + voteVS.getId() + "/cancelation";
        }
        this.eventVSId = voteVS.getEventVS().getId();
        this.eventVSURL = contextURL + "/eventVSElection/id/" + this.eventVSId;
        this.optionSelectedId = voteVS.getOptionSelected().getId();
        this.hashCertVSBase64 = voteVS.getCertificateVS().getHashCertVSBase64();
        String hashHex = hexConverter.marshal(voteVS.getCertificateVS().getHashCertVSBase64().getBytes());
        this.certificateURL = contextURL + "/certificateVS/hashHex/" + hashHex;
        this.messageSMIMEURL = contextURL + "/messageSMIME/id/" + voteVS.getMessageSMIME().getId();
    }


}
