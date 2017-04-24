package org.votingsystem.dto.voting;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertVoteExtensionDto {

    private String identityServiceEntity;
    private String votingServiceEntity;
    private String revocationHash;
    private String electionUUID;

    public CertVoteExtensionDto() {}

    public CertVoteExtensionDto(String identityServiceEntity, String votingServiceEntity, String revocationHash,
                                String electionUUID) {
        this.identityServiceEntity = identityServiceEntity;
        this.votingServiceEntity = votingServiceEntity;
        this.revocationHash = revocationHash;
        this.electionUUID = electionUUID;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
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