package org.controlacceso.clientegwt.client.util;

import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import com.google.gwt.core.client.JavaScriptObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class ServerPaths {

    public static String getInfoServidorPath () {
    	return getApplicationPath() + "infoServidor/obtener";
    } 
	
    public static String getApplicationPath () {
    	String applicationPath = "http://" +
			com.google.gwt.user.client.Window.Location.getHostName();
    	String port = com.google.gwt.user.client.Window.Location.getPort();
    	if (port != null && !("".equals(port.trim()))) {
    		applicationPath = applicationPath + ":" + port;
    	}
        String totalPath = com.google.gwt.user.client.Window.Location.getPath();
        int indexOfSecondSlash = totalPath.indexOf("/app", 1);
        if (indexOfSecondSlash != -1) {
        	applicationPath = applicationPath + 
        		totalPath.substring(0, indexOfSecondSlash) + "/";
        } else {
        	indexOfSecondSlash = totalPath.indexOf("/index.html", 1);
        	if (indexOfSecondSlash != -1) {
            	applicationPath = applicationPath + 
            		totalPath.substring(0, indexOfSecondSlash) + "/";
        	}
        }
        if (!applicationPath.endsWith("/")) applicationPath = applicationPath + "/";
        return applicationPath;
    }

    public static String getUrlClienteAndroid () {
    	return getApplicationPath() + "app/clienteAndroid";
    }

    public static String getUrlAppAndroid () {
   	 	return getApplicationPath() + "android/SistemaVotacion.apk";
    }
    
    public static String getUrlEventos (int max, int offset) {
    	 return getApplicationPath() + "evento/obtener?max=" + max + "&offset=" + offset;
    }
    
	public static String getUrlManifiestos() {
		return getApplicationPath() + "eventoFirma/obtenerManifiestos";
	}
	
	public static String getUrlValidationToolJar () {
		return getApplicationPath() + "applet/HerramientaValidacion.jar";
	}
    
    public static String getUrlEvento (int id) {
    	return getApplicationPath() + "evento/obtener?id=" + id;
    }
    
    public static String getUrlAppVotacion () {
    	return getApplicationPath() + "app/home";
    }
    
    public static String getUrlPublicarPDF () {
   	 	return getApplicationPath() + "eventoFirma/publicarPDF";
    }
    
    public static String getUrlTimeStampServer () {
   	 	return getApplicationPath() + "timeStamp/obtener";
    }

    public static String getUrlSolicitudAcceso () {
    	return getApplicationPath() + "solicitudAcceso/procesar";
    }
    
    public static String getUrlVotoCentroControl (String serverUrl) {
        if (!serverUrl.endsWith("/")) serverUrl = serverUrl + "/";
    	return serverUrl + "voto/guardarAdjuntandoValidacion";
    }
    
    public static String getUrlFrameClienteFirma () {
    	return getApplicationPath() + "applet/cliente";
    }
    
    public static String getUrlFrameHerramientaValidacion () {
    	return getApplicationPath() + "applet/herramientaValidacion?gwt=true";
    }
    
    public static String getUrlSolicitudCopiaSeguridad () {
    	return getApplicationPath() + "solicitudCopia/validarSolicitud";
    }
    
    public static String getUrlSolicitudAccesoPorNif (String nif, int eventoId) {
    	return getApplicationPath() + "solicitudAcceso/encontrarPorNif?nif=" + nif +
    			"&eventoId=" + eventoId;
    }
    
    public static String getUrlManifiesto (String id) {
    	return getApplicationPath() + "eventoFirma/obtener?id=" + id;
    }
    
    public static String getUrlSubscripcionManifiestos () {
    	return getApplicationPath() + "subscripcion/manifiestos";
    }
    
    public static String getUrlSubscripcionReclamaciones () {
    	return getApplicationPath() + "subscripcion/reclamaciones";
    }
    
    public static String getUrlSubscripcionVotaciones () {
    	return getApplicationPath() + "subscripcion/votaciones";
    }
    
    public static String getUrlEventosVotacion (int max, int offset, 
    		EventoSistemaVotacionJso.Estado estado) {
    	String sufix = ""; 
    	if(estado != null) sufix = "&estadoEvento=" + estado.toString();
    	return getApplicationPath() + "eventoVotacion/obtener?max=" 
    		+ max + "&offset=" + offset + sufix;
    }
    
    
    public static String getUrlEventosReclamacion (int max, int offset, 
    		EventoSistemaVotacionJso.Estado estado) {
    	String sufix = ""; 
    	if(estado != null) sufix = "&estadoEvento=" + estado.toString();
   	 	return getApplicationPath() + "eventoReclamacion/obtener?max=" 
    			+ max + "&offset=" + offset  + sufix;
    }
    
    public static String getUrlEventosFirma (int max, int offset, 
    		EventoSistemaVotacionJso.Estado estado) {
    	String sufix = ""; 
    	if(estado != null) sufix = "&estadoEvento=" + estado.toString();
   	 	return getApplicationPath() + "eventoFirma/obtenerManifiestos?max=" 
    			+ max + "&offset=" + offset  + sufix;
    }
    
    public static String getUrlEstadisticasEventoFirma (int eventoId) {
   	 	return getApplicationPath() + "eventoFirma/estadisticas?id=" + eventoId;
    }
    
    public static String getUrlEstadisticasEventoReclamacion (int eventoId) {
   	 	return getApplicationPath() + "eventoReclamacion/estadisticas?id=" + eventoId;
    }
    
    public static String getUrlEstadisticasEventoReclamacion (String serverURL, int eventoId) {
   	 	return serverURL + "eventoReclamacion/estadisticas?id=" + eventoId;
    }
    
    public static String getUrlEstadisticasEventoVotacion (int eventoId) {
   	 	return getApplicationPath() + "eventoVotacion/estadisticas?id=" + eventoId;
    }
    
    public static String getUrlDatosAplicacion () {
   	 	return getApplicationPath() + "infoServidor/informacion";
    }
    
    public static String getUrlPDFManifiesto (int eventoId) {
   	 	return getApplicationPath() + "eventoFirma/obtenerPDF?id=" + eventoId;
    }
    
    public static String getUrlValidacionPDFPendientePublicacion (int eventoId) {
   	 	return getApplicationPath() + "eventoFirma/validarPDF?id=" + eventoId;
    }
    
    public static String getUrlRecolectorFirmaPDF (int eventoId) {
   	 	return getApplicationPath() + "recolectorFirma/validarPDF?id=" + eventoId;
    }
    
    public static String getUrlRecolectorReclamaciones () {
   	 	return getApplicationPath() + "recolectorReclamacion/guardarAdjuntandoValidacion";
    }
    
    public static String getUrlCancelarEvento () {
   	 	return getApplicationPath() + "evento/guardarCancelacion";
    }
    
    public static String getUrlAsociacionCentroControl () {
   	 	return getApplicationPath() + "subscripcion/guardarAsociacionConCentroControl";
    }

    public static String getUrlPublicacionVotacion () {
   	 	return getApplicationPath() + "eventoVotacion/guardarAdjuntandoValidacion";
    }
    
    public static String getUrlPublicacionReclamacion () {
   	 	return getApplicationPath() + "eventoReclamacion/guardarAdjuntandoValidacion";
    }
    
    public static String getUrlServerCert() {
    	return getApplicationPath() + "certificado/cadenaCertificacion";
    }
    
    public static String getUrlVoto(String serverUrl, String hashHex) {
        if (!serverUrl.endsWith("/")) serverUrl = serverUrl + "/";
    	return serverUrl + "voto/obtener?hashCertificadoVotoHex=" + hashHex;
    }
    
    public static String getURLAnulacionVoto() {
    	return getApplicationPath() + "anuladorVoto/guardarAdjuntandoValidacion";
    }
    
    public static native void redirect(String url)/*-{
          $wnd.location = url;
    }-*/;

    public static native void alert(String msg)/*-{
          $wnd.alert(msg);
    }-*/;
    
    public static native String openInNewTab(String url)/*-{
    	return $wnd.open(url, 'target=_blnk')
    }-*/;
    
    public static native JavaScriptObject openInNewWindow(String url)/*-{
		return $wnd.open(url,'mywindow','width=700,height=500')
	}-*/;
    
    public static native void setWindowTarget(JavaScriptObject window, String target)/*-{
    	window.location = target;
	}-*/;
    
    public static native void openClienteVotacion(String url)/*-{
		var clienteVotoWindow = window.open(url,'mywindow','width=500,height=500')
		clienteVotoWindow.moveTo(0, 0);
	}-*/;
    
    public static native void openClientePublicacion(String url)/*-{
		var clientePublicacionWindow = window.open(url,'mywindow','width=400,height=400')
		clientePublicacionWindow.moveTo(0, 0);
	}-*/;

    public static String getUrlBusquedas (int offset, int max) {
   	 	return getApplicationPath() + "buscador/consultaJSON?max=" + max + "&offset=" + offset;
    }
    
    public static String getUrlComprobacionFechasEvento(int eventoId) {
        return getApplicationPath() + "evento/comprobarFechas?id=" + eventoId;
    }
    

}