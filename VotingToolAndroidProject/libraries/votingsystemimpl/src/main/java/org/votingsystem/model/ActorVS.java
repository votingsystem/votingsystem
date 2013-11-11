package org.votingsystem.model;

import org.votingsystem.signature.util.CertUtil;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

public class ActorVS implements java.io.Serializable {
	
	public static final String TAG = "ActorVS";

    private static final long serialVersionUID = 1L;
        
    public enum Tipo {CENTRO_CONTROL, CONTROL_ACCESO}

    public enum Estado {
        SUSPENDIDO ("Suspendido"), ACTIVO("Activo"), INACTIVO("Inactivo");
        private String mensaje;
        Estado(String mensaje) {
            this.mensaje = mensaje;
        }
        public String getMensaje() {
            return this.mensaje;
        }
    }        

    private Long id;
    private String serverURL;
    private String nombre;
    private String urlBlog;
    private Date dateCreated;
    private Date lastUpdated;
    private Estado estado; 
    private Tipo tipo;
    private String certificadoURL;
    private String certificadoPEM;
    private X509Certificate certificado;
    private Collection<X509Certificate> certChain;
    private X509Certificate timeStampCert = null;

    public void setServerURL(String serverURL) {
            this.serverURL = serverURL;
    }
    public String getServerURL() {
            return serverURL;
    }

    public void setUrlBlog(String urlBlog) {
            this.urlBlog = urlBlog;
    }
    public String getURLBlog() {
            return urlBlog;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
    }
    
    public String getNombreNormalizado () {
        return nombre.replaceAll("[\\/:.]", ""); 
    }

    /**
     * @param nombre the nombre to set
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
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
     * @return the estado
     */
    public Estado getEstado() {
        return estado;
    }

    /**
     * @param estado the estado to set
     */
    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    /**
     * @return the tipo
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * @param tipo the tipo to set
     */
    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }
    
    
    /**
     * @return the certificadoURL
     */
    public String getCertificadoURL() {
        return certificadoURL;
    }

    /**
     * @param certificadoURL the certificadoURL to set
     */
    public void setCertificadoURL(String certificadoURL) {
        this.certificadoURL = certificadoURL;
    }

    /**
     * @return the certificadoPEM
     */
    public String getCertificadoPEM() {
        return certificadoPEM;
    }

    /**
     * @param certificadoPEM the certificadoPEM to set
     */
    public void setCertificadoPEM(String certificadoPEM) {
        this.certificadoPEM = certificadoPEM;
    }
    
    
    /**
     * @return the certificado
     */
    public X509Certificate getCertificado() {
        return certificado;
    }

    /**
     * @param certificado the certificado to set
     */
    public void setCertificado(X509Certificate certificado) {
        this.certificado = certificado;
    }
	public Collection<X509Certificate> getCertChain() {
		return certChain;
	}
	public void setCertChain(Collection<X509Certificate> certChain) {
		this.certChain = certChain;
	}
	
    public void setTimeStampCertPEM(String timeStampPEM) throws Exception {
        timeStampCert = CertUtil.fromPEMToX509CertCollection(
                    timeStampPEM.getBytes()).iterator().next();
    }

    public X509Certificate getTimeStampCert() {
    	return timeStampCert;
    }

}
