package org.votingsystem.test.util;

import org.votingsystem.signature.smime.SignedMailGenerator;

import java.security.KeyStore;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MockDNI {

    private String nif;
    private KeyStore keyStore;
    private SignedMailGenerator messageSigner;

    public MockDNI(String nif, KeyStore keyStore, SignedMailGenerator messageSigner) {
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

    public SignedMailGenerator getMessageSigner() {
        return messageSigner;
    }
}
