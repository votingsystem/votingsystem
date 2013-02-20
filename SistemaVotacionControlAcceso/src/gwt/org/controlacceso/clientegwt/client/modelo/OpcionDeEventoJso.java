package org.controlacceso.clientegwt.client.modelo;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public final class OpcionDeEventoJso extends JavaScriptObject {
	
	protected OpcionDeEventoJso() {}
		
	public static native OpcionDeEventoJso create(String contenido, int id, 
			int numeroVotos, EventoSistemaVotacionJso eventoSistemaVotacion) /*-{
		return {id: id, contenido: contenido, numeroVotos: numeroVotos, 
			eventoSistemaVotacion:eventoSistemaVotacion};
	}-*/;
	
    public static native OpcionDeEventoJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
    
    public static native OpcionDeEventoJso create() /*-{
		return {};
	}-*/;
    
    private static final native JsArray<OpcionDeEventoJso> asJsArray(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;

    public static List<OpcionDeEventoJso> asList(String jsonStr) {
    	if(jsonStr == null) return null;
    	List<OpcionDeEventoJso> respuesta = new ArrayList<OpcionDeEventoJso>();
    	JsArray<OpcionDeEventoJso> jsArray = asJsArray(jsonStr);
    	for(int i = 0; i <jsArray.length(); i++) {
    		respuesta.add(jsArray.get(i));
    	}
    	return respuesta;
    } 
	
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
	
	public final native int getId() /*-{
		return this.id;
	}-*/;

	public final native void setId(int value) /*-{
		this.id = value;
	}-*/;
	
	public final native String getContenido() /*-{
		return this.contenido;
	}-*/;

	public final native void setContenido(String value) /*-{
		this.contenido = value;
	}-*/;
	
	public final native int getNumeroVotos() /*-{
		return this.numeroVotos;
	}-*/;

	public final native void setNumeroVotos(int value) /*-{
		this.numeroVotos = value;
	}-*/;
	
	public final native EventoSistemaVotacionJso getEventoSistemaVotacion() /*-{
		return this.eventoSistemaVotacion;
	}-*/;

	public final native void setEventoSistemaVotacion(EventoSistemaVotacionJso value) /*-{
		this.eventoSistemaVotacion = value;
	}-*/;

}