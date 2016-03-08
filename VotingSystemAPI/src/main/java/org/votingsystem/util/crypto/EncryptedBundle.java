package org.votingsystem.util.crypto;


public class EncryptedBundle {

    byte[] iv, cipherText, salt;

    public EncryptedBundle(byte[] cipherText, byte[] iv, byte[] salt) {
        this.iv = iv;
        this.cipherText = cipherText;
        this.salt = salt;
    }

    public byte[] getIV() { return iv; }
    public byte[] getCipherText() { return cipherText; }
    public byte[] getSalt() { return salt; }

}
