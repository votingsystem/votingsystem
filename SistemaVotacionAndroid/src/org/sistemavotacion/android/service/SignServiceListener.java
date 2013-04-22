package org.sistemavotacion.android.service;

import org.sistemavotacion.smime.SMIMEMessageWrapper;


public interface SignServiceListener {

	void setSignServiceMsg(int statusCode, String msg);
	void proccessReceipt(SMIMEMessageWrapper receipt);
	void proccessEncryptedResponse(byte[] encryptedResponse);
	  
}
