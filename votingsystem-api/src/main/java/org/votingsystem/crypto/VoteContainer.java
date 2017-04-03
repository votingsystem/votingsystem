package org.votingsystem.crypto;

import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.voting.ElectionDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.dto.metadata.SystemEntityDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
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
public class VoteContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originHashIdentityRequest;
    private String hashIdentityRequestBase64;
    private String originRevocationHash;
    private String revocationHashBase64;
    private VoteDto voteDto;
    private ElectionDto election;
    private IdentityRequestDto identityRequestDto;
    private CertificationRequest certificationRequest;

    public VoteContainer() throws Exception {}


    public static VoteContainer generate(ElectionDto election, ElectionOptionDto optionSelected,
                                         String identityServiceId) throws Exception {
        VoteContainer result = new VoteContainer();
        result.originHashIdentityRequest = UUID.randomUUID().toString();
        result.hashIdentityRequestBase64 = HashUtils.getHashBase64(
                result.originHashIdentityRequest.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        result.originRevocationHash = UUID.randomUUID().toString();
        result.revocationHashBase64 = HashUtils.getHashBase64(
                result.originRevocationHash.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
        result.election = election;

        IdentityRequestDto identityRequestDto = new IdentityRequestDto();
        identityRequestDto.setUUID(election.getUUID());
        identityRequestDto.setCallbackServiceEntityId(new SystemEntityDto(election.getEntityId(),
                SystemEntityType.VOTING_SERVICE_PROVIDER));
        identityRequestDto.setRevocationHashBase64(result.hashIdentityRequestBase64);
        identityRequestDto.setUUID(UUID.randomUUID().toString());
        result.identityRequestDto = identityRequestDto;

        VoteDto voteDto = new VoteDto(identityServiceId, election.getEntityId());
        voteDto.setOperation(OperationType.SEND_VOTE);
        voteDto.setRevocationHashBase64(result.revocationHashBase64);
        voteDto.setElectionUUID(election.getUUID());
        voteDto.setOptionSelected(optionSelected);
        result.voteDto = voteDto;
        result.certificationRequest = CertificationRequest.getVoteRequest(identityServiceId, election.getEntityId(),
                election.getUUID(), result.revocationHashBase64);
        return result;
    }

    public static VoteContainer genRandomVote(ElectionDto election, String identityServiceId) throws Exception {
        ElectionOptionDto optionSelected = getRandomOption(election.getElectionOptions());
        return  VoteContainer.generate(election, optionSelected, identityServiceId);
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

    public String getRevocationHashBase64() {
        return revocationHashBase64;
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