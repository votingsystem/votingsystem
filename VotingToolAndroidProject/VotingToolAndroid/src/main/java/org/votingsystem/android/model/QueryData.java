package org.votingsystem.android.model;

import android.util.Log;
import org.json.JSONObject;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;

import java.util.HashMap;
import java.util.Map;


public class QueryData {
	
	public static final String TAG = "QueryData";
	
	private TypeVS typeVS;
	private EventVS.State eventVSState;
	private String textQuery;
	
	public QueryData(TypeVS typeVS, EventVS.State eventVSState,
                     String textQuery) {
		this.typeVS = typeVS;
		this.eventVSState = eventVSState;
		this.textQuery = textQuery;
	}

	public TypeVS getTypeVS() {
		return typeVS;
	}

	public void setTypeVS(TypeVS typeVS) {
		this.typeVS = typeVS;
	}

	public EventVS.State getEventVSState() {
		return eventVSState;
	}

	public void setEventVSState(EventVS.State eventVSState) {
		this.eventVSState = eventVSState;
	}

	public String getTextQuery() {
		return textQuery;
	}

	public void setTextQuery(String textQuery) {
		this.textQuery = textQuery;
	}
	
	public JSONObject toJSON() {
		Log.d(TAG + ".toJSON(...)", " - toJSON");
		Map<String, Object> map = new HashMap<String, Object>();
		if(typeVS != null)
			map.put("typeVS", typeVS.toString());
		if(eventVSState != null)
			map.put("eventVSState", eventVSState.toString());
		if(textQuery != null)
			map.put("textQuery", textQuery);
	    return new JSONObject(map);
	}
	
}
