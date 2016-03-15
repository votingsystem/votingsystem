package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.FieldEvent;
import org.votingsystem.model.voting.Vote;
import org.votingsystem.model.voting.VoteCanceler;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoteDto {

    private TypeVS operation;
    private Long id;
    private Long cancelerId;
    private Long EventId;
    private String certificateURL;
    private String hashCertVSBase64;
    private String hashCertVoteHex;
    private String cmsMessageURL;
    private String cmsCancelationMessageURL;
    private String eventURL;
    private Vote.State state;
    private FieldEvent optionSelected;
    private String UUID;


    public VoteDto() {}


    public VoteDto(VoteCanceler canceler, String contextURL) {
        this.setId(canceler.getVote().getId());
        this.setCancelerId(canceler.getId());
        this.setState(canceler.getVote().getState());
        this.setCmsCancelationMessageURL(contextURL + "/rest/vote/id/" + canceler.getVote().getId() + "/cancelation");
    }

    public VoteDto(Vote vote, String contextURL) {
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        setId(vote.getId());
        setState(vote.getState());
        if(Vote.State.CANCELED == getState()) {
            setCmsCancelationMessageURL(contextURL + "/rest/vote/id/" + vote.getId() + "/cancelation");
        }
        setEventId(vote.getEventVS().getId());
        setEventURL(contextURL + "/rest/eventElection/id/" + getEventId());
        setOptionSelected(vote.getOptionSelected());
        setHashCertVSBase64(vote.getCertificateVS().getHashCertVSBase64());
        if(getHashCertVSBase64() != null) setHashCertVoteHex(StringUtils.toHex(getHashCertVSBase64()));
        String hashHex = hexConverter.marshal(vote.getCertificateVS().getHashCertVSBase64().getBytes());
        setCertificateURL(contextURL + "/rest/certificateVS/hashHex/" + hashHex);
        setCmsMessageURL(contextURL + "/rest/cmsMessage/id/" + vote.getCMSMessage().getId());
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

    public Long getEventId() {
        return EventId;
    }

    public void setEventId(Long EventId) {
        this.EventId = EventId;
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

    public String getCmsMessageURL() {
        return cmsMessageURL;
    }

    public void setCmsMessageURL(String cmsMessageURL) {
        this.cmsMessageURL = cmsMessageURL;
    }

    public String getCmsCancelationMessageURL() {
        return cmsCancelationMessageURL;
    }

    public void setCmsCancelationMessageURL(String cmsCancelationMessageURL) {
        this.cmsCancelationMessageURL = cmsCancelationMessageURL;
    }

    public String getEventURL() {
        return eventURL;
    }

    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    public Vote.State getState() {
        return state;
    }

    public void setState(Vote.State state) {
        this.state = state;
    }

    public FieldEvent getOptionSelected() {
        return optionSelected;
    }

    public void setOptionSelected(FieldEvent optionSelected) {
        this.optionSelected = optionSelected;
    }


    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

}
