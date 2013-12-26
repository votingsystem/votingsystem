package org.votingsystem.model;

import android.util.Log;
import org.json.JSONObject;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;

import java.util.HashMap;
import java.util.Map;


public class QueryData {
	
	public static final String TAG = "QueryData";
	
	private SubSystemVS subsystem;
	private EventVS.State eventState;
	private String textQuery;
	
	public QueryData(SubSystemVS subsystem, EventVS.State eventState, String textQuery) {
		this.subsystem = subsystem;
		this.eventState = eventState;
		this.textQuery = textQuery;
	}

	public EventVS.State getEventState() {
		return eventState;
	}

	public void setEventState(EventVS.State eventState) {
		this.eventState = eventState;
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
		if(subsystem != null)
			map.put("typeVS", subsystem.toString());
		if(eventState != null)
			map.put("eventVSState", eventState.toString());
		if(textQuery != null)
			map.put("textQuery", textQuery);
	    return new JSONObject(map);
	}
	
}
