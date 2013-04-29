package org.controlacceso.clientegwt.client.modelo;

import java.text.ParseException;
import java.util.Date;

import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso.Estado;
import org.controlacceso.clientegwt.client.util.DateUtils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public final class UsuarioJso extends JavaScriptObject {

	 public enum Type {USER, REPRESENTATIVE}
	
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
	
	public final native int getId() /*-{
		return this.id;
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
	
	public final native int getRepresentationsNumber() /*-{
		if(this.representationsNumber == null)
		 	return 0
		else return this.representationsNumber;
	}-*/;
	
	public final native String getInfoURLf() /*-{
		return this.infoURL;
	}-*/;
	
	public final native String getInfo() /*-{
		return this.info;
	}-*/;
	
	public final native String getRepresentativeMessageURL() /*-{
		return this.representativeMessageURL;
	}-*/;
	
	public final native String getImageURL() /*-{
		return this.imageURL;
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
	
	public final native String getType() /*-{
		return this.type;
	}-*/;

	public final native void setType(String value) /*-{
		this.type = value;
	}-*/;
	
	public Type getTypeEnumValue() {
		String type = getType();
		if(type == null) return null;
		return Type.valueOf(type);
	}
	
	public void setTypeEnumValue(Type type) {
		if(type == null) setType(null);
		else setType(type.toString());
	}
}