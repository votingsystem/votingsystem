package org.votingsystem.test.util;

import org.votingsystem.signature.smime.SMIMESignedGeneratorVS;

import java.security.KeyStore;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MockDNI {

    private String nif;
    private KeyStore keyStore;
    private SMIMESignedGeneratorVS messageSigner;

    public MockDNI(String nif, KeyStore keyStore, SMIMESignedGeneratorVS messageSigner) {
        this.nif = nif;
        this.keyStore = keyStore;
        this.messageSigner = messageSigner;
    }

    public String getNif() {
        return nif;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public SMIMESignedGeneratorVS getMessageSigner() {
        return messageSigner;
    }
}
