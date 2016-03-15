package org.votingsystem.dto.voting;


import org.votingsystem.model.KeyStore;

import java.security.cert.X509Certificate;

public class KeyStoreDto {

    private KeyStore keyStore;
    private X509Certificate x509Cert;

    public KeyStoreDto(KeyStore keyStore, X509Certificate x509Cert) {
        this.setKeyStore(keyStore);
        this.setX509Cert(x509Cert);
    }

    public X509Certificate getX509Cert() {
        return x509Cert;
    }

    public void setX509Cert(X509Certificate x509Cert) {
        this.x509Cert = x509Cert;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }
}
