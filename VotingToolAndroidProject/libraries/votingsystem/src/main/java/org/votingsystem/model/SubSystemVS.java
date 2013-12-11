package org.votingsystem.model;

import android.content.Context;
import org.votingsystem.android.R;


public enum SubSystemVS {
	
	VOTING(0, TypeVS.VOTING_EVENT),
	MANIFESTS(1, TypeVS.MANIFEST_EVENT),
	CLAIMS(2, TypeVS.CLAIM_EVENT),
	UNKNOW(-1, null);
	
	int position;
	TypeVS eventType;
	
	private SubSystemVS(int position, TypeVS eventType) {
		this.position = position;
		this.eventType = eventType;
	}
	
	public static SubSystemVS valueOf(int position)  {
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
	
	public TypeVS getEventType() {
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