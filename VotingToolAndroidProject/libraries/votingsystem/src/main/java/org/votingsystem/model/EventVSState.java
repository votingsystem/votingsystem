package org.votingsystem.model;

import android.content.Context;
import org.votingsystem.android.R;


public enum EventVSState {
	
	OPEN, PENDING, CLOSED;
	
	public static EventVSState valueOf(int position)  {
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
	
	public EventVS.State getEventState() {
        switch(this) {
	    	case OPEN: return EventVS.State.ACTIVE;
	    	case PENDING: return EventVS.State.AWAITING;
	    	case CLOSED: return EventVS.State.TERMINATED;
	    	default: return null;
        }
	}

    public String getDescription(SubSystemVS subSystemVS, Context context) {
        switch(subSystemVS) {
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
