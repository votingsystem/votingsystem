package org.sistemavotacion.herramientavalidacion;

import java.security.cert.X509Certificate;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InformacionVoto {
	
    private static Logger logger = LoggerFactory.getLogger(InformacionVoto.class);
    
	private String hashCertificadoVotoHEX;
	private String hashCertificadoVotoBase64;
	private String eventoId;
	private String controlAccesoURL;
	private String eventoURL;
	private String opcionSeleccionadaId;
	private X509Certificate certificadoVoto;
	private X509Certificate certificadoCentroControl;
	private X509Certificate certificadoControlAcceso;
	
	
	public String getHashCertificadoVotoHEX() {
		return hashCertificadoVotoHEX;
	}
	public void setHashCertificadoVotoHEX(String hashCertificadoVotoHEX) {
		this.hashCertificadoVotoHEX = hashCertificadoVotoHEX;
	}
	public String getHashCertificadoVotoBase64() {
		return hashCertificadoVotoBase64;
	}
	public void setHashCertificadoVotoBase64(String hashCertificadoVotoBase64) {
		this.hashCertificadoVotoBase64 = hashCertificadoVotoBase64;
	}
	public String getEventoId() {
		return eventoId;
	}
	public void setEventoId(String eventoId) {
		this.eventoId = eventoId;
	}
	public String getControlAccesoURL() {
		return controlAccesoURL;
	}
	public void setControlAccesoURL(String controlAccesoURL) {
		this.controlAccesoURL = controlAccesoURL;
	}
	public String getEventoURL() {
		return eventoURL;
	}
	public void setEventoURL(String eventoURL) {
		this.eventoURL = eventoURL;
	}
	public String getOpcionSeleccionadaId() {
		return opcionSeleccionadaId;
	}
	public void setOpcionSeleccionadaId(String opcionSeleccionadaId) {
		this.opcionSeleccionadaId = opcionSeleccionadaId;
	}

	public X509Certificate getCertificadoCentroControl() {
		return certificadoCentroControl;
	}
	public void setCertificadoCentroControl(X509Certificate certificadoCentroControl) {
		this.certificadoCentroControl = certificadoCentroControl;
	}
	public X509Certificate getCertificadoControlAcceso() {
		return certificadoControlAcceso;
	}
	public void setCertificadoControlAcceso(X509Certificate certificadoControlAcceso) {
		this.certificadoControlAcceso = certificadoControlAcceso;
	}
	public X509Certificate getCertificadoVoto() {
		return certificadoVoto;
	}
	public void setCertificadoVoto(X509Certificate certificadoVoto) {
		this.certificadoVoto = certificadoVoto;
    	String subjectDN = certificadoVoto.getSubjectDN().getName();
    	logger.debug("setCertificadoVoto - subjectDN: " +subjectDN);
    	if(subjectDN.split("OU=eventoId:").length > 1) {
    		setEventoId(subjectDN.split("OU=eventoId:")[1].split(",")[0]);
    	}
    	if(subjectDN.split("CN=controlAccesoURL:").length > 1) {
    		String parte = subjectDN.split("CN=controlAccesoURL:")[1];
    		logger.debug("Certificado - parte: " + parte);
    		if (parte.split(",").length > 1) {
    			controlAccesoURL = parte.split(",")[0];
    		} else controlAccesoURL = parte;
    	}
    	if (subjectDN.split("OU=hashCertificadoVotoHEX:").length > 1) {
    		hashCertificadoVotoHEX = subjectDN.split("OU=hashCertificadoVotoHEX:")[1].split(",")[0];
            HexBinaryAdapter hexConverter = new HexBinaryAdapter();     
			hashCertificadoVotoBase64 = new String(
					hexConverter.unmarshal(hashCertificadoVotoHEX));
    	}
	}
}
