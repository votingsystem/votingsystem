package org.sistemavotacion.util;

public class ServerPaths {

	public static final String sufijoURLCadenaCertificacion = "certificado/cadenaCertificacion";
	public static final String sufijoURLEventos = "evento/obtener";
	public static final String sufijoURLVotingEvents = "eventoVotacion/obtener";
	
	public static final String sufixURLTimeStampService = "timeStamp/obtener";
	public static final String sufixURLPDFManifest = "eventoFirma/obtenerPDF?id=";
	public static final String sufixURLPDFManifestCollector = "recolectorFirma/validarPDF?id=";
	public static final String sufixURLSearch = "buscador/consultaJSON?max=";
	
	public static final String sufixURLCheckEvent = "evento/comprobarFechas?id=";
	
	public static final String sufijoURLManifestEvents = "eventoFirma/obtenerManifiestos";
	public static final String sufijoURLClaimEvents = "eventoReclamacion/obtener";
	public static final String sufixURLCertificationAddresses = "infoServidor/centrosCertificacion";
	
	public static final String sufijoInfoServidor = "infoServidor/obtener";
	public static final String sufijoURLEventoParaVotar = "eventoVotacion/obtener"; 
	public static final String sufijoURLSolcitudAcceso = "solicitudAcceso/procesar"; 
	public static final String sufijoURLVoto = "voto/guardarAdjuntandoValidacion"; 
	public static final String sufijoURLSolicitudCSRUsuario = "csr/solicitar";
	public static final String sufijoURLSolicitudCertificadoUsuario = "csr/obtener?idSolicitudCSR=";
	public static final String sufijoEventoFirmado = "recolectorFirma/guardarAdjuntandoValidacion";
	public static final String sufijoReclamacion = "recolectorReclamacion/guardarAdjuntandoValidacion";
	
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
        return serverURL + sufixURLCheckEvent + eventoId; 
    }
    
    public static String getURLPDFManifestCollector (String serverURL, Long manifestId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufixURLPDFManifestCollector + manifestId;        
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
    
    
    public static String getURLInfoServidor (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoInfoServidor;        
    }

    public static String getURLEventoParaVotar (String serverURL, String eventoId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLEventoParaVotar + "?id=" +eventoId;             
    }
    
    public static String getURLEventoFirmado (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoEventoFirmado;             
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
    	return serverURL + "solicitudAcceso/encontrarPorNif?nif=" + nif +
    			"&eventoId=" + eventoId;
    }
    
    public static String getURLSolicitudCertificadoUsuario (
    		String serverURL, String idSolicitudCSR) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLSolicitudCertificadoUsuario + idSolicitudCSR;             
    }
}