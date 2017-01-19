package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.crypto.HashUtils;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.OperationType;

import java.security.NoSuchAlgorithmException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoteCancelerDto {

    private String originHashCertVote;
    private String hashCertVSBase64;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;
    private OperationType operation = OperationType.CANCEL_VOTE;
    private String UUID;


    public VoteCancelerDto() {}

    public void validate() throws ValidationException, NoSuchAlgorithmException {
        if(operation == null || OperationType.CANCEL_VOTE != operation) throw new ValidationException(
                "ERROR - expected operation 'CANCEL_VOTE' - found: " + operation);
        if(originHashCertVote == null) throw new ValidationException("ERROR - missing param 'originHashCertVote'");
        if(hashCertVSBase64 == null) throw new ValidationException("ERROR - missing param 'hashCertVSBase64'");
        if(hashAccessRequestBase64 == null) throw new ValidationException("ERROR - missing param 'hashAccessRequestBase64'");
        if(originHashAccessRequest == null) throw new ValidationException("ERROR - missing param 'originHashAccessRequest'");
        if(originHashAccessRequest == null) throw new ValidationException("ERROR - missing param 'originHashAccessRequest'");
        if(!hashAccessRequestBase64.equals(HashUtils.getHashBase64(originHashAccessRequest.getBytes(),
                Constants.DATA_DIGEST_ALGORITHM))) throw new ValidationException("voteCancellationAccessRequestHashError");
        if(!hashCertVSBase64.equals(HashUtils.getHashBase64(originHashCertVote.getBytes(),
                Constants.DATA_DIGEST_ALGORITHM))) throw new ValidationException("voteCancellationHashCertificateError");
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

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

}
