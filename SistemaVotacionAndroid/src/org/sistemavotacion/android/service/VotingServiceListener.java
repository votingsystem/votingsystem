package org.sistemavotacion.android.service;

import org.sistemavotacion.modelo.ReciboVoto;

import android.app.Service;

public interface VotingServiceListener {

	void setMsg(int statusCode, String msg);
	void proccessReceipt(ReciboVoto receipt);
	  
}
