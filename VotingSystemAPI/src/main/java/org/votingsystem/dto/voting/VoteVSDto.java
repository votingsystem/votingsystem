package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.voting.FieldEventVS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.model.voting.VoteVSCanceler;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

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
    private String cmsMessageURL;
    private String cmsCancelationMessageURL;
    private String eventURL;
    private VoteVS.State state;
    private FieldEventVS optionSelected;
    private String UUID;


    public VoteVSDto() {}


    public VoteVSDto(VoteVSCanceler canceler, String contextURL) {
        this.setId(canceler.getVoteVS().getId());
        this.setCancelerId(canceler.getId());
        this.setState(canceler.getVoteVS().getState());
        this.setCmsCancelationMessageURL(contextURL + "/rest/voteVS/id/" + canceler.getVoteVS().getId() + "/cancelation");
    }

    public VoteVSDto(VoteVS voteVS, String contextURL) {
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        setId(voteVS.getId());
        setState(voteVS.getState());
        if(VoteVS.State.CANCELED == getState()) {
            setCmsCancelationMessageURL(contextURL + "/rest/voteVS/id/" + voteVS.getId() + "/cancelation");
        }
        setEventVSId(voteVS.getEventVS().getId());
        setEventURL(contextURL + "/rest/eventVSElection/id/" + getEventVSId());
        setOptionSelected(voteVS.getOptionSelected());
        setHashCertVSBase64(voteVS.getCertificateVS().getHashCertVSBase64());
        if(getHashCertVSBase64() != null) setHashCertVoteHex(StringUtils.toHex(getHashCertVSBase64()));
        String hashHex = hexConverter.marshal(voteVS.getCertificateVS().getHashCertVSBase64().getBytes());
        setCertificateURL(contextURL + "/rest/certificateVS/hashHex/" + hashHex);
        setCmsMessageURL(contextURL + "/rest/messageCMS/id/" + voteVS.getCMSMessage().getId());
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
