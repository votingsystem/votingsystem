package org.votingsystem.signature.util;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class KeyStoreInfo {

    private KeyStore keyStore;
    private PrivateKey privateKeySigner;
    private X509Certificate certSigner;

    public KeyStoreInfo(KeyStore keyStore, PrivateKey privateKeySigner, X509Certificate certSigner) {
        this.keyStore = keyStore;
        this.privateKeySigner = privateKeySigner;
        this.certSigner = certSigner;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public PrivateKey getPrivateKeySigner() {
        return privateKeySigner;
    }

    public X509Certificate getCertSigner() {
        return certSigner;
    }

}
