package org.votingsystem.util.crypto;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.voting.AccessRequestDto;
import org.votingsystem.dto.voting.VoteVSCancelerDto;
import org.votingsystem.dto.voting.VoteVSDto;
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
public class VoteVSHelper {

    private Long eventVSId;
    private String eventVSURL;
    private String NIF;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;
    private String originHashCertVote;
    private String hashCertVSBase64;
    private CMSSignedMessage validatedVote;
    private VoteVSDto voteVSDto;
    private VoteVSCancelerDto cancelerDto;
    private AccessRequestDto accessRequestDto;


    public static VoteVSHelper load(VoteVSDto voteVSDto) throws NoSuchAlgorithmException {
        VoteVSHelper voteVSHelper = new VoteVSHelper();
        voteVSHelper.originHashAccessRequest = UUID.randomUUID().toString();
        voteVSHelper.hashAccessRequestBase64 = StringUtils.getHashBase64(
                voteVSHelper.originHashAccessRequest, ContextVS.DATA_DIGEST_ALGORITHM);
        voteVSHelper.originHashCertVote = UUID.randomUUID().toString();
        voteVSHelper.hashCertVSBase64 = StringUtils.getHashBase64(
                voteVSHelper.originHashCertVote, ContextVS.DATA_DIGEST_ALGORITHM);
        voteVSHelper.eventVSId = voteVSDto.getEventVSId();
        voteVSHelper.eventVSURL = voteVSDto.getEventURL();
        voteVSHelper.genVote(voteVSDto.getOptionSelected());
        return voteVSHelper;
    }

    public static VoteVSHelper genRandomVote(Long eventVSId, String eventVSURL, Set<FieldEventVS> options)
            throws NoSuchAlgorithmException {
        VoteVSDto voteVSDto =  new VoteVSDto();
        voteVSDto.setEventVSId(eventVSId);
        voteVSDto.setEventURL(eventVSURL);
        voteVSDto.setOptionSelected(getRandomOption(options));
        return VoteVSHelper.load(voteVSDto);
    }

    public static FieldEventVS getRandomOption (Set<FieldEventVS> options) {
        int item = new Random().nextInt(options.size()); // In real life, the Random object should be rather more shared than this
        return (FieldEventVS) options.toArray()[item];
    }

    private void genVote(FieldEventVS optionSelected) {
        genAccessRequest();
        voteVSDto = new VoteVSDto();
        voteVSDto.setOperation(TypeVS.SEND_VOTE);
        voteVSDto.setHashCertVSBase64(hashCertVSBase64);
        voteVSDto.setEventVSId(eventVSId);
        voteVSDto.setEventURL(eventVSURL);
        voteVSDto.setOptionSelected(optionSelected);
        voteVSDto.setUUID(UUID.randomUUID().toString());
    }


    private void genAccessRequest() {
        accessRequestDto = new AccessRequestDto();
        accessRequestDto.setEventId(eventVSId);
        accessRequestDto.setEventURL(eventVSURL);
        accessRequestDto.setHashAccessRequestBase64(hashAccessRequestBase64);
        accessRequestDto.setUUID(UUID.randomUUID().toString());
    }

    public VoteVSDto getVote() {
        return voteVSDto;
    }

    public VoteVSCancelerDto getVoteCanceler() {
        if(cancelerDto == null) {
            cancelerDto = new VoteVSCancelerDto();
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
