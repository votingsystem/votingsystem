package org.centrocontrol.clientegwt.client.modelo;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public final class ConsultaEventosSistemaVotacionJso extends JavaScriptObject {
	
	protected ConsultaEventosSistemaVotacionJso() {}
		
    public static native ConsultaEventosSistemaVotacionJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
	
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
    
    public final native EventosSistemaVotacionJso getEventos() /*-{
		return this.eventos;
	}-*/;

	public final native int getNumeroEventosFirmaEnPeticion() /*-{
		return this.numeroEventosFirmaEnPeticion;
	}-*/;

	public final native int getNumeroTotalEventosVotacionEnSistema() /*-{
		return this.numeroTotalEventosVotacionEnSistema;
	}-*/;
	
	public final native int getNumeroTotalEventosFirmaEnSistema() /*-{
		return this.numeroTotalEventosFirmaEnSistema;
	}-*/;
	
	public final native int getNumeroTotalEventosReclamacionEnSistema() /*-{
		return this.numeroTotalEventosReclamacionEnSistema;
	}-*/;
	
	public final native int getNumeroEventosReclamacionEnPeticion() /*-{
		return this.numeroEventosReclamacionEnPeticion;
	}-*/;
	
	public final native int getNumeroEventosEnPeticion() /*-{
		return this.numeroEventosEnPeticion;
	}-*/;
	
	public final native int getNumeroTotalEventosEnSistema() /*-{
		return this.numeroTotalEventosEnSistema;
	}-*/;
	
	public final native int getNumeroEventosVotacionEnPeticion() /*-{
		return this.numeroEventosVotacionEnPeticion;
	}-*/;
	
	public final native int getOffset() /*-{
		return this.offset;
	}-*/;
	

}