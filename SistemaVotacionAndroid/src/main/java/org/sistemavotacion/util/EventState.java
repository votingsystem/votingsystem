package org.sistemavotacion.util;

import android.content.Context;

import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.Evento;


public enum EventState {
	
	OPEN, PENDING, CLOSED;
	
	public static EventState valueOf(int position)  {
        switch (position) {
	        case 0: return OPEN;
	        case 1: return PENDING;
	        case 2: return CLOSED;
	        default: return null;
        }
	}

    public int getposition()  {
        switch(this) {
            case OPEN: return 0;
            case PENDING: return 1;
            case CLOSED: return 2;
            default: return 0;
        }
    }

	public String getColor()  {
        switch(this) {
        	case OPEN: return "#6bad74";
        	case PENDING: return "#fba131";
        	case CLOSED: return "#cc1606";
        	default: return "#000000";
        }
	}
	
	public Evento.Estado getEventState() {
        switch(this) {
	    	case OPEN: return Evento.Estado.ACTIVO;
	    	case PENDING: return Evento.Estado.PENDIENTE_COMIENZO;
	    	case CLOSED: return Evento.Estado.FINALIZADO;
	    	default: return null;
        }
	}

    public String getDescription(SubSystem subSystem, Context context) {
        switch(subSystem) {
            case CLAIMS:
                switch(this) {
                    case OPEN:
                        return context.getString(R.string.open_claim_lbl);
                    case CLOSED:
                        return context.getString(R.string.closed_claim_lbl);
                    case PENDING:
                        return context.getString(R.string.pending_claim_lbl);
                    default:
                        return context.getString(R.string.unknown_event_state_lbl);
                }
            case MANIFESTS:
                switch(this) {
                    case OPEN:
                        return context.getString(R.string.open_manifest_lbl);
                    case CLOSED:
                        return context.getString(R.string.closed_manifest_lbl);
                    case PENDING:
                        return context.getString(R.string.pending_manifest_lbl);
                    default:
                        return context.getString(R.string.unknown_event_state_lbl);
                }
            case VOTING:
                switch(this) {
                    case OPEN:
                        return context.getString(R.string.open_voting_lbl);
                    case CLOSED:
                        return context.getString(R.string.closed_voting_lbl);
                    case PENDING:
                        return context.getString(R.string.pending_voting_lbl);
                    default:
                        return context.getString(R.string.unknown_event_state_lbl);
                }
            default:
                return context.getString(R.string.unknown_drop_down_lbl);
        }
    }
}
