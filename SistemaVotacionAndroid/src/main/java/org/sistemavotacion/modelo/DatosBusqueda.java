package org.sistemavotacion.modelo;

import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class DatosBusqueda {
	
	public static final String TAG = "DatosBusqueda";
	
	private Tipo tipo;
	private Evento.Estado estadoEvento;
	private String textQuery;
	
	public DatosBusqueda(Tipo tipo, Evento.Estado estadoEvento, 
			String textQuery) {
		this.tipo = tipo;
		this.estadoEvento = estadoEvento;
		this.textQuery = textQuery;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

	public Evento.Estado getEstadoEvento() {
		return estadoEvento;
	}

	public void setEstadoEvento(Evento.Estado estadoEvento) {
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
		if(tipo != null)
			map.put("tipo", tipo.toString());
		if(estadoEvento != null)
			map.put("estadoEvento", estadoEvento.toString());
		if(textQuery != null)
			map.put("textQuery", textQuery);
	    return new JSONObject(map);
	}
	
}
