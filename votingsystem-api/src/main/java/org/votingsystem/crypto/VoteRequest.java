package org.votingsystem.crypto;

import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.util.Constants;
import org.votingsystem.util.OperationType;

import java.io.Serializable;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originHashIdentityRequest;
    private String hashIdentityRequestBase64;
    private String originRevocationHash;
    private String revocationHash;
    private VoteDto voteDto;
    private ElectionDto election;
    private IdentityRequestDto identityRequestDto;
    private CertificationRequest certificationRequest;

    public VoteRequest() throws Exception {}


    public VoteRequest(ElectionDto election, ElectionOptionDto optionSelected,
                                       String identityServiceId) throws Exception {
        this.originHashIdentityRequest = UUID.randomUUID().toString();
        this.hashIdentityRequestBase64 = HashUtils.getHashBase64(
                this.originHashIdentityRequest.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        this.originRevocationHash = UUID.randomUUID().toString();
        this.revocationHash = HashUtils.getHashBase64(
                this.originRevocationHash.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        this.election = election;

        IdentityRequestDto identityRequestDto = new IdentityRequestDto();
        identityRequestDto.setUUID(election.getUUID());
        identityRequestDto.setCallbackServiceEntityId(new SystemEntityDto(election.getEntityId(),
                SystemEntityType.VOTING_SERVICE_PROVIDER));
        identityRequestDto.setRevocationHash(this.hashIdentityRequestBase64);
        identityRequestDto.setUUID(UUID.randomUUID().toString());
        this.identityRequestDto = identityRequestDto;

        VoteDto voteDto = new VoteDto(identityServiceId, election.getEntityId());
        voteDto.setOperation(OperationType.SEND_VOTE);
        voteDto.setRevocationHash(this.revocationHash);
        voteDto.setElectionUUID(election.getUUID());
        voteDto.setOptionSelected(optionSelected);
        this.voteDto = voteDto;
        this.certificationRequest = CertificationRequest.getVoteRequest(identityServiceId, election.getEntityId(),
                election.getUUID(), this.revocationHash);
    }

    public static VoteRequest genRandomVote(ElectionDto election, String identityServiceId) throws Exception {
        ElectionOptionDto optionSelected = getRandomOption(election.getElectionOptions());
        return new VoteRequest(election, optionSelected, identityServiceId);
    }

    public static ElectionOptionDto getRandomOption(Set<ElectionOptionDto> options) {
        // In real life, the Random object should be rather more shared than this
        int item = new Random().nextInt(options.size());
        return (ElectionOptionDto) options.toArray()[item];
    }

    public ElectionDto getElection() {
        return election;
    }

    public VoteDto getVote() {
        return voteDto;
    }

    public IdentityRequestDto getAccessRequest() {
        return identityRequestDto;
    }

    public String getOriginHashIdentityRequest() {
        return originHashIdentityRequest;
    }

    public String getHashIdentityRequestBase64() {
        return hashIdentityRequestBase64;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public CertificationRequest getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(CertificationRequest certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

    public String getSubject() {
        if (election != null) return election.getSubject();
        else return null;
    }

}