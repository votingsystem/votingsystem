package org.controlacceso.clientegwt.client.modelo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.controlacceso.clientegwt.client.util.DateUtils;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

public final class EventoSistemaVotacionJso extends JavaScriptObject {
	
    private static Logger logger = Logger.getLogger("EventoSistemaVotacionJso");
	
    public enum Estado {ACTIVO, FINALIZADO, CANCELADO, ACTORES_PENDIENTES_NOTIFICACION, PENDIENTE_COMIENZO,
    	PENDIENTE_DE_FIRMA, BORRADO_DE_SISTEMA}  
    
    public enum Cardinalidad { MULTIPLES, UNA}

    protected EventoSistemaVotacionJso() {}
    
    private static final native JsArray<EventoSistemaVotacionJso> asJsArray(String jsonStr) /*-{
    	return JSON.parse(jsonStr);
  	}-*/;
    
    public static List<EventoSistemaVotacionJso> asList(String jsonStr) {
    	if(jsonStr == null) return null;
    	List<EventoSistemaVotacionJso> respuesta = new ArrayList<EventoSistemaVotacionJso>();
    	JsArray<EventoSistemaVotacionJso> jsArray = asJsArray(jsonStr);
    	for(int i = 0; i <jsArray.length(); i++) {
    		respuesta.add(jsArray.get(i));
    	}
    	return respuesta;
    }    
    
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
    
