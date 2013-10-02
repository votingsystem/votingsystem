package org.sistemavotacion.util;

import android.content.Context;

import org.sistemavotacion.android.R;
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

    public String getDescription(Context context) {
        switch(this) {
            case VOTING:
                return context.getString(R.string.voting_drop_down_lbl);
            case MANIFESTS:
                return context.getString(R.string.manifiests_drop_down_lbl);
            case CLAIMS:
                return context.getString(R.string.claims_drop_down_lbl);
            default:
                return context.getString(R.string.unknown_drop_down_lbl);
        }
    }

}