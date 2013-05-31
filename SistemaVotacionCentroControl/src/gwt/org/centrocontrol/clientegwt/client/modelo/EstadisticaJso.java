package org.centrocontrol.clientegwt.client.modelo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.centrocontrol.clientegwt.client.util.DateUtils;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public final class EstadisticaJso extends JavaScriptObject {
	
	protected EstadisticaJso() {}
		
    public static native EstadisticaJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
    
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
	
	public final native int getId() /*-{
		return this.id;
	}-*/;

	public final native String getEstado() /*-{
		return this.estado;
	}-*/;
	
	public EventoSistemaVotacionJso.Estado getEstadoEnunValue() {
		String estado = getEstado();
		if(estado == null) return null;
		return EventoSistemaVotacionJso.Estado.valueOf(estado);
	}

	public final native String getUsuario() /*-{
		return this.usuario;
	}-*/;

	public final native int getNumeroFirmas() /*-{
		return this.numeroFirmas;
	}-*/;
	
	public final native int getNumeroCertificadosEmitidos() /*-{
		return this.numeroCertificadosEmitidos;
	}-*/;
	
	
	public final native int getNumeroSolicitudesDeAcceso() /*-{
		return this.numeroSolicitudesDeAcceso;
	}-*/;
	
	public final native int getNumeroSolicitudesDeAccesoOK() /*-{
		return this.numeroSolicitudesDeAccesoOK;
	}-*/;
	
	public final native int getNumeroSolicitudesDeAccesoANULADAS() /*-{
		return this.numeroSolicitudesDeAccesoANULADAS;
	}-*/;
	
	public final native int getNumeroVotos() /*-{
		return this.numeroVotos;
	}-*/;
	
	public final native int getNumeroVotosOK() /*-{
		return this.numeroVotosOK;
	}-*/;
	
	public final native int getNumeroVotosANULADOS() /*-{
		return this.numeroVotosANULADOS;
	}-*/;
	
	
	
	public final native int getNumeroCertificadosRecibidos() /*-{
		return this.numeroCertificadosRecibidos;
	}-*/;

	
	public final native String getUrlEvento() /*-{
		return this.urlEvento;
	}-*/;
	
	public final native String getSolicitudPublicacionURL() /*-{
		return this.solicitudPublicacionURL;
	}-*/;
	
	public Date getFechaFin() {
		String fechaFinStr = getFechaFinStr();
		if(fechaFinStr == null) return null;
		return DateUtils.getDateFromString(fechaFinStr);
	}
	
	public final native String getFechaFinStr() /*-{
		return this.fechaFin;
	}-*/;
	
	public Date getFechaInicio() {
		String fechaInicioStr = getFechaInicioStr();
		if(fechaInicioStr == null) return null;
		return DateUtils.getDateFromString(fechaInicioStr);
	}
	
	public final native String getFechaInicioStr() /*-{
		return this.fechaInicio;
	}-*/;
	
	public final native String getUrl() /*-{
		return this.URL;
	}-*/;
	
	public final native JsArray<OpcionDeEventoJso> getOpcionesJsArray() /*-{
		return this.opciones;
	}-*/;
	
	public List<OpcionDeEventoJso> getOpcionesList() {
		JsArray<OpcionDeEventoJso> opcionesJsArray = getOpcionesJsArray();
		List<OpcionDeEventoJso> opcionesList = new ArrayList<OpcionDeEventoJso>();
		if(opcionesJsArray == null || opcionesJsArray.length() == 0) return opcionesList;
		for(int i = 0; i < opcionesJsArray.length(); i++) {
			opcionesList.add(opcionesJsArray.get(i));
		}
		return opcionesList;
	}
	
	public Tipo getTipoEnumValue() {
		String tipo = getTipo();
		if(tipo == null) return null;
		return Tipo.valueOf(tipo);
	}
	
	public final native String getTipo() /*-{
		return this.tipo;
	}-*/;
	
	public final native String getInformacionFirmasURL() /*-{
		return this.informacionFirmasURL;
	}-*/;
	
	public final native String getSolicitudPublicacionValidadaURL() /*-{
		return this.solicitudPublicacionValidadaURL;
	}-*/;
}