package org.votingsystem.test.util;


import java.security.KeyStore;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DNIBundle {

    private String nif;
    private KeyStore keyStore;

    public DNIBundle(String nif, KeyStore keyStore) {
        this.nif = nif;
        this.keyStore = keyStore;
    }

    public String getNif() {
        return nif;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

}
