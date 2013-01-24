package org.sistemavotacion.android;

import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operation;

public class AppData {

	public static final AppData INSTANCE = new AppData();
	
	private Evento event;
	private Operation operation = null;
	
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
		this.operation = operation;
	}
}
