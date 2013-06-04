package org.sistemavotacion.modelo;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.seguridad.CertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ActorConIP {
        
    private static Logger logger = LoggerFactory.getLogger(ActorConIP.class);
    
    public enum Tipo {CENTRO_CONTROL, CONTROL_ACCESO;}
	
    public enum EnvironmentMode {DEVELOPMENT, TEST, PRODUCTION; }
	
    public enum Estado { SUSPENDIDO, ACTIVO, INACTIVO;}       
    
    private Long id;
    private String serverURL;
    private String nombre;
    private Estado estado; 
    private Tipo tipo;
    private EnvironmentMode environmentMode;
    private String certificadoURL;
    private String informacionVotosURL;
    private Set<ActorConIP> centrosDeControl;
    private X509Certificate certificate = null;
    private X509Certificate timeStampCert = null;
    private Set<TrustAnchor> trustAnchors = null;

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
     * @return the serverURL
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * @param serverURL the serverURL to set
     */
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
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
     * @return the centrosDeControl
     */
    public Set<ActorConIP> getCentrosDeControl() {
        return centrosDeControl;
    }

    /**
     * @param centrosDeControl the centrosDeControl to set
     */
    public void setCentrosDeControl(Set<ActorConIP> centrosDeControl) {
        this.centrosDeControl = centrosDeControl;
    }
    
    /**
     * @return the informacionVotosURL
     */
    public String getInformacionVotosURL() {
        return informacionVotosURL;
    }

    /**
     * @param informacionVotosURL the informacionVotosURL to set
     */
    public void setInformacionVotosURL(String informacionVotosURL) {
        this.informacionVotosURL = informacionVotosURL;
    }
    
    /**
     * @param cadenaCertificacionPEM la cadena de certifcación del actor en formato PEM
     */
    public void setCadenaCertificacionPEM(String cadenaCertificacionPEM) throws Exception {
        certificate = CertUtil.fromPEMToX509CertCollection(
                    cadenaCertificacionPEM.getBytes()).iterator().next();
        Collection<X509Certificate> certificates = CertUtil.
                fromPEMToX509CertCollection(cadenaCertificacionPEM.getBytes());
        trustAnchors = new HashSet<TrustAnchor>();
        for (X509Certificate cert:certificates) {
            TrustAnchor anchorCertificado = new TrustAnchor(cert, null);
            trustAnchors.add(anchorCertificado);
        }
    }
     
    public void setTimeStampCertPEM(String timeStampPEM) throws Exception {
        timeStampCert = CertUtil.fromPEMToX509CertCollection(
                    timeStampPEM.getBytes()).iterator().next();
    }

    public Estado getEstado() {
            return estado;
    }

    public void setEstado(Estado estado) {
            this.estado = estado;
    }

    public Tipo getTipo() {
            return tipo;
    }

    public void setTipo(Tipo tipo) {
            this.tipo = tipo;
    }

    public EnvironmentMode getEnvironmentMode() {
            return environmentMode;
    }

    public void setEnvironmentMode(EnvironmentMode environmentMode) {
            this.environmentMode = environmentMode;
    }

    public String getCertificadoURL() {
            return certificadoURL;
    }

    public void setCertificadoURL(String certificadoURL) {
            this.certificadoURL = certificadoURL;
    }

    /**
     * @return the certificate
     */
    public X509Certificate getCertificate() {
        return certificate;
    }

    /**
     * @param certificate the certificate to set
     */
    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    /**
     * @return the timeStampCert
     */
    public X509Certificate getTimeStampCert() {
        return timeStampCert;
    }

    /**
     * @param timeStampCert the timeStampCert to set
     */
    public void setTimeStampCert(X509Certificate timeStampCert) {
        this.timeStampCert = timeStampCert;
    }
    
    public static JSONObject getAssociationDocumentJSON(String serverURL) {
        logger.debug("getAssociationDocumentJSON");
        Map map = new HashMap();
        map.put("operation", "ASOCIAR_CENTRO_CONTROL");
        map.put("serverURL", serverURL.trim());
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON( map );        
        return jsonObject;
    }
    
    public JSONObject obtenerJSON() {
        logger.debug("obtenerJSON");
        Map map = new HashMap();
        map.put("id", id);
        map.put("serverURL", serverURL);
        map.put("nombre", nombre);
        map.put("informacionVotosURL", informacionVotosURL);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);        
        return jsonObject;
    }
        
    public static ActorConIP parse (JSONObject actorConIPJSON) throws Exception {
        if(actorConIPJSON == null) return null;
        return parse(actorConIPJSON.toString());
    }
    
    public static ActorConIP parse(String actorConIPStr) throws Exception {
        logger.debug("parse - ActorConIP: " + actorConIPStr);
        if(actorConIPStr == null) return null;
        JSONObject actorConIPJSON = (JSONObject) JSONSerializer.toJSON(actorConIPStr);
        JSONObject jsonObject = null;
        ActorConIP actorConIP = new ActorConIP();
        JSONArray jsonArray;
        if (actorConIPJSON.containsKey("tipoServidor")){
            ActorConIP.Tipo tipoServidor = ActorConIP.Tipo.valueOf(actorConIPJSON.getString("tipoServidor"));
            actorConIP.setTipo(tipoServidor);
             switch (tipoServidor) {
                 case CENTRO_CONTROL: break;
                 case CONTROL_ACCESO:
                     if (actorConIPJSON.getJSONArray("centrosDeControl") != null) {
                         Set<ActorConIP> centrosDeControl = new HashSet<ActorConIP>();
                         jsonArray = actorConIPJSON.getJSONArray("centrosDeControl");
                         for (int i = 0; i< jsonArray.size(); i++) {
                             jsonObject = jsonArray.getJSONObject(i);
                             ActorConIP centroControl = new ActorConIP();
                             centroControl.setNombre(jsonObject.getString("nombre"));
                             centroControl.setServerURL(jsonObject.getString("serverURL"));
                             centroControl.setId(jsonObject.getLong("id"));
                             if (jsonObject.getString("estado") != null) {
                                 centroControl.setEstado(ActorConIP.Estado.
                                         valueOf(jsonObject.getString("estado")));
                             }
                             centrosDeControl.add(centroControl);
                         }
                         actorConIP.setCentrosDeControl(centrosDeControl);
                     }
                     break;
             }   
        }
        if(actorConIPJSON.containsKey("id") && !JSONNull.getInstance().
                equals(actorConIPJSON.get("id"))) actorConIP.setId(
                actorConIPJSON.getLong("id"));
        if (actorConIPJSON.containsKey("environmentMode")) {
            actorConIP.setEnvironmentMode(ActorConIP.EnvironmentMode.valueOf(
                    actorConIPJSON.getString("environmentMode")));
        }
        if (actorConIPJSON.containsKey("cadenaCertificacionURL"))
             actorConIP.setCertificadoURL(actorConIPJSON.
                     getString("cadenaCertificacionURL"));
        if (actorConIPJSON.containsKey("serverURL"))
             actorConIP.setServerURL(actorConIPJSON.getString("serverURL"));
        if (actorConIPJSON.containsKey("nombre"))
             actorConIP.setNombre(actorConIPJSON.getString("nombre"));
        if(actorConIPJSON.containsKey("informacionVotosURL")) actorConIP.
                setInformacionVotosURL(actorConIPJSON.
                getString("informacionVotosURL"));        
        if (actorConIPJSON.containsKey("cadenaCertificacionPEM")) {
            actorConIP.setCadenaCertificacionPEM(actorConIPJSON.getString(
                        "cadenaCertificacionPEM"));
        }
        if (actorConIPJSON.containsKey("timeStampCertPEM")) {
            actorConIP.setTimeStampCertPEM(actorConIPJSON.getString(
                        "timeStampCertPEM"));
        }
        return actorConIP;
    }

    /**
     * @return the trustAnchors
     */
    public Set<TrustAnchor> getTrustAnchors() {
        return trustAnchors;
    }

}
