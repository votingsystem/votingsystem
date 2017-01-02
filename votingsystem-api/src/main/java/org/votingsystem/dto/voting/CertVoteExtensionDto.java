package org.votingsystem.dto.voting;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertVoteExtensionDto {

    private String identityServiceEntity;
    private String votingServiceEntity;
    private String revocationHashBase64;
    private String electionUUID;

    public CertVoteExtensionDto() {}

    public CertVoteExtensionDto(String identityServiceEntity, String votingServiceEntity, String revocationHashBase64,
                                String electionUUID) {
        this.identityServiceEntity = identityServiceEntity;
        this.votingServiceEntity = votingServiceEntity;
        this.revocationHashBase64 = revocationHashBase64;
        this.electionUUID = electionUUID;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public void setRevocationHashBase64(String revocationHashBase64) {
        this.revocationHashBase64 = revocationHashBase64;
    }

    public String getElectionUUID() {
        return electionUUID;
    }

    public void setElectionUUID(String electionUUID) {
        this.electionUUID = electionUUID;
    }

    public String getIdentityServiceEntity() {
        return identityServiceEntity;
    }

    public CertVoteExtensionDto setIdentityServiceEntity(String identityServiceEntity) {
        this.identityServiceEntity = identityServiceEntity;
        return this;
    }

    public String getVotingServiceEntity() {
        return votingServiceEntity;
    }

    public CertVoteExtensionDto setVotingServiceEntity(String votingServiceEntity) {
        this.votingServiceEntity = votingServiceEntity;
        return this;
    }
}