package org.votingsystem.util.crypto;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.VoteCancelerDto;
import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.model.voting.FieldEventVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VoteHelper {

    private Long EventId;
    private String eventVSURL;
    private String NIF;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;
    private String originHashCertVote;
    private String hashCertVSBase64;
    private CMSSignedMessage validatedVote;
    private VoteDto voteDto;
    private VoteCancelerDto cancelerDto;
    private AccessRequestDto accessRequestDto;


    public static VoteHelper load(VoteDto voteDto) throws NoSuchAlgorithmException {
        VoteHelper voteHelper = new VoteHelper();
        voteHelper.originHashAccessRequest = UUID.randomUUID().toString();
        voteHelper.hashAccessRequestBase64 = StringUtils.getHashBase64(
                voteHelper.originHashAccessRequest, ContextVS.DATA_DIGEST_ALGORITHM);
        voteHelper.originHashCertVote = UUID.randomUUID().toString();
        voteHelper.hashCertVSBase64 = StringUtils.getHashBase64(
                voteHelper.originHashCertVote, ContextVS.DATA_DIGEST_ALGORITHM);
        voteHelper.EventId = voteDto.getEventId();
        voteHelper.eventVSURL = voteDto.getEventURL();
        voteHelper.genVote(voteDto.getOptionSelected());
        return voteHelper;
    }

    public static VoteHelper genRandomVote(Long EventId, String eventVSURL, Set<FieldEventVS> options)
            throws NoSuchAlgorithmException {
        VoteDto voteDto =  new VoteDto();
        voteDto.setEventId(EventId);
        voteDto.setEventURL(eventVSURL);
        voteDto.setOptionSelected(getRandomOption(options));
        return VoteHelper.load(voteDto);
    }

    public static FieldEventVS getRandomOption (Set<FieldEventVS> options) {
        int item = new Random().nextInt(options.size()); // In real life, the Random object should be rather more shared than this
        return (FieldEventVS) options.toArray()[item];
    }

    private void genVote(FieldEventVS optionSelected) {
        genAccessRequest();
        voteDto = new VoteDto();
        voteDto.setOperation(TypeVS.SEND_VOTE);
        voteDto.setHashCertVSBase64(hashCertVSBase64);
        voteDto.setEventId(EventId);
        voteDto.setEventURL(eventVSURL);
        voteDto.setOptionSelected(optionSelected);
        voteDto.setUUID(UUID.randomUUID().toString());
    }


    private void genAccessRequest() {
        accessRequestDto = new AccessRequestDto();
        accessRequestDto.setEventId(EventId);
        accessRequestDto.setEventURL(eventVSURL);
        accessRequestDto.setHashAccessRequestBase64(hashAccessRequestBase64);
        accessRequestDto.setUUID(UUID.randomUUID().toString());
    }

    public VoteDto getVote() {
        return voteDto;
    }

    public VoteCancelerDto getVoteCanceler() {
        if(cancelerDto == null) {
            cancelerDto = new VoteCancelerDto();
            cancelerDto.setOperation(TypeVS.CANCEL_VOTE);
            cancelerDto.setOriginHashAccessRequest(originHashAccessRequest);
            cancelerDto.setHashAccessRequestBase64(hashAccessRequestBase64);
            cancelerDto.setOriginHashCertVote(originHashCertVote);
            cancelerDto.setHashCertVSBase64(hashCertVSBase64);
            cancelerDto.setUUID(UUID.randomUUID().toString());
        }
        return cancelerDto;
    }

    public AccessRequestDto getAccessRequest() {
        return accessRequestDto;
    }

    public CMSSignedMessage getValidatedVote() {
        return validatedVote;
    }

    public void setValidatedVote(CMSSignedMessage validatedVote) {
        this.validatedVote = validatedVote;
    }

    public String getOriginHashAccessRequest() {
        return originHashAccessRequest;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public String getOriginHashCertVote() {
        return originHashCertVote;
    }

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public String getNIF() {
        return NIF;
    }

    public void setNIF(String NIF) {
        this.NIF = NIF;
    }

}
