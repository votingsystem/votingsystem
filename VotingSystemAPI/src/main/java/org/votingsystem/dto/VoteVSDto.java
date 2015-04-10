package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.model.VoteVSCanceler;
import org.votingsystem.util.StringUtils;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoteVSDto {

    private Long id;
    private Long cancelerId;
    private Long eventVSId;
    private String certificateURL;
    private String hashAccessRequestBase64;
    private String hashAccessRequestHex;
    private String hashCertVSBase64;
    private String hashCertVoteHex;
    private String messageSMIMEURL;
    private String cancelationMessageSMIMEURL;
    private String eventVSURL;
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
        setHashAccessRequestBase64(voteVS.getHashAccessRequestBase64());
        if(getHashAccessRequestBase64() != null) setHashAccessRequestHex(StringUtils.toHex(getHashAccessRequestBase64()));
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

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public String getHashAccessRequestHex() {
        return hashAccessRequestHex;
    }

    public void setHashAccessRequestHex(String hashAccessRequestHex) {
        this.hashAccessRequestHex = hashAccessRequestHex;
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
}
