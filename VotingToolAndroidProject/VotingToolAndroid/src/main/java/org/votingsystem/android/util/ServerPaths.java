package org.votingsystem.android.util;

import android.util.Log;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;

import java.text.MessageFormat;

public class ServerPaths {

    public static final String TAG = "ServerPaths";

	public static final String sufijoURLCertChain = "certificateVS/certChain";
	public static final String sufijoURLEventos = "eventVS";
	public static final String sufijoURLVotingEvents = "eventVSElection";
	
	public static final String sufixURLTimeStampService = "timeStamp";
	public static final String sufixURLPDFManifest = "eventVSManifest/";
	public static final String sufixURLPDFManifestCollector = "eventVSManifestCollector/";
	public static final String sufixURLSearch = "buscador/consultaJSON?max=";
	
	public static final String sufixURLCheckEvent = "evento/{0}/checkDates";
	
	public static final String sufijoURLManifestEvents = "eventVSManifest";
	public static final String sufijoURLClaimEvents = "eventVSClaim";
	public static final String sufixURLCertificationAddresses = "serverInfo/certificationCenters";
	
	public static final String sufijoInfoServidor = "serverInfo";
	public static final String sufijoURLEventoParaVotar = "eventVSElection";
	public static final String sufijoURLSolcitudAcceso = "accessRequestVS";
	public static final String sufijoURLVoto = "voto"; 
	public static final String sufijoURLSolicitudCSRUsuario = "csr/solicitar";
	public static final String sufijoURLSolicitudCertificadoUsuario = "csr?idSolicitudCSR=";
	public static final String sufijoEventoFirmado = "eventVSManifestCollector";
	public static final String sufijoReclamacion = "eventVSClaimCollector";
	
    public static String getURLCertChain (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLCertChain;
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
    
    public static String getURLCheckEvent(String serverURL, Long eventId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return MessageFormat.format(sufixURLCheckEvent, eventId);
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
    		EventState eventState, SubSystem subSystem, int max, int offset) {
        Log.d(TAG + ".getURLEventos() ", " - serverURL: " + serverURL);
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
        switch(eventState) {
	        case CLOSED:
	        	path = "TERMINATED";
	        	break;
	        case OPEN:
	        	path = "ACTIVE";
	        	break;
	        case PENDING:
	        	path = "AWAITING";
	        	break;
	    	default:
	    		path = "";
	    		break;
        }
        if(!"".equals(path)) {
        	path = "&eventVSState=" + path;
        }
    	return result + "?max="+ max + "&offset=" + offset  + path;
    }
    
    public static String getURLPublish (
    		String serverURL, TypeVS type) {
    	String param = null;
    	switch(type) {
	    	case CLAIM_PUBLISHING:
	    		param = "claim";
	    		break;
	    	case MANIFEST_PUBLISHING:
	    		param = "manifest";
	    		break;
	    	case VOTING_PUBLISHING:
	    		param = "vote";
	    		break;
    	}
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + "mobileEditor/" + param;
    }
    
    public static String getURLInfoServidor (String serverURL) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoInfoServidor;        
    }

    public static String getURLEventoParaVotar (String serverURL, String eventId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
        return serverURL + sufijoURLEventoParaVotar + "?id=" +eventId;
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
    
    public static String getAccessRequestURLPorNif (String serverURL , String nif, int eventId) {
        if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
    	return serverURL + "solicitudAcceso/encontrarPorNif?nif=" + nif +
    			"&eventId=" + eventId;
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

    public static String getURLStatistics(EventVS event) {
        String basePath = event.getAccessControlVS().getServerURL();
        if (!basePath.endsWith("/")) basePath = basePath + "/";
        switch(event.getTypeVS()) {
            case VOTING_EVENT:
                basePath = basePath + "eventVSElection/";
                break;
            case CLAIM_EVENT:
                basePath = basePath + "eventVSClaim/";
                break;
            case MANIFEST_EVENT:
                basePath = basePath + "eventVSManifest/";
                break;
        }
        return basePath + event.getId() + "/statistics";
    }
}