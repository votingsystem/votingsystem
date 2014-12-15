package org.votingsystem.model;

import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EncryptedBundleVS {

    public enum Type {SMIME_MESSAGE, TEXT, FILE}

    private byte[] encryptedMessageBytes;
    private SMIMEMessage decryptedSMIMEMessage;
    private byte[] decryptedMessageBytes;
    private String message;
    private Type type;
    private int statusCode = ResponseVS.SC_PROCESSING;

    public EncryptedBundleVS(byte[] encryptedMessageBytes, Type type) {
        this.encryptedMessageBytes = encryptedMessageBytes;
        this.type = type;
    }

    public void setDecryptedMessageBytes(byte[] decryptedMessageBytes) {
        this.decryptedMessageBytes = decryptedMessageBytes;
    }

    public SMIMEMessage getDecryptedSMIMEMessage() {
        return decryptedSMIMEMessage;
    }

    public void setDecryptedSMIMEMessage(SMIMEMessage decryptedSMIMEMessage) {
        this.decryptedSMIMEMessage = decryptedSMIMEMessage;
    }

    public byte[] getEncryptedMessageBytes() {
        return encryptedMessageBytes;
    }

    public void setEncryptedMessageBytes(byte[] encryptedMessageBytes) {
        this.encryptedMessageBytes = encryptedMessageBytes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

}