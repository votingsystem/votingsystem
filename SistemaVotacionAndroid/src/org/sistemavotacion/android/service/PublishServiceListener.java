package org.sistemavotacion.android.service;

import org.sistemavotacion.smime.SMIMEMessageWrapper;


public interface PublishServiceListener {

	void setPublishServiceMsg(int statusCode, String msg);
	void proccessReceipt(SMIMEMessageWrapper receipt);
	  
}
