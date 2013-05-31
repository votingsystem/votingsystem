package org.sistemavotacion.modelo;

import org.sistemavotacion.smime.SMIMEMessageWrapper;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class EncryptedBundle {
	
	public enum Type {SMIME_MESSAGE, TEXT, FILE} 
	
	private byte[] encryptedMessageBytes;
	private SMIMEMessageWrapper decryptedSMIMEMessage;
	private byte[] decryptedMessageBytes;
	private String message;
	private Type type;
	private int statusCode = Respuesta.SC_PROCESANDO;
	
	public EncryptedBundle (byte[] encryptedMessageBytes, Type type) {
		this.encryptedMessageBytes = encryptedMessageBytes;
		this.type = type;
	}

	public void setDecryptedMessageBytes(byte[] decryptedMessageBytes) {
		this.decryptedMessageBytes = decryptedMessageBytes;
	}

	public SMIMEMessageWrapper getDecryptedSMIMEMessage() {
		return decryptedSMIMEMessage;
	}
	
	public void setDecryptedSMIMEMessage(SMIMEMessageWrapper decryptedSMIMEMessage) {
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