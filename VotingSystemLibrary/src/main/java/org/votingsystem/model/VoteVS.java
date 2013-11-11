package org.votingsystem.model;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.apache.log4j.Logger;

public class VoteVS {
	
    private static Logger logger = Logger.getLogger(VoteVS.class);

    private int statusCode;
    private boolean isValid = false;
    private SMIMEMessageWrapper receipt;
    private EventVSBase vote;
    private String hashCertificadoVotoHEX;
    private String hashCertificadoVotoBase64;
    private String eventoId;
    private String controlAccesoURL;
    private String eventoURL;
    private String representativeURL;
    private Long opcionSeleccionadaId;
    private X509Certificate certificadoVoto;
    private TimeStampToken voteTimeStampToken;
    private Set<X509Certificate> serverCerts = new HashSet<X509Certificate>();
    private CertificateVS certificate;

    public VoteVS () {}
	
    public VoteVS (int statusCode, SMIMEMessageWrapper voteReceipt, 
            Map voteReceiptDataMap, EventVSBase vote, OptionVS optionSelected) throws Exception { 
        this.receipt = voteReceipt;
        this.statusCode = statusCode;
        this.vote = vote;
        if(voteReceiptDataMap.get("opcionSeleccionadaId") != null &&
        		!"null".equals(voteReceiptDataMap.get("opcionSeleccionadaId"))) {
        	opcionSeleccionadaId = ((Integer)voteReceiptDataMap.get("opcionSeleccionadaId")).longValue();
        	if(opcionSeleccionadaId == optionSelected.getId()) {
        		if (voteReceipt.isValidSignature()) {
                    isValid = true;
                } else logger.error("ERROR -receipt with bad signature");
        	} else logger.error("ERROR - selected option id from receipt: " + opcionSeleccionadaId + 
        			" - Exepected id: " + optionSelected);
        } else logger.error("ERROR - vote receipt without option id");
        //voteReceiptDataMap.get("opcionSeleccionadaContenido")
        this.eventoURL = (String) voteReceiptDataMap.get("eventoURL");
    }
	
    public SMIMEMessageWrapper getReceipt() {
            return receipt;
    }
    public void setReceipt(SMIMEMessageWrapper receipt) {
            this.receipt = receipt;
    }
    public EventVSBase getVote() {
            return vote;
    }
    public void setVote(EventVSBase vote) {
            this.vote = vote;
    }

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
    public Long getOpcionSeleccionadaId() {
            return opcionSeleccionadaId;
    }
    public void setOpcionSeleccionadaId(Long opcionSeleccionadaId) {
            this.opcionSeleccionadaId = opcionSeleccionadaId;
    }

    public Set<X509Certificate> getServerCerts() {
            return serverCerts;
    }
    public void addServerCert(X509Certificate cert) {
            this.serverCerts.add(cert);
    }

    public X509Certificate getCertificadoVoto() {
            return certificadoVoto;
    }
    public void setCertificadoVoto(X509Certificate certificadoVoto) {
            this.certificadoVoto = certificadoVoto;
    String subjectDN = certificadoVoto.getSubjectDN().getName();
    //logger.debug("setCertificadoVoto - subjectDN: " +subjectDN);
    if(subjectDN.split("OU=eventoId:").length > 1) {
            setEventoId(subjectDN.split("OU=eventoId:")[1].split(",")[0]);
    }
    if(subjectDN.split("CN=controlAccesoURL:").length > 1) {
            String parte = subjectDN.split("CN=controlAccesoURL:")[1];
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
    if(subjectDN.split("OU=RepresentativeURL:").length > 1) {
            String parte = subjectDN.split("OU=RepresentativeURL:")[1];
            if (parte.split(",").length > 1) {
                    setRepresentativeURL(parte.split(",")[0]);
            } else setRepresentativeURL(parte);
    }
    }
    public TimeStampToken getVoteTimeStampToken() {
            return voteTimeStampToken;
    }
    public void setVoteTimeStampToken(TimeStampToken voteTimeStampToken) {
            this.voteTimeStampToken = voteTimeStampToken;
    }
    public CertificateVS getCertificado() {
            return certificate;
    }
    public void setCertificado(CertificateVS certificado) {
            this.certificate = certificate;
    }
    public String getRepresentativeURL() {
            return representativeURL;
    }
    public void setRepresentativeURL(String representativeURL) {
            this.representativeURL = representativeURL;
    }

    public int getStatusCode() {
            return statusCode;
    }

    public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
    }

    public boolean isValid() {
            return isValid;
    }

    public void setValid(boolean isValid) {
            this.isValid = isValid;
    }
}
