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
    private List<EventVS> eventVSes;


    public void setOffset(int offset) {
            this.offset = offset;
    }

    public int getOffset() {
            return offset;
    }

    public int getNumeroEventosFirmaEnPeticion() {
        return numEventsVSManifest;
    }

    public void setNumeroEventosFirmaEnPeticion(int numEventsVSManifest) {
        this.numEventsVSManifest = numEventsVSManifest;
    }

    public int getNumeroTotalEventosFirmaEnSistema() {
        return numEventsVSManifestInSystem;
    }

    public void setNumeroTotalEventosFirmaEnSistema(int numEventsVSManifestInSystem) {
        this.numEventsVSManifestInSystem = numEventsVSManifestInSystem;
    }

    /**
     * @return the numEventsVSElection
     */
    public int getNumeroEventosVotacionEnPeticion() {
        return numEventsVSElection;
    }

    /**
     * @param numEventsVSElection the numEventsVSElection to set
     */
    public void setNumeroEventosVotacionEnPeticion(int numEventsVSElection) {
        this.numEventsVSElection = numEventsVSElection;
    }

    /**
     * @return the numEventsVSElectionInSystem
     */
    public int getNumeroTotalEventosVotacionEnSistema() {
        return numEventsVSElectionInSystem;
    }

    /**
     * @param numEventsVSElectionInSystem the numEventsVSElectionInSystem to set
     */
    public void setNumeroTotalEventosVotacionEnSistema(int numEventsVSElectionInSystem) {
        this.numEventsVSElectionInSystem = numEventsVSElectionInSystem;
    }

    /**
     * @return the numEventsVSClaim
     */
    public int getNumeroEventosReclamacionEnPeticion() {
        return numEventsVSClaim;
    }

    /**
     * @param numEventsVSClaim the numEventsVSClaim to set
     */
    public void setNumeroEventosReclamacionEnPeticion(int numEventsVSClaim) {
        this.numEventsVSClaim = numEventsVSClaim;
    }

    /**
     * @return the numEventsVSClaimInSystem
     */
    public int getNumeroTotalEventosReclamacionEnSistema() {
        return numEventsVSClaimInSystem;
    }

    /**
     * @param numEventsVSClaimInSystem the numEventsVSClaimInSystem to set
     */
    public void setNumeroTotalEventosReclamacionEnSistema(int numEventsVSClaimInSystem) {
        this.numEventsVSClaimInSystem = numEventsVSClaimInSystem;
    }

    /**
     * @return the numEventVSInRequest
     */
    public int getNumeroEventosEnPeticion() {
        return numEventVSInRequest;
    }

    /**
     * @param numEventVSInRequest the numEventVSInRequest to set
     */
    public void setNumeroEventosEnPeticion(int numEventVSInRequest) {
        this.numEventVSInRequest = numEventVSInRequest;
    }

    /**
     * @return the numEventVSInSystem
     */
    public int getNumeroTotalEventosEnSistema() {
        return numEventVSInSystem;
    }

    /**
     * @param numEventVSInSystem the numEventVSInSystem to set
     */
    public void setNumeroTotalEventosEnSistema(int numEventVSInSystem) {
        this.numEventVSInSystem = numEventVSInSystem;
    }

	public List<EventVS> getEventVSs() {
		return eventVSes;
	}

	public void setEventVSs(List<EventVS> eventVSes) {
		this.eventVSes = eventVSes;
	}
    
	public static EventVSResponse parse(String consultaStr) throws ParseException, JSONException {
    	Log.d(TAG + ".parse(...)", "parse(...)");
    	JSONObject jsonObject = new JSONObject (consultaStr);
        List<EventVS> eventVSes = new ArrayList<EventVS>();
        JSONObject jsonEventos = jsonObject.getJSONObject("eventVSs");
        JSONArray arrayEventos;
        if (jsonEventos != null) {
        	if(jsonEventos.has("firmas")) {
                arrayEventos = jsonEventos.getJSONArray("firmas");
                if (arrayEventos != null) {
                    for (int i=0; i<arrayEventos.length(); i++) {
                        EventVS eventVS = EventVS.parse(arrayEventos.getJSONObject(i));
                        eventVS.setTypeVS(TypeVS.MANIFEST_EVENT);
                        eventVSes.add(eventVS);
                    }
                }	
        	}
        	if(jsonEventos.has("claims")) {
                arrayEventos = jsonEventos.getJSONArray("claims");
                if (arrayEventos != null) {
                    for (int i=0; i<arrayEventos.length(); i++) {
                        EventVS eventVS = EventVS.parse(arrayEventos.getJSONObject(i));
                        eventVS.setTypeVS(TypeVS.CLAIM_EVENT);
                        eventVSes.add(eventVS);
                    }
                }	
        	}
        	if(jsonEventos.has("elections")) {
                arrayEventos = jsonEventos.getJSONArray("elections");
                if (arrayEventos != null) {
                    for (int i=0; i<arrayEventos.length(); i++) {
                        EventVS eventVS = EventVS.parse(arrayEventos.getJSONObject(i));
                        eventVS.setTypeVS(TypeVS.VOTING_EVENT);
                        eventVSes.add(eventVS);
                    }
                }	
        	}
        }
        EventVSResponse eventVSResponse = new EventVSResponse();
        if(jsonEventos.has("numEventsVSManifest"))
        	eventVSResponse.setNumeroEventosFirmaEnPeticion(jsonObject.getInt("numEventsVSManifest"));
        if(jsonEventos.has("numEventsVSManifestInSystem"))
        	eventVSResponse.setNumeroTotalEventosFirmaEnSistema(jsonObject.getInt("numEventsVSManifestInSystem"));
        if(jsonEventos.has("numEventsVSElection"))
        	eventVSResponse.setNumeroEventosVotacionEnPeticion(jsonObject.getInt("numEventsVSElection"));
        if(jsonEventos.has("numEventsVSElectionInSystem"))
        	eventVSResponse.setNumeroTotalEventosVotacionEnSistema(jsonObject.getInt("numEventsVSElectionInSystem"));
        if(jsonEventos.has("numEventsVSClaim"))
        	eventVSResponse.setNumeroEventosReclamacionEnPeticion(jsonObject.getInt("numEventsVSClaim"));
        if(jsonEventos.has("numEventsVSClaimInSystem"))
        	eventVSResponse.setNumeroTotalEventosReclamacionEnSistema(jsonObject.getInt("numEventsVSClaimInSystem"));
        if(jsonEventos.has("numEventVSInRequest"))
        	eventVSResponse.setNumeroEventosEnPeticion(jsonObject.getInt("numEventVSInRequest"));
        if(jsonEventos.has("numEventVSInSystem"))
        	eventVSResponse.setNumeroTotalEventosEnSistema(jsonObject.getInt("numEventVSInSystem"));
        if (jsonObject.has("offset"))
            eventVSResponse.setOffset(jsonObject.getInt("offset"));
        eventVSResponse.setEventVSs(eventVSes);
        return eventVSResponse;
    }
	
}
