package org.votingsystem.android.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.model.EventQueryResponseVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class EventQueryResponse implements EventQueryResponseVS {
	
	public static final String TAG = "EventQueryResponse";

    private int numeroEventosFirmaEnPeticion;
    private int numeroTotalEventosFirmaEnSistema;
    private int numeroEventosVotacionEnPeticion;
    private int numeroTotalEventosVotacionEnSistema;
    private int numeroEventosReclamacionEnPeticion;
    private int numeroTotalEventosReclamacionEnSistema;
    private int numeroEventosEnPeticion;
    private int numeroTotalEventosEnSistema;
    
    private int offset;
    private List<EventVS> eventVSes;


    public void setOffset(int offset) {
            this.offset = offset;
    }

    public int getOffset() {
            return offset;
    }

    /**
     * @return the numeroEventosFirmaEnPeticion
     */
    public int getNumeroEventosFirmaEnPeticion() {
        return numeroEventosFirmaEnPeticion;
    }

    /**
     * @param numeroEventosFirmaEnPeticion the numeroEventosFirmaEnPeticion to set
     */
    public void setNumeroEventosFirmaEnPeticion(int numeroEventosFirmaEnPeticion) {
        this.numeroEventosFirmaEnPeticion = numeroEventosFirmaEnPeticion;
    }

    /**
     * @return the numeroTotalEventosFirmaEnSistema
     */
    public int getNumeroTotalEventosFirmaEnSistema() {
        return numeroTotalEventosFirmaEnSistema;
    }

    /**
     * @param numeroTotalEventosFirmaEnSistema the numeroTotalEventosFirmaEnSistema to set
     */
    public void setNumeroTotalEventosFirmaEnSistema(int numeroTotalEventosFirmaEnSistema) {
        this.numeroTotalEventosFirmaEnSistema = numeroTotalEventosFirmaEnSistema;
    }

    /**
     * @return the numeroEventosVotacionEnPeticion
     */
    public int getNumeroEventosVotacionEnPeticion() {
        return numeroEventosVotacionEnPeticion;
    }

    /**
     * @param numeroEventosVotacionEnPeticion the numeroEventosVotacionEnPeticion to set
     */
    public void setNumeroEventosVotacionEnPeticion(int numeroEventosVotacionEnPeticion) {
        this.numeroEventosVotacionEnPeticion = numeroEventosVotacionEnPeticion;
    }

    /**
     * @return the numeroTotalEventosVotacionEnSistema
     */
    public int getNumeroTotalEventosVotacionEnSistema() {
        return numeroTotalEventosVotacionEnSistema;
    }

    /**
     * @param numeroTotalEventosVotacionEnSistema the numeroTotalEventosVotacionEnSistema to set
     */
    public void setNumeroTotalEventosVotacionEnSistema(int numeroTotalEventosVotacionEnSistema) {
        this.numeroTotalEventosVotacionEnSistema = numeroTotalEventosVotacionEnSistema;
    }

    /**
     * @return the numeroEventosReclamacionEnPeticion
     */
    public int getNumeroEventosReclamacionEnPeticion() {
        return numeroEventosReclamacionEnPeticion;
    }

    /**
     * @param numeroEventosReclamacionEnPeticion the numeroEventosReclamacionEnPeticion to set
     */
    public void setNumeroEventosReclamacionEnPeticion(int numeroEventosReclamacionEnPeticion) {
        this.numeroEventosReclamacionEnPeticion = numeroEventosReclamacionEnPeticion;
    }

    /**
     * @return the numeroTotalEventosReclamacionEnSistema
     */
    public int getNumeroTotalEventosReclamacionEnSistema() {
        return numeroTotalEventosReclamacionEnSistema;
    }

    /**
     * @param numeroTotalEventosReclamacionEnSistema the numeroTotalEventosReclamacionEnSistema to set
     */
    public void setNumeroTotalEventosReclamacionEnSistema(int numeroTotalEventosReclamacionEnSistema) {
        this.numeroTotalEventosReclamacionEnSistema = numeroTotalEventosReclamacionEnSistema;
    }

    /**
     * @return the numeroEventosEnPeticion
     */
    public int getNumeroEventosEnPeticion() {
        return numeroEventosEnPeticion;
    }

    /**
     * @param numeroEventosEnPeticion the numeroEventosEnPeticion to set
     */
    public void setNumeroEventosEnPeticion(int numeroEventosEnPeticion) {
        this.numeroEventosEnPeticion = numeroEventosEnPeticion;
    }

    /**
     * @return the numeroTotalEventosEnSistema
     */
    public int getNumeroTotalEventosEnSistema() {
        return numeroTotalEventosEnSistema;
    }

    /**
     * @param numeroTotalEventosEnSistema the numeroTotalEventosEnSistema to set
     */
    public void setNumeroTotalEventosEnSistema(int numeroTotalEventosEnSistema) {
        this.numeroTotalEventosEnSistema = numeroTotalEventosEnSistema;
    }

	public List<EventVS> getEventVSBases() {
		return eventVSes;
	}

	public void setEventVSBases(List<EventVS> eventVSes) {
		this.eventVSes = eventVSes;
	}
    
	public static EventQueryResponse parse(String consultaStr) throws ParseException, JSONException {
    	Log.d(TAG + ".parse(...)", "parse(...)");
    	JSONObject jsonObject = new JSONObject (consultaStr);
        List<EventVS> eventVSes = new ArrayList<EventVS>();
        JSONObject jsonEventos = jsonObject.getJSONObject("eventVSBases");
        JSONArray arrayEventos;
        if (jsonEventos != null) {
        	if(jsonEventos.has("firmas")) {
                arrayEventos = jsonEventos.getJSONArray("firmas");
                if (arrayEventos != null) {
                    for (int i=0; i<arrayEventos.length(); i++) {
                        EventVSAndroid eventVSAndroid = EventVSAndroid.parse(arrayEventos.getJSONObject(i));
                        eventVSAndroid.setTypeVS(TypeVS.EVENTO_FIRMA);
                        eventVSes.add(eventVSAndroid);
                    }
                }	
        	}
        	if(jsonEventos.has("reclamaciones")) { 
                arrayEventos = jsonEventos.getJSONArray("reclamaciones");
                if (arrayEventos != null) {
                    for (int i=0; i<arrayEventos.length(); i++) {
                        EventVSAndroid eventVSAndroid = EventVSAndroid.parse(arrayEventos.getJSONObject(i));
                        eventVSAndroid.setTypeVS(TypeVS.EVENTO_RECLAMACION);
                        eventVSes.add(eventVSAndroid);
                    }
                }	
        	}
        	if(jsonEventos.has("votaciones")) {
                arrayEventos = jsonEventos.getJSONArray("votaciones");
                if (arrayEventos != null) {
                    for (int i=0; i<arrayEventos.length(); i++) {
                        EventVSAndroid eventVSAndroid = EventVSAndroid.parse(arrayEventos.getJSONObject(i));
                        eventVSAndroid.setTypeVS(TypeVS.EVENTO_VOTACION);
                        eventVSes.add(eventVSAndroid);
                    }
                }	
        	}
        }
        EventQueryResponse eventQueryResponse = new EventQueryResponse();
        if(jsonEventos.has("numeroEventosFirmaEnPeticion"))
        	eventQueryResponse.setNumeroEventosFirmaEnPeticion(jsonObject.getInt("numeroEventosFirmaEnPeticion"));
        if(jsonEventos.has("numeroTotalEventosFirmaEnSistema"))
        	eventQueryResponse.setNumeroTotalEventosFirmaEnSistema(jsonObject.getInt("numeroTotalEventosFirmaEnSistema"));
        if(jsonEventos.has("numeroEventosVotacionEnPeticion"))
        	eventQueryResponse.setNumeroEventosVotacionEnPeticion(jsonObject.getInt("numeroEventosVotacionEnPeticion"));
        if(jsonEventos.has("numeroTotalEventosVotacionEnSistema"))
        	eventQueryResponse.setNumeroTotalEventosVotacionEnSistema(jsonObject.getInt("numeroTotalEventosVotacionEnSistema"));
        if(jsonEventos.has("numeroEventosReclamacionEnPeticion"))
        	eventQueryResponse.setNumeroEventosReclamacionEnPeticion(jsonObject.getInt("numeroEventosReclamacionEnPeticion"));
        if(jsonEventos.has("numeroTotalEventosReclamacionEnSistema"))
        	eventQueryResponse.setNumeroTotalEventosReclamacionEnSistema(jsonObject.getInt("numeroTotalEventosReclamacionEnSistema"));
        if(jsonEventos.has("numeroEventosEnPeticion"))
        	eventQueryResponse.setNumeroEventosEnPeticion(jsonObject.getInt("numeroEventosEnPeticion"));
        if(jsonEventos.has("numeroTotalEventosEnSistema"))
        	eventQueryResponse.setNumeroTotalEventosEnSistema(jsonObject.getInt("numeroTotalEventosEnSistema"));
        if (jsonObject.has("offset"))
            eventQueryResponse.setOffset(jsonObject.getInt("offset"));
        eventQueryResponse.setEventVSBases(eventVSes);
        return eventQueryResponse;
    }
	
}
