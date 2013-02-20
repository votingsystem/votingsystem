package org.controlacceso.clientegwt.client.modelo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.controlacceso.clientegwt.client.util.DateUtils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public final class ActorConIPJso extends JavaScriptObject {
	
    public static enum Environment {DEVELOPMENT, PRODUCTION, TEST, 
    	APPLICATION, CUSTOM}
    
    public enum Estado {SUSPENDIDO, ACTIVO, INACTIVO}
	

	protected ActorConIPJso() {}
	
    public static native ActorConIPJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
    
    public static native ActorConIPJso create( ) /*-{
		return { };
	}-*/;
    
    private static final native JsArray<ActorConIPJso> asJsArray(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;

    public static List<ActorConIPJso> asList(String jsonStr) {
    	if(jsonStr == null) return null;
    	List<ActorConIPJso> respuesta = new ArrayList<ActorConIPJso>();
    	JsArray<ActorConIPJso> jsArray = asJsArray(jsonStr);
    	for(int i = 0; i <jsArray.length(); i++) {
    		respuesta.add(jsArray.get(i));
    	}
    	return respuesta;
    } 
	
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
	
	public final native double getId() /*-{
		return this.id;
	}-*/;

	public final native void setId(double value) /*-{
		this.id = value;
	}-*/;
	
	public final native String getServerURL() /*-{
		return this.serverURL;
	}-*/;
	
	public final native void setServerURL(String value) /*-{
		this.serverURL = value;
	}-*/;

	public final native String getNombre() /*-{
		return this.nombre;
	}-*/;

	public final native void setNombre(String value) /*-{
		this.nombre = value;
	}-*/;

	public final native String getEstado() /*-{
		return this.estado;
	}-*/;

	public final native void setEstado(String value) /*-{
		this.estado = value;
	}-*/;

	public final native String getUrlBlog() /*-{
		return this.urlBlog;
	}-*/;

	public final native void setUrlBlog(String value) /*-{
		this.urlBlog = value;
	}-*/;
	
	public final native String getFechaCreacionStr() /*-{
		return this.fechaCreacion;
	}-*/;

	public final native String getEstadisticasEventoURL() /*-{
		return this.estadisticasEventoURL;
	}-*/;
	
	public final native void setFechaCreacionStr(String value) /*-{
		this.fechaCreacion = value;
	}-*/;
	
	public final native String getCertificadoURL() /*-{
		return this.certificadoURL;
	}-*/;

	public final native void setCertificadoURL(String value) /*-{
		this.certificadoURL = value;
	}-*/;

	public Date getFechaCreacion() {
		return DateUtils.getDateFromString(getFechaCreacionStr());
	}

	public void setFechaCreacion(Date value) {	
		if(value == null) setFechaCreacionStr(null);
		else setFechaCreacionStr(DateUtils.getStringFromDate(value));
	}

	public final native String getTipo() /*-{
		return this.tipo;
	}-*/;
	
	public final native String setTipo(String value) /*-{
		this.tipo = value;
	}-*/;
	
	public Tipo getTipoEnumValue() {
		String tipo = getTipo();
		if(tipo == null) return null;
		return Tipo.valueOf(tipo);
	}

	public void setTipoEnumValue(Tipo tipo) {
		if(tipo == null) setTipo(null);
		else setTipo(tipo.toString());
	}
	
	public final native void setCentrosDeControlJsArray(
			JsArray<ActorConIPJso> centrosDeControl) /*-{
		this.centrosDeControl = centrosDeControl;
	}-*/;
	
	public final native JsArray<ActorConIPJso> getCentrosDeControlJsArray() /*-{
		return this.centrosDeControl;
	}-*/;
	
	public List<ActorConIPJso> getCentrosDeControlList() {
		JsArray<ActorConIPJso> centrosDeControlJso = getCentrosDeControlJsArray();
		if(centrosDeControlJso == null || centrosDeControlJso.length() == 0) return null;
		List<ActorConIPJso> centrosDeControl = new ArrayList<ActorConIPJso>();
		for(int i = 0; i < centrosDeControlJso.length(); i++) {
			centrosDeControl.add(centrosDeControlJso.get(i));
		}
		return centrosDeControl;
	}
	
	public void setCentrosDeControlList(List<ActorConIPJso> centrosDeControl) { 
		if(centrosDeControl == null || centrosDeControl.size() == 0) return;
    	JSONArray jsonArray = new JSONArray();
    	JsArray<ActorConIPJso> jsArray = (JsArray<ActorConIPJso>) JavaScriptObject.createArray();
    	int i = 0;
    	for(ActorConIPJso cctorConIPJso : centrosDeControl) {
    		jsArray.set(i++, cctorConIPJso);
		}
    	setCentrosDeControlJsArray(jsArray);	
	}
	
}