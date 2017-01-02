package org.votingsystem.crypto;

import java.security.PublicKey;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CsrResponse {

    private int statusCode;
    private String message;
    private PublicKey publicKey;
    private byte[] issuedCert;
    private String revocationHashBase64;

    public CsrResponse(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public CsrResponse(PublicKey publicKey, byte[] issuedCert, String revocationHashBase64) {
        this.publicKey = publicKey;
        this.issuedCert = issuedCert;
        this.revocationHashBase64 = revocationHashBase64;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public byte[] getIssuedCert() {
        return issuedCert;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public void setIssuedCert(byte[] issuedCert) {
        this.issuedCert = issuedCert;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public CsrResponse setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public CsrResponse setMessage(String message) {
        this.message = message;
        return this;
    }

}
