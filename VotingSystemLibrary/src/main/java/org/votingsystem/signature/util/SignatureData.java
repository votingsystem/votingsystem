package org.votingsystem.signature.util;

import org.bouncycastle.util.Store;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureData {

    private X509Certificate signingCert;
    private PrivateKey signingKey;
    private Store certs;

    public SignatureData(X509Certificate signingCert, PrivateKey signingKey, Store certs) {
        this.setSigningCert(signingCert);
        this.setSigningKey(signingKey);
        this.setCerts(certs);
    }

    public X509Certificate getSigningCert() {
        return signingCert;
    }

    public void setSigningCert(X509Certificate signingCert) {
        this.signingCert = signingCert;
    }

    public PrivateKey getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(PrivateKey signingKey) {
        this.signingKey = signingKey;
    }

    public Store getCerts() {
        return certs;
    }

    public void setCerts(Store certs) {
        this.certs = certs;
    }

}
