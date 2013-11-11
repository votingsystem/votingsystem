package org.votingsystem.android.model;

import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.model.TypeVS;

import java.util.HashMap;
import java.util.Map;


public class DatosBusqueda {
	
	public static final String TAG = "DatosBusqueda";
	
	private TypeVS typeVS;
	private EventVSAndroid.Estado estadoEvento;
	private String textQuery;
	
	public DatosBusqueda(TypeVS typeVS, EventVSAndroid.Estado estadoEvento,
			String textQuery) {
		this.typeVS = typeVS;
		this.estadoEvento = estadoEvento;
		this.textQuery = textQuery;
	}

	public TypeVS getTypeVS() {
		return typeVS;
	}

	public void setTypeVS(TypeVS typeVS) {
		this.typeVS = typeVS;
	}

	public EventVSAndroid.Estado getEstadoEvento() {
		return estadoEvento;
	}

	public void setEstadoEvento(EventVSAndroid.Estado estadoEvento) {
		this.estadoEvento = estadoEvento;
	}

	public String getTextQuery() {
		return textQuery;
	}

	public void setTextQuery(String textQuery) {
		this.textQuery = textQuery;
	}
	
	public JSONObject toJSON() {
		Log.d(TAG + ".toJSON(...)", " - toJSON");
		Map<String, Object> map = new HashMap<String, Object>();
		if(typeVS != null)
			map.put("typeVS", typeVS.toString());
		if(estadoEvento != null)
			map.put("estadoEvento", estadoEvento.toString());
		if(textQuery != null)
			map.put("textQuery", textQuery);
	    return new JSONObject(map);
	}
	
}
