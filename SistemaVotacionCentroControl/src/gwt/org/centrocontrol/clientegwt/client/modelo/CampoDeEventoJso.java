package org.centrocontrol.clientegwt.client.modelo;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public final class CampoDeEventoJso extends JavaScriptObject {
	
	protected CampoDeEventoJso() {}
		
	public static native CampoDeEventoJso create(String contenido, int id, 
			String valor) /*-{
		return {id: id, contenido: contenido, valor: valor};
	}-*/;
	
    public static native CampoDeEventoJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
    
    public static native CampoDeEventoJso create() /*-{
		return {};
	}-*/;
    
    private static final native JsArray asJsArray(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;

    public static List asList(String jsonStr) {
    	if(jsonStr == null) return null;
    	List<CampoDeEventoJso> respuesta = new ArrayList<CampoDeEventoJso>();
    	JsArray<CampoDeEventoJso> jsArray = asJsArray(jsonStr);
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
	
	public final native String getValor() /*-{
		return this.valor;
	}-*/;

	public final native void setValor(String value) /*-{
		this.valor = value;
	}-*/;

}