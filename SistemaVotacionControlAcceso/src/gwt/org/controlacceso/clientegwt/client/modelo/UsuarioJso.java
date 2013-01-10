package org.controlacceso.clientegwt.client.modelo;

import java.text.ParseException;
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
* Licencia: http://bit.ly/j9jZQH
*/
public final class UsuarioJso extends JavaScriptObject {

	protected UsuarioJso() {}

    public static native UsuarioJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
    
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
	
	public final native String getNif() /*-{
		return this.nif;
	}-*/;
	
	public final native String getNombre() /*-{
		return this.nombre;
	}-*/;
	
	public final native String getPrimerApellido() /*-{
		return this.primerApellido;
	}-*/;
	
	public final native String getPais() /*-{
		return this.pais;
	}-*/;
	
	public final native String getCn() /*-{
		return this.cn;
	}-*/;

	public final native String getUrlCertificado() /*-{
		return this.urlCertificado;
	}-*/;
	
	public Date getDateCreated() throws ParseException {
		String dateCreatedStr = getDateCreatedStr();
		if(dateCreatedStr == null) return null;
		return DateUtils.getDateFromString(dateCreatedStr);
	}
	
	public final native String getDateCreatedStr() /*-{
		return this.dateCreated;
	}-*/;
	
	public Date getLastUpdated() throws ParseException {
		String lastUpdatedStr = getLastUpdatedStr();
		if(lastUpdatedStr == null) return null;
		return DateUtils.getDateFromString(lastUpdatedStr);
	}
	
	public final native String getLastUpdatedStr() /*-{
		return this.lastUpdated;
	}-*/;
}