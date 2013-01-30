package org.sistemavotacion.android;

import java.util.HashMap;
import java.util.Map;

import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operation;
import org.sistemavotacion.modelo.VoteReceipt;

import android.util.Log;

public class AppData {
	
	public static final String TAG = "AppData";

	public static final AppData INSTANCE = new AppData();
	
	private Evento event;
	private Operation operation = null;
	private Map<String, VoteReceipt> receiptsMap = new HashMap<String, VoteReceipt>();
	
	public void setEvent(Evento event) {
		this.event = event;
	}
	
	public Evento getEvent() {
		return event;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		if(operation == null) Log.d(TAG + ".setOperation(...)", "- removing pending operation");
		else Log.d(TAG + ".setOperation(...)", "- operation: " + operation.getTipo());
		this.operation = operation;
	}
	
	public void putReceipt(String key, VoteReceipt receipt) {
		receiptsMap.put(key, receipt);
	}
	
	public VoteReceipt getReceipt(String key) {
		return receiptsMap.get(key);
	}
	
	public void removeReceipt(String key) {
		receiptsMap.remove(key);
	}
}
