package org.sistemavotacion.util;

import org.sistemavotacion.android.WebActivity;

public class ServerPaths {

	public static final String sufijoURLCadenaCertificacion = "certificado/cadenaCertificacion";
	public static final String sufijoURLEventos = "evento";
	public static final String sufijoURLVotingEvents = "eventoVotacion";
	
	public static final String sufixURLTimeStampService = "timeStamp";
	public static final String sufixURLPDFManifest = "eventoFirma/";
	public static final String sufixURLPDFManifestCollector = "recolectorFirma/";

	public static final String sufixURLSearch = "buscador/consultaJSON?max=";
	
	public static final String sufijoURLManifestEvents = "eventoFirma";
	public static final String sufijoURLClaimEvents = "eventoReclamacion";
	public static final String sufixURLCertificationAddresses = "infoServidor/centrosCertificacion";
	
	public static final String sufijoInfoServidor = "infoServidor";
	public static final String sufijoURLEventoParaVotar = "eventoVotacion"; 
	public static final String sufijoURLSolcitudAcceso = "solicitudAcceso"; 
	public static final String sufijoURLVoto = "voto"; 
	public static final String sufijoURLSolicitudCSRUsuario = "csr/solicitar";
	public static final String sufijoURLSolicitudCertificadoUsuario = "csr/";

	public static final String sufijoReclamacion = "recolectorReclamacion";
	
    public static String getURLCadenaCertificacion (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLCadenaCertificacion;        
    }

    public static String getURLPDFManifest (String serverURL, Long manifestId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufixURLPDFManifest + manifestId;        
    }
    
    public static String getURLCertificationAddresses (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufixURLCertificationAddresses;        
    }
    
    public static String getURLTimeStampService (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufixURLTimeStampService;        
    }
    
    public static String getURLSearch (String serverURL, int offset, int max) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufixURLSearch + max + "&offset=" + offset;    
    }
    
    public static String getURLCheckEvent(String serverURL, Long eventoId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "evento/" + eventoId + "/comprobarFechas"; 
    }
    
    public static String getURLPDFManifestCollector (String serverURL, Long manifestId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufixURLPDFManifestCollector + manifestId;        
    }
    
    public static String getURLAnulacionVoto(String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
    	return serverURL + "anuladorVoto";
    }
    
    public static String getURLEventos (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLEventos;        
    }
    
    public static String getURLEventos (String serverURL, 
    		EnumTab enumTab, SubSystem subSystem, int max, int offset) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        String result = serverURL;
        String path = null;
        switch (subSystem) {
	        case CLAIMS:
	        	path = sufijoURLClaimEvents;
	        	break;
	        case MANIFESTS:
	        	path = sufijoURLManifestEvents;
	        	break;
	        case VOTING:
	        	path = sufijoURLVotingEvents;
	        	break;
	    	default:
	    		path = sufijoURLEventos;
	    		break;
        }
        result = result + path;
        switch(enumTab) {
	        case CLOSED:
	        	path = "FINALIZADO";
	        	break;
	        case OPEN:
	        	path = "ACTIVO";
	        	break;
	        case PENDING:
	        	path = "PENDIENTE_COMIENZO";
	        	break;
	    	default:
	    		path = "";
	    		break;
        }
        if(!"".equals(path)) {
        	path = "&estadoEvento=" + path;
        }
    	return result + "?max="+ max + "&offset=" + offset  + path;
    }
    
    public static String getURLPublish (
    		String serverURL, WebActivity.Screen screen) {
    	String param = null;
    	switch(screen) {
	    	case PUBLISH_CLAIM:
	    		param = "claim";
	    		break;
	    	case PUBLISH_MANIFEST:
	    		param = "manifest";
	    		break;
	    	case PUBLISH_VOTING:
	    		param = "vote";
	    		break;
    	}
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "app/editor?androidClientLoaded=true&editor=" + param;        
    }
    
    public static String getURLInfoServidor (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoInfoServidor;        
    }

    public static String getURLEventoParaVotar (String serverURL, String eventoId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLEventoParaVotar + "/" +eventoId;             
    }
    
    public static String getURLReclamacion (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoReclamacion;             
    }
    
    public static String getURLSolicitudAcceso (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLSolcitudAcceso;             
    }
    
    public static String getURLVoto (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLVoto;             
    }
    
    public static String getURLSolicitudCSRUsuario (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLSolicitudCSRUsuario;             
    }
    
    public static String getUrlSolicitudAccesoPorNif (
    		String serverURL , String nif, int eventoId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
    	return serverURL + "solicitudAcceso/evento/" 
        		+ eventoId  + "/nif/" +  nif;
    }
    
    public static String getUrlAndroidBrowserSession (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
    	return serverURL + "app/index?androidClientLoaded=true";
    }
    
    public static String getURLSolicitudCertificadoUsuario (
    		String serverURL, String idSolicitudCSR) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLSolicitudCertificadoUsuario + idSolicitudCSR;             
    }
}