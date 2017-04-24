package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.voting.Vote;
import org.votingsystem.model.voting.VoteCanceler;
import org.votingsystem.util.OperationType;

import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */

@JacksonXmlRootElement(localName = "Vote")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoteDto {

    private Long id;
    @JacksonXmlProperty(localName = "Operation")
    private OperationType operation;
    @JacksonXmlProperty(localName = "State")
    private Vote.State state;
    @JacksonXmlProperty(localName = "RevocationHash")
    private String revocationHash;
    @JacksonXmlProperty(localName = "IndentityServiceEntity")
    private String indentityServiceEntity;
    @JacksonXmlProperty(localName = "VotingServiceEntity")
    private String votingServiceEntity;
    @JacksonXmlProperty(localName = "OptionSelected")
    private ElectionOptionDto optionSelected;
    @JacksonXmlProperty(localName = "ElectionUUID")
    private String electionUUID;

    @JsonIgnore
    private TimeStampToken timeStampToken;
    @JsonIgnore
    private X509Certificate anonCert;
    @JsonIgnore
    private
    Set<X509Certificate> signerCerts;

    public VoteDto() {}

    public VoteDto(String indentityServiceEntity, String votingServiceEntity) {
        this.indentityServiceEntity = indentityServiceEntity;
        this.votingServiceEntity = votingServiceEntity;
    }

    public VoteDto(VoteCanceler canceler, String indentityServiceEntity, String votingServiceEntity) {
        this.id = canceler.getId();
        this.operation = OperationType.CANCEL_VOTE;
        this.state = canceler.getVote().getState();
        this.indentityServiceEntity = indentityServiceEntity;
        this.votingServiceEntity = votingServiceEntity;
    }

    public VoteDto(Vote vote, String indentityServiceEntity, String votingServiceEntity) {
        this.id = vote.getId();
        this.operation = OperationType.SEND_VOTE;
        this.state = vote.getState();
        this.indentityServiceEntity = indentityServiceEntity;
        this.votingServiceEntity = votingServiceEntity;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getElectionUUID() {
        return electionUUID;
    }

    public VoteDto setElectionUUID(String EventId) {
        this.electionUUID = EventId;
        return this;
    }


    public String getRevocationHash() {
        return revocationHash;
    }

    public VoteDto setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
        return this;
    }


    public Vote.State getState() {
        return state;
    }

    public VoteDto setState(Vote.State state) {
        this.state = state;
        return this;
    }

    public ElectionOptionDto getOptionSelected() {
        return optionSelected;
    }

    public VoteDto setOptionSelected(ElectionOptionDto optionSelected) {
        this.optionSelected = optionSelected;
        return this;
    }

    public OperationType getOperation() {
        return operation;
    }

    public VoteDto setOperation(OperationType operation) {
        this.operation = operation;
        return this;
    }

    public String getIndentityServiceEntity() {
        return indentityServiceEntity;
    }

    public VoteDto setIndentityServiceEntity(String indentityServiceEntity) {
        this.indentityServiceEntity = indentityServiceEntity;
        return this;
    }

    public String getVotingServiceEntity() {
        return votingServiceEntity;
    }

    public VoteDto setVotingServiceEntity(String votingServiceEntity) {
        this.votingServiceEntity = votingServiceEntity;
        return this;
    }

    public void validate(CertVoteExtensionDto certVoteExtension, String votingServiceId) {
        if(votingServiceEntity == null)
            throw new IllegalArgumentException("Vote without voting service info");
        if(indentityServiceEntity == null)
            throw new IllegalArgumentException("Vote without identity service info");
        if(!votingServiceEntity.equals(votingServiceId))
            throw new IllegalArgumentException("Expected voting service id: " + votingServiceId +
                    " - found : " + votingServiceEntity);
        if(!votingServiceEntity.equals(certVoteExtension.getVotingServiceEntity()))
            throw new IllegalArgumentException("Expected voting service: " + votingServiceEntity +
                    " - found in cert vote extension: " + certVoteExtension.getVotingServiceEntity());
        if(!indentityServiceEntity.equals(certVoteExtension.getIdentityServiceEntity()))
            throw new IllegalArgumentException("Expected identity service: " + votingServiceEntity +
                    " - found in cert vote extension: " + certVoteExtension.getVotingServiceEntity());
        if(!electionUUID.equals(certVoteExtension.getElectionUUID()))
            throw new IllegalArgumentException("Expected electionUUID: " + electionUUID +
                    " - found in cert vote extension: " + certVoteExtension.getElectionUUID());
        if(!revocationHash.equals(certVoteExtension.getRevocationHash()))
            throw new IllegalArgumentException("Expected revocation hash: " + revocationHash +
                    " - found in cert vote extension: " + certVoteExtension.getRevocationHash());
    }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    public VoteDto setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
        return this;
    }

    public X509Certificate getAnonCert() {
        return anonCert;
    }

    public VoteDto setAnonCert(X509Certificate anonCert) {
        this.anonCert = anonCert;
        return this;
    }

    public Set<X509Certificate> getSignerCerts() {
        return signerCerts;
    }

    public void setSignerCerts(Set<X509Certificate> signerCerts) {
        this.signerCerts = signerCerts;
    }
}