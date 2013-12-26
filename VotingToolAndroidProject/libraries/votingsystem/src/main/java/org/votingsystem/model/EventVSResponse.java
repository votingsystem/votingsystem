package org.votingsystem.model;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class EventVSResponse {
	
	public static final String TAG = "EventVSResponse";

    private int numEventsVSManifest;
    private int numEventsVSManifestInSystem;
    private int numEventsVSElection;
    private int numEventsVSElectionInSystem;
    private int numEventsVSClaim;
    private int numEventsVSClaimInSystem;
    private int numEventVSInRequest;
    private int numEventVSInSystem;
    
    private int offset;
    private List<EventVS> eventList;


    public void setOffset(int offset) {
            this.offset = offset;
    }

    public int getOffset() {
            return offset;
    }


	public List<EventVS> getEvents() {
		return eventList;
	}

	public void setEvents(List<EventVS> eventList) {
		this.eventList = eventList;
	}
    
	public static EventVSResponse parse(String requestStr) throws ParseException, JSONException {
    	Log.d(TAG + ".parse(...)", "parse(...)");
    	JSONObject jsonObject = new JSONObject (requestStr);
        List<EventVS> eventList = new ArrayList<EventVS>();
        JSONObject requestJSON = jsonObject.getJSONObject("eventsVS");
        JSONArray eventsArray;
        if (requestJSON != null) {
        	if(requestJSON.has("manifests")) {
                eventsArray = requestJSON.getJSONArray("manifests");
                if (eventsArray != null) {
                    for (int i=0; i<eventsArray.length(); i++) {
                        EventVS eventVS = EventVS.parse(eventsArray.getJSONObject(i));
                        eventVS.setTypeVS(TypeVS.MANIFEST_EVENT);
                        eventList.add(eventVS);
                    }
                }	
        	}
        	if(requestJSON.has("claims")) {
                eventsArray = requestJSON.getJSONArray("claims");
                if (eventsArray != null) {
                    for (int i=0; i<eventsArray.length(); i++) {
                        EventVS eventVS = EventVS.parse(eventsArray.getJSONObject(i));
                        eventVS.setTypeVS(TypeVS.CLAIM_EVENT);
                        eventList.add(eventVS);
                    }
                }	
        	}
        	if(requestJSON.has("elections")) {
                eventsArray = requestJSON.getJSONArray("elections");
                if (eventsArray != null) {
                    for (int i=0; i<eventsArray.length(); i++) {
                        EventVS eventVS = EventVS.parse(eventsArray.getJSONObject(i));
                        eventVS.setTypeVS(TypeVS.VOTING_EVENT);
                        eventList.add(eventVS);
                    }
                }	
        	}
        }
        EventVSResponse eventVSResponse = new EventVSResponse();
        if(requestJSON.has("numEventsVSManifest"))
        	eventVSResponse.setNumEventsVSManifest(jsonObject.getInt("numEventsVSManifest"));
        if(requestJSON.has("numEventsVSManifestInSystem"))
        	eventVSResponse.setNumEventsVSManifestInSystem(jsonObject.getInt("numEventsVSManifestInSystem"));
        if(requestJSON.has("numEventsVSElection"))
        	eventVSResponse.setNumEventsVSElection(jsonObject.getInt("numEventsVSElection"));
        if(requestJSON.has("numEventsVSElectionInSystem"))
        	eventVSResponse.setNumEventsVSElectionInSystem(jsonObject.getInt("numEventsVSElectionInSystem"));
        if(requestJSON.has("numEventsVSClaim"))
        	eventVSResponse.setNumEventsVSClaim(jsonObject.getInt("numEventsVSClaim"));
        if(requestJSON.has("numEventsVSClaimInSystem"))
        	eventVSResponse.setNumEventsVSClaimInSystem(jsonObject.getInt("numEventsVSClaimInSystem"));
        if(requestJSON.has("numEventVSInRequest"))
        	eventVSResponse.setNumEventVSInRequest(jsonObject.getInt("numEventVSInRequest"));
        if(requestJSON.has("numEventVSInSystem"))
        	eventVSResponse.setNumEventVSInSystem(jsonObject.getInt("numEventVSInSystem"));
        if (jsonObject.has("offset")) eventVSResponse.setOffset(jsonObject.getInt("offset"));
        eventVSResponse.setEvents(eventList);
        return eventVSResponse;
    }

    public int getNumEventsVSManifest() {
        return numEventsVSManifest;
    }

    public void setNumEventsVSManifest(int numEventsVSManifest) {
        this.numEventsVSManifest = numEventsVSManifest;
    }

    public int getNumEventsVSManifestInSystem() {
        return numEventsVSManifestInSystem;
    }

    public void setNumEventsVSManifestInSystem(int numEventsVSManifestInSystem) {
        this.numEventsVSManifestInSystem = numEventsVSManifestInSystem;
    }

    public int getNumEventsVSElection() {
        return numEventsVSElection;
    }

    public void setNumEventsVSElection(int numEventsVSElection) {
        this.numEventsVSElection = numEventsVSElection;
    }

    public int getNumEventsVSElectionInSystem() {
        return numEventsVSElectionInSystem;
    }

    public void setNumEventsVSElectionInSystem(int numEventsVSElectionInSystem) {
        this.numEventsVSElectionInSystem = numEventsVSElectionInSystem;
    }

    public int getNumEventsVSClaim() {
        return numEventsVSClaim;
    }

    public void setNumEventsVSClaim(int numEventsVSClaim) {
        this.numEventsVSClaim = numEventsVSClaim;
    }

    public int getNumEventsVSClaimInSystem() {
        return numEventsVSClaimInSystem;
    }

    public void setNumEventsVSClaimInSystem(int numEventsVSClaimInSystem) {
        this.numEventsVSClaimInSystem = numEventsVSClaimInSystem;
    }

    public int getNumEventVSInRequest() {
        return numEventVSInRequest;
    }

    public void setNumEventVSInRequest(int numEventVSInRequest) {
        this.numEventVSInRequest = numEventVSInRequest;
    }

    public int getNumEventVSInSystem() {
        return numEventVSInSystem;
    }

    public void setNumEventVSInSystem(int numEventVSInSystem) {
        this.numEventVSInSystem = numEventVSInSystem;
    }
}
