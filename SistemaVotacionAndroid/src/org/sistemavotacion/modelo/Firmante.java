package org.sistemavotacion.modelo;

import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.util.encoders.Base64;
import org.bouncycastle2.util.encoders.Hex;
import org.sistemavotacion.smime.CMSUtils;

/**
 *
 * @author jgzornoza
 */
public class Firmante {

    private Usuario usuario;
    private Date fechaFirma;
    private String subjectDN;
    private X509Certificate cert;
    private SignerInformation signer;
    private CertPath certPath;
    private String contenidoFirmado;

    public Firmante() { }    
    
    /**
     * @return the usuario
     */
    public Usuario getUsuario() {
        return usuario;
    }

    /**
     * @param usuario the usuario to set
     */
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * @return the fechaFirma
     */
    public Date getFechaFirma() {
        return fechaFirma;
    }

    /**
     * @param fechaFirma the fechaFirma to set
     */
    public void setFechaFirma(Date fechaFirma) {
        this.fechaFirma = fechaFirma;
    }

    /**
     * @return the subjectDN
     */
    public String getSubjectDN() {
        return subjectDN;
    }

    /**
     * @param subjectDN the subjectDN to set
     */
    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    /**
     * @return the cert
     */
    public X509Certificate getCert() {
        return cert;
    }

    /**
     * @param cert the cert to set
     */
    public void setCert(X509Certificate cert) {
        this.cert = cert;
    }

    public String getInfoCert() {
        return CMSUtils.obtenerInfoCertificado(cert);
    }
    
    /**
     * @return the certPath
     */
    public CertPath getCertPath() {
        return certPath;
    }

    /**
     * @param certPath the certPath to set
     */
    public void setCertPath(CertPath certPath) {
        this.certPath = certPath;
    }
    
        /**
     * @return the contentDigest
     */
    public String getContentDigestBase64() {
        if (signer.getContentDigest() == null) return null;
        return new String(Base64.encode(signer.getContentDigest()));
    }

    /**
     * @return the contentDigest
     */
    public String getContentDigestHex() {
        if (signer.getContentDigest() == null) return null;
        return new String(Hex.encode(signer.getContentDigest()));
    }

    /**
     * @return the contentDigest
     */
    public String getFirmaBase64() {
        if (signer.getSignature() == null) return null;
        return new String(Base64.encode(signer.getSignature()));
    }

        /**
     * @return the contentDigest
     */
    public String getFirmaHex() {
        if (signer.getSignature() == null) return null;
        return new String(signer.getSignature());
    }

    /**
     * @param signer the signer to set
     */
    public void setSigner(SignerInformation signer) {
        this.signer = signer;
    }
    
    public String getEncryptiontId() {
        return CMSUtils.getEncryptiontId(signer.getEncryptionAlgOID());
    }

    public String getDigestId() {
        return CMSUtils.getDigestId(signer.getDigestAlgOID());
    }

    /**
     * @return the contenidoFirmado
     */
    public String getContenidoFirmado() {
        return contenidoFirmado;
    }

    /**
     * @param contenidoFirmado the contenidoFirmado to set
     */
    public void setContenidoFirmado(String contenidoFirmado) {
        this.contenidoFirmado = contenidoFirmado;
    }
}
