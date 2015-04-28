package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.FieldEventVS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.model.voting.VoteVSCanceler;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoteVSDto {

    private TypeVS operation;
    private Long id;
    private Long cancelerId;
    private Long eventVSId;
    private String certificateURL;
    private String hashCertVSBase64;
    private String hashCertVoteHex;
    private String messageSMIMEURL;
    private String cancelationMessageSMIMEURL;
    private String eventVSURL;
    private String voteUUID;
    private VoteVS.State state;
    private FieldEventVS optionSelected;


    public VoteVSDto() {}


    public VoteVSDto(VoteVSCanceler canceler, String contextURL) {
        this.setId(canceler.getVoteVS().getId());
        this.setCancelerId(canceler.getId());
        this.setState(canceler.getVoteVS().getState());
        this.setCancelationMessageSMIMEURL(contextURL + "/voteVS/id/" + canceler.getVoteVS().getId() + "/cancelation");
    }

    public VoteVSDto(VoteVS voteVS, String contextURL) {
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        setId(voteVS.getId());
        setState(voteVS.getState());
        if(VoteVS.State.CANCELED == getState()) {
            setCancelationMessageSMIMEURL(contextURL + "/voteVS/id/" + voteVS.getId() + "/cancelation");
        }
        setEventVSId(voteVS.getEventVS().getId());
        setEventVSURL(contextURL + "/eventVSElection/id/" + getEventVSId());
        setOptionSelected(voteVS.getOptionSelected());
        setHashCertVSBase64(voteVS.getCertificateVS().getHashCertVSBase64());
        if(getHashCertVSBase64() != null) setHashCertVoteHex(StringUtils.toHex(getHashCertVSBase64()));
        String hashHex = hexConverter.marshal(voteVS.getCertificateVS().getHashCertVSBase64().getBytes());
        setCertificateURL(contextURL + "/certificateVS/hashHex/" + hashHex);
        setMessageSMIMEURL(contextURL + "/messageSMIME/id/" + voteVS.getMessageSMIME().getId());
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCancelerId() {
        return cancelerId;
    }

    public void setCancelerId(Long cancelerId) {
        this.cancelerId = cancelerId;
    }

    public Long getEventVSId() {
        return eventVSId;
    }

    public void setEventVSId(Long eventVSId) {
        this.eventVSId = eventVSId;
    }

    public String getCertificateURL() {
        return certificateURL;
    }

    public void setCertificateURL(String certificateURL) {
        this.certificateURL = certificateURL;
    }

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public String getHashCertVoteHex() {
        return hashCertVoteHex;
    }

    public void setHashCertVoteHex(String hashCertVoteHex) {
        this.hashCertVoteHex = hashCertVoteHex;
    }

    public String getMessageSMIMEURL() {
        return messageSMIMEURL;
    }

    public void setMessageSMIMEURL(String messageSMIMEURL) {
        this.messageSMIMEURL = messageSMIMEURL;
    }

    public String getCancelationMessageSMIMEURL() {
        return cancelationMessageSMIMEURL;
    }

    public void setCancelationMessageSMIMEURL(String cancelationMessageSMIMEURL) {
        this.cancelationMessageSMIMEURL = cancelationMessageSMIMEURL;
    }

    public String getEventVSURL() {
        return eventVSURL;
    }

    public void setEventVSURL(String eventVSURL) {
        this.eventVSURL = eventVSURL;
    }

    public VoteVS.State getState() {
        return state;
    }

    public void setState(VoteVS.State state) {
        this.state = state;
    }

    public FieldEventVS getOptionSelected() {
        return optionSelected;
    }

    public void setOptionSelected(FieldEventVS optionSelected) {
        this.optionSelected = optionSelected;
    }


    public String getVoteUUID() {
        return voteUUID;
    }

    public void setVoteUUID(String voteUUID) {
        this.voteUUID = voteUUID;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

}
