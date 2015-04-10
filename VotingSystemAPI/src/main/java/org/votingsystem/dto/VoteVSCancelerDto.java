package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.security.NoSuchAlgorithmException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoteVSCancelerDto {

    private String originHashCertVote;
    private String hashCertVSBase64;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;
    private TypeVS operation;

    public VoteVSCancelerDto() {}

    public void validate() throws ValidationExceptionVS, NoSuchAlgorithmException {
        if(operation == null || TypeVS.CANCEL_VOTE != operation) throw new ValidationExceptionVS(
                "ERROR - expected operation 'CANCEL_VOTE' - found: " + operation);
        if(originHashCertVote == null) throw new ValidationExceptionVS("ERROR - missing param 'originHashCertVote'");
        if(hashCertVSBase64 == null) throw new ValidationExceptionVS("ERROR - missing param 'hashCertVSBase64'");
        if(hashAccessRequestBase64 == null) throw new ValidationExceptionVS("ERROR - missing param 'hashAccessRequestBase64'");
        if(originHashAccessRequest == null) throw new ValidationExceptionVS("ERROR - missing param 'originHashAccessRequest'");
        if(originHashAccessRequest == null) throw new ValidationExceptionVS("ERROR - missing param 'originHashAccessRequest'");
        if(!hashAccessRequestBase64.equals(CMSUtils.getHashBase64(originHashAccessRequest,
                ContextVS.VOTING_DATA_DIGEST))) throw new ValidationExceptionVS("voteCancellationAccessRequestHashError");
        if(!hashCertVSBase64.equals(CMSUtils.getHashBase64(originHashCertVote,
                ContextVS.VOTING_DATA_DIGEST))) throw new ValidationExceptionVS("voteCancellationHashCertificateError");
    }

    public String getOriginHashCertVote() {
        return originHashCertVote;
    }

    public void setOriginHashCertVote(String originHashCertVote) {
        this.originHashCertVote = originHashCertVote;
    }

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public String getOriginHashAccessRequest() {
        return originHashAccessRequest;
    }

    public void setOriginHashAccessRequest(String originHashAccessRequest) {
        this.originHashAccessRequest = originHashAccessRequest;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }
}
