package org.sistemavotacion.android.service;

import org.sistemavotacion.modelo.VoteReceipt;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

import android.app.Service;

public interface VotingServiceListener {

	void setMsg(int statusCode, String msg);
	void proccessReceipt(VoteReceipt receipt);
	void proccessReceipt(SMIMEMessageWrapper receipt);
	  
}
