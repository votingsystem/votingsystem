package org.votingsystem.util.crypto;

import java.security.PublicKey;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CsrResponse {

    private PublicKey publicKey;
    private byte[] issuedCert;
    private String hashCertVSBase64;

    public CsrResponse(PublicKey publicKey, byte[] issuedCert, String hashCertVSBase64) {
        this.publicKey = publicKey;
        this.issuedCert = issuedCert;
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public byte[] getIssuedCert() {
        return issuedCert;
    }

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setIssuedCert(byte[] issuedCert) {
        this.issuedCert = issuedCert;
    }
}
