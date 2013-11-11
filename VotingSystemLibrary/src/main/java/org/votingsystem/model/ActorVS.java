package org.votingsystem.model;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.votingsystem.signature.util.CertUtil;
import org.apache.log4j.Logger;

public class ActorVS {
    
    private static Logger logger = Logger.getLogger(ActorVS.class);
 
    public enum Type {CENTRO_CONTROL, CONTROL_ACCESO;}

    public enum EnvironmentMode {DEVELOPMENT, TEST, PRODUCTION; }
	
    public enum Estado { SUSPENDIDO, ACTIVO, INACTIVO;}       

    private Long id;
    private String serverURL;
    private String nombre;
    private Estado estado; 
    private Type tipo;
    private EnvironmentMode environmentMode;
    private String certificadoURL;
    private String informacionVotosURL;
    private List<ActorVS> centrosDeControl;
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
 public List<ActorVS> getCentrosDeControl() {
     return centrosDeControl;
 }

 /**
  * @param centrosDeControl the centrosDeControl to set
  */
 public void setCentrosDeControl(List<ActorVS> centrosDeControl) {
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

 public Type getType() {
         return tipo;
 }

 public void setType(Type tipo) {
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
 
 public static Map getAssociationDocumentMap(String serverURL) {
     logger.debug("getAssociationDocumentMap");
     Map map = new HashMap();
     map.put("operation", "ASOCIAR_CENTRO_CONTROL");
     map.put("serverURL", serverURL.trim());
     map.put("UUID", UUID.randomUUID().toString());     
     return map;
 }
 
 public Map getDataMap() {
     logger.debug("getDataMap");
     Map map = new HashMap();
     map.put("id", id);
     map.put("serverURL", serverURL);
     map.put("nombre", nombre);
     map.put("informacionVotosURL", informacionVotosURL);
     return map;
 }
 
 public static ActorVS populate(Map actorVSMap) throws Exception {
     logger.debug("populate");
     if(actorVSMap == null) return null;
     ActorVS actorVS = new ActorVS();
     if (actorVSMap.containsKey("serverType")){
         ActorVS.Type serverType = ActorVS.Type.valueOf((String) actorVSMap.get("serverType"));
         actorVS.setType(serverType);
          switch (serverType) {
              case CENTRO_CONTROL: break;
              case CONTROL_ACCESO:
                  if (actorVSMap.get("centrosDeControl") != null) {
                      List<ActorVS> centrosDeControl = new ArrayList<ActorVS>();
                      for (Map controlCenterMap : (List<Map>) actorVSMap.get("centrosDeControl")) {
                          ActorVS centroControl = new ActorVS();
                          centroControl.setNombre((String) controlCenterMap.get("nombre"));
                          centroControl.setServerURL((String) controlCenterMap.get("serverURL"));
                          centroControl.setId(((Integer) controlCenterMap.get("id")).longValue());
                          if (controlCenterMap.get("estado") != null) {
                              centroControl.setEstado(ActorVS.Estado.
                                      valueOf((String) controlCenterMap.get("estado")));
                          }
                          centrosDeControl.add(centroControl);
                      }
                      actorVS.setCentrosDeControl(centrosDeControl);
                  }
                  break;
          }   
     }
     if(actorVSMap.containsKey("id")) 
    	 actorVS.setId(((Integer) actorVSMap.get("id")).longValue());
     if (actorVSMap.containsKey("environmentMode")) {
         actorVS.setEnvironmentMode(ActorVS.EnvironmentMode.valueOf(
                 (String) actorVSMap.get("environmentMode")));
     }
     if (actorVSMap.containsKey("cadenaCertificacionURL"))
          actorVS.setCertificadoURL((String) actorVSMap.
                  get("cadenaCertificacionURL"));
     if (actorVSMap.containsKey("serverURL"))
          actorVS.setServerURL((String) actorVSMap.get("serverURL"));
     if (actorVSMap.containsKey("nombre"))
          actorVS.setNombre((String) actorVSMap.get("nombre"));
     if(actorVSMap.containsKey("informacionVotosURL")) actorVS.
             setInformacionVotosURL((String) actorVSMap.
             get("informacionVotosURL"));        
     if (actorVSMap.containsKey("cadenaCertificacionPEM")) {
         actorVS.setCadenaCertificacionPEM((String) actorVSMap.get(
                     "cadenaCertificacionPEM"));
     }
     if (actorVSMap.containsKey("timeStampCertPEM")) {
         actorVS.setTimeStampCertPEM((String) actorVSMap.get(
                     "timeStampCertPEM"));
     }
     return actorVS;
 }

 /**
  * @return the trustAnchors
  */
 public Set<TrustAnchor> getTrustAnchors() {
     return trustAnchors;
 }

}
