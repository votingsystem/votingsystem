package org.centrocontrol.clientegwt.client.modelo;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public final class InfoServidorJso extends JavaScriptObject {
	
	
	protected InfoServidorJso() {}
	
    public static native InfoServidorJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
    
    public static native InfoServidorJso create( ) /*-{
		return { };
	}-*/;
	
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
	
	public final native String getServerURL() /*-{
		return this.serverURL;
	}-*/;

	public final native String getNombre() /*-{
		return this.nombre;
	}-*/;

	public final native String getEnvironmentMode() /*-{
		return this.environmentMode;
	}-*/;

	public ActorConIPJso.Environment getEnvironmentModeEnumValue() {
		if(getEnvironmentMode() == null || "".equals(getEnvironmentMode())) return null;
		else return ActorConIPJso.Environment.valueOf(getEnvironmentMode());
	}

	public final native String getEstado() /*-{
		return this.estado;
	}-*/;

	public ActorConIPJso.Estado getEstadoEnumValue() {
		if(getEstado() == null || "".equals(getEstado())) return null;
		else return ActorConIPJso.Estado.valueOf(getEstado());
	}

	public final native JsArray<InfoServidorJso> getCentrosDeControlJsArray() /*-{
		return this.centrosDeControl;
	}-*/;
	
	public List<InfoServidorJso> getCentrosDeControlList() {
		JsArray<InfoServidorJso> centrosDeControlJso = getCentrosDeControlJsArray();
		if(centrosDeControlJso == null || centrosDeControlJso.length() == 0) return null;
		List<InfoServidorJso> centrosDeControl = new ArrayList<InfoServidorJso>();
		for(int i = 0; i < centrosDeControlJso.length(); i++) {
			centrosDeControl.add(centrosDeControlJso.get(i));
		}
		return centrosDeControl;
	}
	
	public final native String getUrlBlog() /*-{
		return this.urlBlog;
	}-*/;

	public final native void setUrlBlog(String value) /*-{
		this.urlBlog = value;
	}-*/;
	
	public final native String getCadenaCertificacionURL() /*-{
		return this.cadenaCertificacionURL;
	}-*/;


	public final native String getTipoServidor() /*-{
		return this.tipoServidor;
	}-*/;
	

	public Tipo getTipoServidorEnumValue() {
		String tipo = getTipoServidor();
		if(tipo == null) return null;
		return Tipo.valueOf(tipo);
	}
	
}