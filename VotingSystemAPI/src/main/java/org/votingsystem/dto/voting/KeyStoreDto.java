package org.votingsystem.dto.voting;


import org.votingsystem.model.KeyStoreVS;
import java.security.cert.X509Certificate;

public class KeyStoreDto {

    private KeyStoreVS keyStoreVS;
    private X509Certificate x509Cert;

    public KeyStoreDto(KeyStoreVS keyStore, X509Certificate x509Cert) {
        this.setKeyStoreVS(keyStore);
        this.setX509Cert(x509Cert);
    }

    public X509Certificate getX509Cert() {
        return x509Cert;
    }

    public void setX509Cert(X509Certificate x509Cert) {
        this.x509Cert = x509Cert;
    }

    public KeyStoreVS getKeyStoreVS() {
        return keyStoreVS;
    }

    public void setKeyStoreVS(KeyStoreVS keyStoreVS) {
        this.keyStoreVS = keyStoreVS;
    }
}
