package org.centrocontrol.clientegwt.client.util;

import org.centrocontrol.clientegwt.client.modelo.EventoSistemaVotacionJso;

import com.google.gwt.core.client.JavaScriptObject;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class ServerPaths {

    public static String getInfoServidorPath () {
    	return getApplicationPath() + "infoServidor";
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
        }
        if (!applicationPath.endsWith("/")) applicationPath = applicationPath + "/";
        return applicationPath;
    }

    
    public static String getUrlEventoVotacion (int id) {
    	return getApplicationPath() + "eventoVotacion/" + id;
    }
    
    public static String getUrlEventosVotacion (int max, int offset, 
    		EventoSistemaVotacionJso.Estado estado) {
    	String sufix = ""; 
    	if(estado != null) sufix = "&estadoEvento=" + estado.toString();
    	return getApplicationPath() + "eventoVotacion?max=" 
    		+ max + "&offset=" + offset + sufix;
    }

    
    public static String getUrlBusquedas (int offset, int max) {
   	 	return getApplicationPath() + "buscador/consultaJSON?max=" + max + "&offset=" + offset;
    }
    
    public static String getUrlDatosAplicacion () {
   	 	return getApplicationPath() + "infoServidor";
    }
    
    //redirect the browser to the given url
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
    
    public static native void openClienteVotacion(String url)/*-{
		var clienteVotoWindow = window.open(url,'mywindow','width=400,height=400')
		clienteVotoWindow.moveTo(0, 0);
	}-*/;
    
    public static native void openClientePublicacion(String url)/*-{
		var clientePublicacionWindow = window.open(url,'mywindow','width=400,height=400')
		clientePublicacionWindow.moveTo(0, 0);
	}-*/;

    public static String getUrlSubscripcionVotaciones () {
    	return getApplicationPath() + "subscripcion/votaciones";
    }

    public static String getUrlFrameHerramientaValidacion () {
    	return getApplicationPath() + "applet/herramientaValidacion?gwt=true";
    }

    public static String getUrlFrameClienteFirma () {
    	return getApplicationPath() + "applet/cliente";
    }
    
    public static String getUrlTimeStampServer (String controlAccesoUrl) {
    	if (!controlAccesoUrl.endsWith("/")) controlAccesoUrl = controlAccesoUrl + "/";
    	return controlAccesoUrl + "timeStamp";
    }

    ///solicitudAcceso/evento/$eventoId/nif/$nif
    public static String getUrlSolicitudAccesoPorNif (
    		String controlAccesoUrl, String nif, int eventoId) {
    	if (!controlAccesoUrl.endsWith("/")) controlAccesoUrl = controlAccesoUrl + "/";
    	return controlAccesoUrl + "solicitudAcceso/evento/" + eventoId + "/nif/" + nif;
    }

    public static String getUrlEstadisticas (int eventoId) {
   	 	return getApplicationPath() + "eventoVotacion/" + eventoId + "/estadisticas";
    }
    
    public static String getUrlEstadisticasControlAcceso (String serverUrl, String eventoId) {
        if (!serverUrl.endsWith("/")) serverUrl = serverUrl + "/";
   	 	return serverUrl + "eventoVotacion/" + eventoId + "/estadisticas";
    }

    public static String getUrlSolicitudCopiaSeguridad () {
    	return getApplicationPath() + "solicitudCopia";
    }


    public static String getUrlSolicitudAcceso (String controlAccesoUrl) {
    	if (!controlAccesoUrl.endsWith("/")) controlAccesoUrl = controlAccesoUrl + "/";
    	return controlAccesoUrl + "solicitudAcceso";
    }

    public static String getUrlVotoCentroControl () {
    	return getApplicationPath() + "voto";
    }

    public static String getUrlCancelarEvento (String controlAccesoUrl) {
    	if (!controlAccesoUrl.endsWith("/")) controlAccesoUrl = controlAccesoUrl + "/";
   	 	return controlAccesoUrl + "evento/cancelled";
    }

    public static String getUrlVoto(String hashHex) {
        return getApplicationPath() + "voto/hashHex/" + hashHex;
    }

    public static String getURLAnulacionVoto(String controlAccesoUrl) {
    	if (!controlAccesoUrl.endsWith("/")) controlAccesoUrl = controlAccesoUrl + "/";
    	return getApplicationPath() + "anuladorVoto";
    }
    
    public static String getUrlComprobacionFechasEvento(int eventoId) {
        return getApplicationPath() + "eventoVotacion/" + eventoId + "/comprobarFechas" ;
    }
    
    public static String getUrlClienteAndroid () {
    	return getApplicationPath() + "app/clienteAndroid";
    }

    public static String getUrlAppAndroid () {
   	 	return getApplicationPath() + "android/SistemaVotacion.apk";
    }

    
}