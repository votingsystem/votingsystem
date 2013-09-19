package org.sistemavotacion.modelo;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.util.DateUtils;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ActorConIP implements java.io.Serializable {
	
	public static final String TAG = "ActorConIP";

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
	
    public static ActorConIP parse(String actorConIPStr, ActorConIP.Tipo tipo) 
 		   throws Exception {
        JSONObject actorConIPJSON = new JSONObject(actorConIPStr);
        JSONObject jsonObject = null;
        ActorConIP actorConIP = null;
        JSONArray jsonArray;
        switch (tipo) {
             case CENTRO_CONTROL:
                 actorConIP = new CentroControl();
                 break;
             case CONTROL_ACCESO:
                 actorConIP = new ControlAcceso();
                 ((ControlAcceso)actorConIP).setUrlClientePublicacionJNLP(actorConIPStr);
                 if (actorConIPJSON.getJSONArray("centrosDeControl") != null) {
                     Set<CentroControl> centrosDeControl = new HashSet<CentroControl>();
                     jsonArray = actorConIPJSON.getJSONArray("centrosDeControl");
                     for (int i = 0; i< jsonArray.length(); i++) {
                         jsonObject = jsonArray.getJSONObject(i);
                         CentroControl centroControl = new CentroControl();
                         centroControl.setNombre(jsonObject.getString("nombre"));
                         centroControl.setServerURL(jsonObject.getString("serverURL"));
                         centroControl.setId(jsonObject.getLong("id"));
                         centroControl.setDateCreated(DateUtils.getDateFromString(jsonObject.getString("fechaCreacion")));
                         if (jsonObject.getString("estado") != null) {
                              centroControl.setEstado(ActorConIP.Estado.valueOf(jsonObject.getString("estado")));
                         }
                         centrosDeControl.add(centroControl);
                     }
                     ((ControlAcceso)actorConIP).setCentrosDeControl(centrosDeControl);
                 }
                 break;
            
        }
        if (actorConIPJSON.has("urlBlog"))
             actorConIP.setUrlBlog(actorConIPJSON.getString("urlBlog"));
        if (actorConIPJSON.has("serverURL"))
             actorConIP.setServerURL(actorConIPJSON.getString("serverURL"));
        if (actorConIPJSON.has("nombre"))
             actorConIP.setNombre(actorConIPJSON.getString("nombre"));
        if (actorConIPJSON.has("cadenaCertificacionPEM")) {
     	   Collection<X509Certificate> certChain = 
	        			CertUtil.fromPEMToX509CertCollection(actorConIPJSON.
	        			getString("cadenaCertificacionPEM").getBytes());
	        	actorConIP.setCertChain(certChain);
	        	X509Certificate serverCert = certChain.iterator().next();
	        	Log.d(TAG + ".obtenerActorConIP(..) ", " - actorConIP Cert: " 
	        			+ serverCert.getSubjectDN().toString());
	        	actorConIP.setCertificado(serverCert);
        }
        if (actorConIPJSON.has("timeStampCertPEM")) {
     	   actorConIP.setTimeStampCertPEM(actorConIPJSON.getString(
                    "timeStampCertPEM"));
        }
        return actorConIP;
    }
}
