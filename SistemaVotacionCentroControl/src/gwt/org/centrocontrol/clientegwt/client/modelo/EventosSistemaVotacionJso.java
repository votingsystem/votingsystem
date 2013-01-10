package org.centrocontrol.clientegwt.client.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public final class EventosSistemaVotacionJso extends JavaScriptObject {
	
    private static Logger logger = Logger.getLogger("EventosSistemaVotacionJso");
	
	protected EventosSistemaVotacionJso() {}
		
    public static native EventosSistemaVotacionJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
	
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
	
	public final native JsArray<EventoSistemaVotacionJso> getFirmasJsArray() /*-{
		return this.firmas;
	}-*/;
	
	public final native JsArray<EventoSistemaVotacionJso> getReclamacionesJsArray() /*-{
		return this.reclamaciones;
	}-*/;	
	
	public final native JsArray<EventoSistemaVotacionJso> getVotacionesJsArray() /*-{
		return this.votaciones;
	}-*/;	
	
	public List<EventoSistemaVotacionJso> getFirmasList() {
		JsArray<EventoSistemaVotacionJso> firmasJsArray = getFirmasJsArray();
		if(firmasJsArray == null || firmasJsArray.length() == 0) 
			return new ArrayList<EventoSistemaVotacionJso>();
		List<EventoSistemaVotacionJso> firmasList = new ArrayList<EventoSistemaVotacionJso>();
		for(int i = 0; i < firmasJsArray.length(); i++) {
			EventoSistemaVotacionJso manifiesto = firmasJsArray.get(i);
			manifiesto.setTipoEnumValue(Tipo.EVENTO_FIRMA);
			firmasList.add(manifiesto);
		}
		return firmasList;
	}
	
	public List<EventoSistemaVotacionJso> getVotacionesList() {
		JsArray<EventoSistemaVotacionJso> votacionesJsArray = getVotacionesJsArray();
		if(votacionesJsArray == null || votacionesJsArray.length() == 0) 
			return new ArrayList<EventoSistemaVotacionJso>();
		List<EventoSistemaVotacionJso> votacionesList = new ArrayList<EventoSistemaVotacionJso>();
		for(int i = 0; i < votacionesJsArray.length(); i++) {
			EventoSistemaVotacionJso votacion = votacionesJsArray.get(i);
			votacion.setTipoEnumValue(Tipo.EVENTO_VOTACION);
			votacionesList.add(votacion);
		}
		return votacionesList;
	}

	public List<EventoSistemaVotacionJso> getReclamacionesList() {
		JsArray<EventoSistemaVotacionJso> reclamacionesJsArray = getReclamacionesJsArray();
		if(reclamacionesJsArray == null || reclamacionesJsArray.length() == 0) 
			return new ArrayList<EventoSistemaVotacionJso>();
		List<EventoSistemaVotacionJso> reclamacionesList = new ArrayList<EventoSistemaVotacionJso>();
		for(int i = 0; i < reclamacionesJsArray.length(); i++) {
			EventoSistemaVotacionJso reclamacion = reclamacionesJsArray.get(i);
			reclamacion.setTipoEnumValue(Tipo.EVENTO_RECLAMACION);
			reclamacionesList.add(reclamacion);
		}
		return reclamacionesList;
	}
	
	public List<EventoSistemaVotacionJso> getEventosList() {
		List<EventoSistemaVotacionJso> eventos = new ArrayList<EventoSistemaVotacionJso>();
		eventos.addAll(getFirmasList());
		eventos.addAll(getVotacionesList());
		eventos.addAll(getReclamacionesList());
		return eventos;
	}
	
}