package org.sistemavotacion.util;

import org.sistemavotacion.modelo.Tipo;


public enum SubSystem  {
	
	VOTING(0, Tipo.EVENTO_VOTACION), 
	MANIFESTS(1, Tipo.EVENTO_FIRMA), 
	CLAIMS(2, Tipo.EVENTO_RECLAMACION), 
	UNKNOW(-1, null);
	
	int position;
	Tipo eventType;
	
	private SubSystem(int position, Tipo eventType) {
		this.position = position;
		this.eventType = eventType;
	}
	
	public static SubSystem valueOf(int position)  {
        switch (position) {
	        case 0: return VOTING;
	        case 1: return MANIFESTS;
	        case 2: return CLAIMS;
	        default: return UNKNOW;
        }
	}
	
	public int getPosition() {
		return position;
	}
	
	public Tipo getEventType() {
        return eventType;
	}
	
}