package org.controlacceso.clientegwt.client.modelo;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public final class RepresentativesMapJso extends JavaScriptObject {
	
	protected RepresentativesMapJso() {}
		
    public static native RepresentativesMapJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
	
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }

	public final native int getNumberRepresentativesInRequest() /*-{
		return this.numberRepresentativesInRequest;
	}-*/;
	
	public final native int getRepresentativesTotalNumber() /*-{
		return this.representativesTotalNumber;
	}-*/;
	
	public final native int getOffset() /*-{
		return this.offset;
	}-*/;
	
	public final native JsArray<UsuarioJso> getRepresentativesJsArray() /*-{
		return this.representatives;
	}-*/;

	public List<UsuarioJso> getRepresentativesList() {
		JsArray<UsuarioJso> representativesJsArray = getRepresentativesJsArray();
		if(representativesJsArray == null || representativesJsArray.length() == 0) 
			return new ArrayList<UsuarioJso>();
		List<UsuarioJso> representativesList = new ArrayList<UsuarioJso>();
		for(int i = 0; i < representativesJsArray.length(); i++) {
			UsuarioJso usuario = representativesJsArray.get(i);
			usuario.setTypeEnumValue(UsuarioJso.Type.REPRESENTATIVE);
			representativesList.add(usuario);
		}
		return representativesList;
	}
}