package org.votingsystem.dto;

import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class KeyStoreDto implements Serializable {

    private String password;
    private byte[] keyStoreBytes;
    private String keyAlias;

    public KeyStoreDto() { }

    public KeyStoreDto(String keyAlias, byte[] keyStoreBytes, String password) {
        this.keyAlias = keyAlias;
        this.password = password;
        this.keyStoreBytes = keyStoreBytes;
    }

    public String getPassword() {
        return password;
    }

    public KeyStoreDto setPassword(String password) {
        this.password = password;
        return this;
    }

    public byte[] getKeyStoreBytes() {
        return keyStoreBytes;
    }

    public KeyStoreDto setKeyStoreBytes(byte[] keyStoreBytes) {
        this.keyStoreBytes = keyStoreBytes;
        return this;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public KeyStoreDto setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
        return this;
    }

}