    public static native EventoSistemaVotacionJso create(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
    
    public static native EventoSistemaVotacionJso create() /*-{
    	return {}
    }-*/;

	public final native int getId() /*-{
		return this.id;
	}-*/;

	public final native void setId(int value) /*-{
		this.id = value;
	}-*/;

 	public final native void setFechaCreacionStr(String value) /*-{
		this.fechaCreacion = value;
	}-*/;

 	public final native String getFechaCreacionStr() /*-{
		return this.fechaCreacion;
	}-*/;
 	
 	public final native boolean getCopiaSeguridadDisponible() /*-{
		return this.copiaSeguridadDisponible;
	}-*/;
 	
 	public final native void setCopiaSeguridadDisponible(boolean value) /*-{
		this.copiaSeguridadDisponible = value;
	}-*/;

	public Date getFechaCreacion() {
		String fechaCreacionStr = getFechaCreacionStr();
		if(fechaCreacionStr == null) return null;
		return DateUtils.getDateFromString(fechaCreacionStr);
	}

	public void setFechaCreacion(Date value) {	
		if(value == null) setFechaCreacionStr(null);
		else setFechaCreacionStr(DateUtils.getStringFromDate(value));
	}
	
	public void setTipoEnumValue(Tipo tipo) {
		if(tipo == null) setTipo(null);
		else setTipo(tipo.toString());
	}
    
	public Tipo getTipoEnumValue() {
		String tipo = getTipo();
		if(tipo == null) return null;
		return Tipo.valueOf(tipo);
	}
	
	public final native String getTipo() /*-{
		return this.tipo;
	}-*/;

	public final native String getHashSolicitudAccesoBase64() /*-{
		return this.hashSolicitudAccesoBase64;
	}-*/;
	
	public final native String getHashSolicitudAccesoHex() /*-{
		return this.hashSolicitudAccesoHex;
	}-*/;
	
	public final native String getHashCertificadoVotoBase64() /*-{
		return this.hashCertificadoVotoBase64;
	}-*/;
	
	public final native String getHashCertificadoVotoHex() /*-{
		return this.hashCertificadoVotoHex;
	}-*/;
	
	public final native void setTipo(String value) /*-{
		this.tipo = value;
	}-*/;
	
	public void setCardinalidadEnumValue(Cardinalidad cardinalidad) {
		if(cardinalidad == null) setCardinalidad(null);
		else setCardinalidad(cardinalidad.toString());
	}
    
	public Cardinalidad getCardinalidadEnumValue() {
		String cardinalidad = getCardinalidad();
		if(cardinalidad == null) return null;
		return Cardinalidad.valueOf(cardinalidad);
	}
	
	public final native String getCardinalidad() /*-{
		return this.cardinalidad;
	}-*/;

	public final native void setCardinalidad(String value) /*-{
		this.cardinalidad = value;
	}-*/;
	
	public final native String getUrlSolicitudAcceso() /*-{
		return this.urlSolicitudAcceso;
	}-*/;

	public final native void setUrlSolicitudAcceso(String value) /*-{
		this.urlSolicitudAcceso = value;
	}-*/;
	
	public final native String getUrlRecolectorVotosCentroControl() /*-{
		return this.urlRecolectorVotosCentroControl;
	}-*/;

	public final native void setUrlRecolectorVotosCentroControl(String value) /*-{
		this.urlRecolectorVotosCentroControl = value;
	}-*/;
	
	public final native void setControlAcceso(ActorConIPJso value) /*-{
		this.controlAcceso = value;
	}-*/;

	public final native ActorConIPJso getControlAcceso() /*-{
		return this.controlAcceso;
	}-*/;

	public final native void setCentroControl(ActorConIPJso value) /*-{
		this.centroControl = value;
	}-*/;

	public final native ActorConIPJso getCentroControl() /*-{
		return this.centroControl;
	}-*/;
	
	public final native JsArray<OpcionDeEventoJso> getOpcionDeEventoJsArray() /*-{
		return this.opciones;
	}-*/;
	
	public final native OpcionDeEventoJso getOpcionSeleccionada() /*-{
		return this.opcionSeleccionada;
	}-*/;
	
	
	public final native void setOpcionSeleccionada(OpcionDeEventoJso value) /*-{
		this.opcionSeleccionada = value;
	}-*/;
	
	public final native JsArray<CampoDeEventoJso> getCampoDeEventoJsArray() /*-{
		return this.campos;
	}-*/;
	
	public final native void setCampoDeEventoJsArray(JsArray<CampoDeEventoJso> value) /*-{
		this.campos = value;
	}-*/;

	public List<CampoDeEventoJso> getCampoDeEventoList() {
		JsArray<CampoDeEventoJso> camposJsArray = getCampoDeEventoJsArray();
		if(camposJsArray == null || camposJsArray.length() == 0) return null;
		List<CampoDeEventoJso> caposList = new ArrayList<CampoDeEventoJso>();
		for(int i = 0; i < camposJsArray.length(); i++) {
			caposList.add(camposJsArray.get(i));
		}
		return caposList;
	}

	public void setCampoDeEventoList(List<CampoDeEventoJso> camposList) { 
		if(camposList == null || camposList.size() == 0) return;
		JsArray<CampoDeEventoJso> jsArray = (JsArray<CampoDeEventoJso>) JavaScriptObject.createArray();
		int i = 0;
		for(CampoDeEventoJso campoJso : (List<CampoDeEventoJso>)camposList) {
			jsArray.set(i++, campoJso);
		}
		setCampoDeEventoJsArray(jsArray);	
	}	

	public final native void setDuracion(String value) /*-{
		this.duracion = value;
	}-*/;

	public final native String getDuracion() /*-{
		return this.duracion;
	}-*/;

	public Date getFechaFin() {
		String fechaFinStr = getFechaFinStr();
		if(fechaFinStr == null) return null;
		return DateUtils.getDateFromString(fechaFinStr);
	}

	public void setFechaFin(Date value) {	
		if(value == null) setFechaFinStr(null);
		else setFechaFinStr(DateUtils.getStringFromDate(value));
	}
    

	public final native void setFechaFinStr(String value) /*-{
		this.fechaFin = value;
	}-*/;
	
	public final native String getFechaFinStr() /*-{
		return this.fechaFin;
	}-*/;

    
	public final native String getFechaInicioStr() /*-{
		return this.fechaInicio;
	}-*/;
	
	public final native void setFechaInicioStr(String value) /*-{
		this.fechaInicio = value;
	}-*/;
	
	public final native String getUsuario() /*-{
		return this.usuario;
	}-*/;
    
	public final native UsuarioJso getVotante() /*-{
		return this.votante;
	}-*/;
	
   	public Date getFechaInicio() {
   		String fechaInicioStr = getFechaInicioStr();
   		if(fechaInicioStr == null) return null;
   		return DateUtils.getDateFromString(fechaInicioStr);
	}

	public void setFechaInicio(Date value) {
		if(value == null) setFechaInicioStr(null);
		else setFechaInicioStr(DateUtils.getStringFromDate(value));
	}
	public final native String getEstado() /*-{
		return this.estado;
	}-*/;

	public final native void setEstado(String value) /*-{
		this.estado = value;
	}-*/;

	public final native String getUrlPDF() /*-{
		return this.urlPDF;
	}-*/;

	public final native void setUrlPDF(String value) /*-{
		this.urlPDF = value;
	}-*/;

	public final native String getUrl() /*-{
		return this.url;
	}-*/;

	public final native void setUrl(String value) /*-{
		this.url = value;
	}-*/;


	public final native int getNumeroTotalFirmas() /*-{
		return this.numeroFirmas;
	}-*/;

	public final native void setNumerTotalFirmas(int value) /*-{
		this.numeroFirmas = value;
	}-*/;
	
	public Estado getEstadoEnumValue() {
		String estado = getEstado();
		if(estado == null) return null;
		return Estado.valueOf(estado);
	}
	
	public void setEstadoEnumValue(Estado estado) {
		if(estado == null) setEstado(null);
		else setEstado(estado.toString());
	}
	
	public final native String getContenido() /*-{
		return this.contenido;
	}-*/;
	
	public final native void setContenido(String value) /*-{
		this.contenido = value;
	}-*/;
	
	public final native void setAsunto(String asunto) /*-{
		this.asunto = asunto;
	}-*/;
	
	public final native String getAsunto() /*-{
		return this.asunto;
	}-*/;
	
	
	public final native JsArray getEtiquetas() /*-{
		return this.etiquetas;
	}-*/;
	
	
	public final native void setOpcionDeEventoJsArray(JsArray value) /*-{
		this.opciones = value;
	}-*/;
	
	
	public List<OpcionDeEventoJso> getOpcionDeEventoList() {
		JsArray<OpcionDeEventoJso> opcionesJsArray = getOpcionDeEventoJsArray();
		if(opcionesJsArray == null || opcionesJsArray.length() == 0) return null;
		List<OpcionDeEventoJso> opcionesList = new ArrayList<OpcionDeEventoJso>();
		for(int i = 0; i < opcionesJsArray.length(); i++) {
			opcionesList.add(opcionesJsArray.get(i));
		}
		return opcionesList;
	}
	
	public void setOpcionDeEventoList(List opcionesList) { 
		if(opcionesList == null || opcionesList.size() == 0) return;
		JsArray<OpcionDeEventoJso> jsArray = (JsArray<OpcionDeEventoJso>) JavaScriptObject.createArray();
		int i = 0;
		for(OpcionDeEventoJso opcionJso : (List<OpcionDeEventoJso>)opcionesList) {
			jsArray.set(i++, opcionJso);
		}
		setOpcionDeEventoJsArray(jsArray);	
	}	
	
	public String getMensaje () {
		String mensaje = null;
		switch (getEstadoEnumValue()) {
			case ACTIVO:
				mensaje = "Recibiendo solicitudes";
				break;
			case PENDIENTE_COMIENZO:
				mensaje = "Pendiente de abrir";
				break;
			case FINALIZADO:
				mensaje = "Finalizado";
				break;
			case CANCELADO:
				mensaje = "Suspendido";
				break;
			case ACTORES_PENDIENTES_NOTIFICACION:
				mensaje = "Falta notificación a participantes";
				break;
		}
		return mensaje;
	}
	
	public boolean isActive() {
		boolean result = false;
		switch (getEstadoEnumValue()) {
			case ACTIVO:
				result = checkDate();
				break;
			case PENDIENTE_COMIENZO:
				result = checkDate();
				break;
		}
		return result;
	}
	
	public boolean checkDate() {
		
		Date fechaActual = DateUtils.getTodayDate();
		if(fechaActual.after(getFechaInicio()) && fechaActual.before(getFechaFin()))
			return true;
		else return false;
	}
}