package org.centrocontrol.clientegwt.client.modelo;

import java.util.Date;
import org.centrocontrol.clientegwt.client.util.DateUtils;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public final class DatosBusquedaJso extends JavaScriptObject {
	
	
	protected DatosBusquedaJso() {}
		
	private static native DatosBusquedaJso createFromTipo(String value) /*-{
		return {tipo:value};
	}-*/;
	
	public static DatosBusquedaJso create(Tipo tipo) {
		return createFromTipo(tipo.toString());
	}
	
    public static native DatosBusquedaJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
	
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
	
	public final native String getTextQuery() /*-{
		return this.textQuery;
	}-*/;
	
	public final native void setTextQuery(String value) /*-{
		this.textQuery = value;
	}-*/;

	public final native String getFechaInicioDesdeStr() /*-{
		return this.fechaInicioDesde;
	}-*/;

	public final native void setFechaInicioDesdeStr(String value) /*-{
		this.fechaInicioDesde = value;
	}-*/;

	public final native String getFechaInicioHastaStr() /*-{
		return this.fechaInicioHasta;
	}-*/;

	public final native void setFechaInicioHastaStr(String value) /*-{
		this.fechaInicioHasta = value;
	}-*/;
	
	public Date getFechaInicioHasta() {
		String fechaFechaInicioHastaStr = getFechaInicioHastaStr();
		if(fechaFechaInicioHastaStr == null) return null;
		return DateUtils.getDateFromString(fechaFechaInicioHastaStr);
	}

	public void setFechaInicioHasta(Date value) {	
		if(value == null) setFechaInicioHastaStr(null);
		else setFechaInicioHastaStr(DateUtils.getStringFromDate(value));
	}
	
   	public Date getFechaInicioDesde() {
   		String fechaInicioDesdeStr = getFechaInicioDesdeStr();
   		if(fechaInicioDesdeStr == null) return null;
   		return DateUtils.getDateFromString(fechaInicioDesdeStr);
	}

	public void setFechaInicioDesde(Date value) {
		if(value == null) setFechaInicioDesdeStr(null);
		else setFechaInicioDesdeStr(DateUtils.getStringFromDate(value));
	}


	public final native String getFechaFinDesdeStr() /*-{
		return this.fechaFinDesde;
	}-*/;

	public final native void setFechaFinDesdeStr(String value) /*-{
		this.fechaFinDesde = value;
	}-*/;

	public final native String getFechaFinHastaStr() /*-{
		return this.fechaFinHasta;
	}-*/;

	public final native void setFechaFinHastaStr(String value) /*-{
		this.fechaFinHasta = value;
	}-*/;
	
	public Date getFechaFinHasta() {
		String fechaFechaFinHastaStr = getFechaFinHastaStr();
		if(fechaFechaFinHastaStr == null) return null;
		return DateUtils.getDateFromString(fechaFechaFinHastaStr);
	}

	public void setFechaFinHasta(Date value) {	
		if(value == null) setFechaFinHastaStr(null);
		else setFechaFinHastaStr(DateUtils.getStringFromDate(value));
	}
	
   	public Date getFechaFinDesde() {
   		String fechaFinDesdeStr = getFechaFinDesdeStr();
   		if(fechaFinDesdeStr == null) return null;
   		return DateUtils.getDateFromString(fechaFinDesdeStr);
	}

	public void setFechaFinDesde(Date value) {
		if(value == null) setFechaFinDesdeStr(null);
		else setFechaFinDesdeStr(DateUtils.getStringFromDate(value));
	}

	public final native String getEstadoEvento() /*-{
		return this.estadoEvento;
	}-*/;
	
	public final EventoSistemaVotacionJso.Estado getEstadoEventoEnumValue() {
		String estadoEvento = getEstadoEvento();
		if(estadoEvento == null) return null;
		return EventoSistemaVotacionJso.Estado.valueOf(estadoEvento);
	}
	
	public void setEstadoEventoEnumValue(EventoSistemaVotacionJso.Estado estado) {
		if(estado == null) setEstadoEvento(null);
		else setEstadoEvento(estado.toString());
	}
	
	public final native void setEstadoEvento(String value) /*-{
		this.estadoEvento = value;
	}-*/;

}