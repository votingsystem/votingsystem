package org.votingsystem.client.util;

import org.votingsystem.dto.voting.VoteDto;
import org.votingsystem.model.User;

import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureValidationResult {

    private Integer statusCode;
    private String message;

    public SignatureValidationResult() {}

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public User getSigner() {
        return null;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public X509Certificate getSigningCert() {
        return null;
    }

    public VoteDto getVote() {
        return null;
    }

}
