package org.sistemavotacion.modelo;

import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.tsp.TimeStampToken;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.sistemavotacion.smime.CMSUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
        
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Usuario {
    
    private static Logger logger = LoggerFactory.getLogger(Usuario.class);

    private String nif;
    private String primerApellido;
    private String pais;
    private String cn;
    private String organizacion;
    private String email;
    private String nombre;
    private String telefono;
    private KeyStore keyStore;
    private X509Certificate certificate;
    private Date fechaFirma;
    private String subjectDN;
    private TimeStampToken timeStampToken;
    private SignerInformation signer;
    private CertPath certPath;
    private String contenidoFirmado;
    private HexBinaryAdapter hexConverter;

    
    public Usuario() { }
    
    public Usuario(String nif) {
        this.nif = nif;
    }
    
   /**
     * @return the id
     */
    public String getNif() {
        return nif;
    }

    /**
     * @param id the id to set
     */
    public void setNif(String nif) {
        this.nif = nif;
    }

    public static Usuario getUsuario (X509Certificate certificate) {
    	Usuario usuario = new Usuario();
    	usuario.setCertificate(certificate);
    	String subjectDN = certificate.getSubjectDN().getName();
        if (subjectDN.split("C=").length > 1) {
            usuario.setPais(subjectDN.split("C=")[1].split(",")[0]);
        }
        if (subjectDN.split("SERIALNUMBER=").length > 1)
            usuario.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.split("SURNAME=").length > 1)
            usuario.setPrimerApellido(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.split("GIVENNAME=").length > 1)
            usuario.setNombre(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
        if (subjectDN.split("O=").length > 1)
             usuario.setOrganizacion(subjectDN.split("O=")[1].split(",")[0]);
        if (subjectDN.split("CN=").length > 1)
            usuario.setCn(subjectDN.split("CN=")[1]);
    	return usuario;
    }

    public void setPais(String pais) {
            this.pais = pais;
    }

    public String getPais() {
            return pais;
    }

    public void setPrimerApellido(String primerApellido) {
            this.primerApellido = primerApellido;
    }

    public String getPrimerApellido() {
            return primerApellido;
    }

    public void setCn(String cn) {
            this.cn = cn;
    }

    public String getCn() {
            return cn;
    }

    public void setCertificate(X509Certificate certificate) {
            this.certificate = certificate;
    }

    public X509Certificate getCertificate() {
            return certificate;
    }

    public void setEmail(String email) {
            this.email = email;
    }

    public String getEmail() {
            return email;
    }

    public void setTelefono(String telefono) {
            this.telefono = telefono;
    }

    public String getTelefono() {
            return telefono;
    }

    /**
     * @return the organizacion
     */
    public String getOrganizacion() {
        return organizacion;
    }

    /**
     * @param organizacion the organizacion to set
     */
    public void setOrganizacion(String organizacion) {
        this.organizacion = organizacion;
    }

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre the nombre to set
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * @return the keyStore
     */
    public KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * @param keyStore the keyStore to set
     */
    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * @return the fechaFirma
     */
    public Date getFechaFirma() {
        if(timeStampToken != null) {
            return timeStampToken.getTimeStampInfo().getGenTime();
        } else {
            logger.debug("timeStampToken null");
            return fechaFirma;
        } 
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
        return certificate;
    }

    /**
     * @param cert the cert to set
     */
    public void setCert(X509Certificate cert) {
        this.certificate = cert;
    }

    public String getInfoCert() {
        return CMSUtils.obtenerInfoCertificado(certificate);
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
        if(hexConverter == null) hexConverter = new HexBinaryAdapter();
        return hexConverter.marshal(signer.getContentDigest());
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
        if(hexConverter == null) hexConverter = new HexBinaryAdapter();
        return hexConverter.marshal(signer.getSignature());
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

    /**
     * @return the timeStampToken
     */
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    /**
     * @param timeStampToken the timeStampToken to set
     */
    public void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }
}
