package org.votingsystem.model;

import java.io.Serializable;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.tsp.TimeStampToken;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.SignerInformation;
import org.votingsystem.signature.util.CMSUtils;
import javax.xml.bind.DatatypeConverter;
import org.bouncycastle.util.encoders.Hex;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class UserVSBase implements Serializable, UserVS {
	
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = Logger.getLogger(UserVS.class);

    public enum Type {USER, REPRESENTATIVE, USER_WITH_CANCELLED_REPRESENTATIVE, EX_REPRESENTATIVE}


    private Type type;
    private String nif;
    private String nombre;
    private String metaInf = "{\"numRepresentations\"=1}"; 
    private String primerApellido;
    private String info; 
    private String pais;
    private String telefono;
    private String email;
    private String cn;
    private UserVS representative;  
    private Date dateCreated;
    private Date lastUpdated;
    private Date representativeRegisterDate;
    private X509Certificate certificate;
    private CertificateVS certificateCA;
    private TimeStampToken timeStampToken;
    private KeyStore keyStore;
    private SignerInformation signerInformation;

    public UserVSBase() {}
    
    public UserVSBase(String nif) {
        this.nif = nif;
    }
    
    
   /**
     * @return the nif
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

    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
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

    public static UserVS getUserVS (X509Certificate certificate) {
    	UserVS usuario = new UserVSBase();
    	usuario.setCertificate(certificate);
    	String subjectDN = certificate.getSubjectDN().getName();
    	if (subjectDN.contains("C="))
    		usuario.setPais(subjectDN.split("C=")[1].split(",")[0]);
    	if (subjectDN.contains("SERIALNUMBER="))
    		usuario.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
    	if (subjectDN.contains("SURNAME="))
    		usuario.setPrimerApellido(subjectDN.split("SURNAME=")[1].split(",")[0]);
    	if (subjectDN.contains("GIVENNAME="))
    		usuario.setNombre(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
    	if (subjectDN.contains("CN="))
    		usuario.setCn(subjectDN.split("CN=")[1]);
		if(subjectDN.split("OU=email:").length > 1) {
			usuario.setEmail(subjectDN.split("OU=email:")[1].split(",")[0]);
		}
		if(subjectDN.split("CN=nif:").length > 1) {
			String nif = subjectDN.split("CN=nif:")[1];
			if (nif.split(",").length > 1) {
				nif = nif.split(",")[0];
			}
			usuario.setNif(nif);
		}
		if (subjectDN.split("OU=telefono:").length > 1) {
			usuario.setTelefono(subjectDN.split("OU=telefono:")[1].split(",")[0]);
		}
    	return usuario;
    }
    
    /*public String getInfoCert() {
        return CertUtil.obtenerInfoCertificado(certificate);
    }*/
    
    /**
     * @return the certPath
     */
   /* public CertPath getCertPath() {
        return certPath;
    }*/

    /**
     * @param certPath the certPath to set
     */
    /*public void setCertPath(CertPath certPath) {
        this.certPath = certPath;
    }*/
    
        /**
     * @return the contentDigest
     */
    public String getContentDigestBase64() {
        if (signerInformation.getContentDigest() == null) return null;
        return DatatypeConverter.printBase64Binary(signerInformation.getContentDigest());
    }

    /**
     * @return the contentDigest
     */
    public String getContentDigestHex() {
        if (signerInformation.getContentDigest() == null) return null;
        return new String(Hex.encode(signerInformation.getContentDigest()));
    }

    /**
     * @return the contentDigest
     */
    public String getSignatureBase64() {
        if (signerInformation.getSignature() == null) return null;	
        return DatatypeConverter.printBase64Binary(
                                signerInformation.getSignature());
    }

        /**
     * @return the contentDigest
     */
    public String getFirmaHex() {
        if (signerInformation.getSignature() == null) return null;
        return new String(signerInformation.getSignature());
    }

    /**
     * @param signer the signer to set
     */
    public void setSigner(SignerInformation signer) {
        this.signerInformation = signer;
    }
    
    public String getEncryptiontId() {
        return CMSUtils.getEncryptiontId(signerInformation.getEncryptionAlgOID());
    }

    public Date getSignatureDate() {
        if(timeStampToken == null) return null;
        return timeStampToken.getTimeStampInfo().getGenTime();
    }
    
    public String getDigestId() {
        return CMSUtils.getDigestId(signerInformation.getDigestAlgOID());
    }
    
    public String getDescription () {
    	String result = "";
    	if (pais != null) result.concat(" - Pais: " + pais);
    	if (nif != null) result.concat(" - Nif: " + nif);
    	if (primerApellido != null) result.concat(" - Apellido: " + primerApellido);
    	if (nombre != null) result.concat(" - Nombre: " + nombre);
    	if (cn != null) result.concat(" - CN: " + cn);
    	return result;
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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void beforeInsert(){
        if(nif != null) this.nif = nif.toUpperCase();
    }

    public CertificateVS getCertificadoCA() {
        return certificateCA;
    }

    public void setCertificadoCA(CertificateVS certificateCA) {
        
    }
        
        
    @Override public void setCertificateCA(CertificateVS certificate) {
        this.certificateCA = certificateCA;
    }

    public UserVS getRepresentative() {
        return representative;
    }

    public void setRepresentative(UserVS representative) {
        this.representative = representative;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }


    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    public void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }

    public String getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(String metaInf) {
        this.metaInf = metaInf;
    }

    public Date getRepresentativeRegisterDate() {
        return representativeRegisterDate;
    }

    public void setRepresentativeRegisterDate(Date representativeRegisterDate) {
        this.representativeRegisterDate = representativeRegisterDate;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    
    @Override public void setSignerInformation(SignerInformation signer) {
        this.signerInformation = signer;
    }
}