package org.votingsystem.dto;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.Vote;

import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CMSDto {

    private User signer;
    private Vote vote;
    private EventVS eventVS;
    private User anonymousSigner;
    private Set<User> signers;
    private CMSMessage cmsMessage;

    public CMSDto() {}

    public CMSDto(Vote vote) {
        this.vote = vote;
    }

    public User getSigner() {
        return signer;
    }

    public void setSigner(User signer) {
        this.signer = signer;
        addSigner(signer);
    }

    public User getAnonymousSigner() {
        return anonymousSigner;
    }

    public void setAnonymousSigner(User anonymousSigner) {
        this.anonymousSigner = anonymousSigner;
        addSigner(anonymousSigner);
    }

    public Set<User> getSigners() {
        return signers;
    }

    public void setSigners(Set<User> signers) {
        this.signers = signers;
    }

    public void addSigner(User signer) {
        if(signers == null) signers = new HashSet<>();
        signers.add(signer);
    }

    public Vote getVote() {
        return vote;
    }

    public void setVote(Vote vote) {
        this.vote = vote;
    }

    public EventVS getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }

    public void setCmsMessage(CMSMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
    }
}
