package org.votingsystem.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class EventVSResponse implements Serializable {

    private static final long serialVersionUID = 1L;

	public static final String TAG = EventVSResponse.class.getSimpleName();

    private Integer numEventsVSManifest;
    private Integer numEventsVSManifestInSystem;
    private Integer numEventsVSElection;
    private Integer numEventsVSElectionInSystem;
    private Integer numEventsVSClaim;
    private Integer numEventsVSClaimInSystem;
    private Integer numEventVSInRequest;
    private Integer numEventVSInSystem;
    
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
    
	public static EventVSResponse parse(String requestStr, TypeVS eventType) throws ParseException,
            JSONException {
    	JSONObject jsonObject = new JSONObject (requestStr);
        List<EventVS> eventList = new ArrayList<EventVS>();
        JSONArray eventsArray = jsonObject.getJSONArray("eventVS");
        EventVSResponse eventVSResponse = new EventVSResponse();
        for (int i=0; i < eventsArray.length(); i++) {
            EventVS eventVS = EventVS.parse(eventsArray.getJSONObject(i));
            eventVS.setTypeVS(eventType);
            eventList.add(eventVS);
        }
        switch(eventType)  {
            case VOTING_EVENT:
                eventVSResponse.setNumEventsVSElection(eventsArray.length());
                eventVSResponse.setNumEventsVSElectionInSystem(jsonObject.getInt("totalCount"));
                break;
            case CLAIM_EVENT:
                eventVSResponse.setNumEventsVSClaim(eventsArray.length());
                eventVSResponse.setNumEventsVSClaimInSystem(jsonObject.getInt("totalCount"));
                break;
            case MANIFEST_EVENT:
                eventVSResponse.setNumEventsVSManifest(eventsArray.length());
                eventVSResponse.setNumEventsVSManifestInSystem(jsonObject.getInt("totalCount"));
                break;
            default:
                Log.d(TAG + ".parse(...)", "unknown eventType: " + eventType.toString());

        }
        eventVSResponse.setNumEventVSInRequest(eventsArray.length());
        eventVSResponse.setNumEventVSInSystem(jsonObject.getInt("totalCount"));
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

    public Integer getNumEventsVSManifestInSystem() {
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

    public Integer getNumEventsVSElectionInSystem() {
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

    public Integer getNumEventsVSClaimInSystem() {
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
