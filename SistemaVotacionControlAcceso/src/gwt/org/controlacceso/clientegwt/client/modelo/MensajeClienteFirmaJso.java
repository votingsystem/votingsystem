package org.controlacceso.clientegwt.client.modelo;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public final class MensajeClienteFirmaJso extends JavaScriptObject {
	
    public static enum Operacion {ASOCIAR_CENTRO_CONTROL_SMIME, 
        CAMBIO_ESTADO_CENTRO_CONTROL_SMIME, SOLICITUD_COPIA_SEGURIDAD, 
        PUBLICACION_MANIFIESTO_PDF, FIRMA_MANIFIESTO_PDF, PUBLICACION_RECLAMACION_SMIME,
        FIRMA_RECLAMACION_SMIME, PUBLICACION_VOTACION_SMIME, ENVIO_VOTO_SMIME,
        MENSAJE_MONITOR_DESCARGA_APPLET, MENSAJE_APPLET, MENSAJE_CIERRE_APPLET,
        GUARDAR_RECIBO_VOTO, ANULAR_VOTO, ANULAR_SOLICITUD_ACCESO, CANCELAR_EVENTO, 
        MENSAJE_HERRAMIENTA_VALIDACION, MENSAJE_CIERRE_HERRAMIENTA_VALIDACION,
        NEW_REPRESENTATIVE, SELECT_REPRESENTATIVE, REPRESENTATIVE_VOTING_HISTORY_REQUEST,
        REPRESENTATIVE_ACCREDITATIONS_REQUEST, REPRESENTATIVE_UNSUBSCRIBE_REQUEST}
	
    public static final int SC_OK = 200;
    public static final int SC_PING = 0;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ERROR_EJECUCION = 500;
    public static final int SC_ERROR_ENVIO_VOTO = 570;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;

	protected MensajeClienteFirmaJso() {}
		
	public static native MensajeClienteFirmaJso create(String mensaje, 
			 String operacion, int codigoEstado) /*-{
		return {codigoEstado: codigoEstado, operacion: operacion, mensaje: mensaje};
	}-*/;
	
	public static native MensajeClienteFirmaJso create() /*-{
		return {codigoEstado:700};
	}-*/;
	
    public static native MensajeClienteFirmaJso getMensajeClienteFirma(String jsonStr) /*-{
		return JSON.parse(jsonStr);
	}-*/;
	
    public String toJSONString() {
    	return new JSONObject(this).toString();
    }
    
	public Operacion getOperacionEnumValue() {
		if(getOperacion() == null) return null;
		return Operacion.valueOf(getOperacion());
	}
	
	public final native String getOperacion() /*-{
		return this.operacion;
	}-*/;

	public final native void setOperacion(String value) /*-{
    	this.operacion = value;
	}-*/;
	
	public final void setOperacionEnumValue(Operacion operacion) {
		setOperacion(operacion.toString());
	}

	public final native String getMensaje() /*-{
		return this.mensaje;
	}-*/;

	public final native void setMensaje(String value) /*-{
		this.mensaje = value;
	}-*/;
	
	public final native String getUrlTimeStampServer() /*-{
		return this.urlTimeStampServer;
	}-*/;
	
	public final native void setUrlTimeStampServer(String value) /*-{
		this.urlTimeStampServer = value;
	}-*/;
	
	public final native String getUrlServer() /*-{
		return this.urlServer;
	}-*/;

	public final native void setUrlServer(String value) /*-{
		this.urlServer = value;
	}-*/;
	
	public final native int getCodigoEstado() /*-{
    	return this.codigoEstado;
  	}-*/;

	public final native void setCodigoEstado(int value) /*-{
		this.codigoEstado = value;
	}-*/;	
	
	public final native JsArrayString getArgsJsArray() /*-{
		return this.args;
	}-*/;
	
	public final native void setArgsJsArray(JsArrayString value) /*-{
		this.args = value;
	}-*/;
	
	public void setArgs(String... args) {
		if(args == null || args.length == 0) return;
		JsArrayString jsArray = JavaScriptObject.createArray().cast();
		int i = 0;
		for(String arg : args) {
			jsArray.set(i++, arg);
		}
		setArgsJsArray(jsArray);
	}
	
	public final native String getUrlDocumento() /*-{
		return this.urlDocumento;
	}-*/;

	public final native void setUrlDocumento(String value) /*-{
		this.urlDocumento = value;
	}-*/;
	
	public final native String getUrlEnvioDocumento() /*-{
		return this.urlEnvioDocumento;
	}-*/;
	
	public final native void setUrlEnvioDocumento(String value) /*-{
		this.urlEnvioDocumento = value;
	}-*/;
	
	public final native void setEvento(EventoSistemaVotacionJso value) /*-{
		this.evento = value;
	}-*/;
	
	public final native EventoSistemaVotacionJso getEvento() /*-{
		return this.evento;
	}-*/;

	public final native JavaScriptObject getContenidoFirma() /*-{
		return this.contenidoFirma;
	}-*/;
	
	public final native void setContenidoFirma(JavaScriptObject value) /*-{
		this.contenidoFirma = value;
	}-*/;

	public final native String getNombreDestinatarioFirma() /*-{
		return this.nombreDestinatarioFirma;
	}-*/;
	
	public final native String setNombreDestinatarioFirma(String value) /*-{
		this.nombreDestinatarioFirma = value;
	}-*/;
	
	public final native String getEmailSolicitante() /*-{
		return this.emailSolicitante;
	}-*/;
	
	public final native String setEmailSolicitante(String value) /*-{
		this.emailSolicitante = value;
	}-*/;
	
	public final native String getAsuntoMensajeFirmado() /*-{
		return this.asuntoMensajeFirmado;
	}-*/;

	public final native String setAsuntoMensajeFirmado(String value) /*-{
		this.asuntoMensajeFirmado = value;
	}-*/;

	public final native String isRespuestaConRecibo() /*-{
		return this.respuestaConRecibo;
	}-*/;

	public final native String setRespuestaConRecibo(boolean value) /*-{
		this.respuestaConRecibo = value;
	}-*/;
	
	public final native String setSessionId(String value) /*-{
		this.sessionId = value;
	}-*/;
	
	public static native JavaScriptObject createMensajeCancelacionEvento(
			String eventURL, String estado) /*-{
		return {eventURL: eventURL, estado: estado};
	}-*/;

}