package org.votingsystem.model;

import org.apache.log4j.Logger;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.StringUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="ActorVS")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn( name="serverType", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("ActorVS")
public class ActorVS implements Serializable {

    private static Logger log = Logger.getLogger(ActorVS.class);

    public static final long serialVersionUID = 1L;

    public enum Type {CONTROL_CENTER, ACCESS_CONTROL, VICKETS, TIMESTAMP_SERVER;}

    public enum State { SUSPENDED, RUNNING, PAUSED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @Column(name="serverURL") private String serverURL;

    @Column(name="name") private String name;

    @Enumerated(EnumType.STRING)
    @Column(name="state") private State state;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23, insertable=true) public Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23, insertable=true) public Date lastUpdated;

    @Transient private Type serverType;
    @Transient private String certChainPEM;
    @Transient private String webSocketURL;
    @Transient private String urlTimeStampServer;
    @Transient private Collection<X509Certificate> certChain;
    @Transient private transient X509Certificate x509Certificate;
    @Transient private Set<TrustAnchor> trustAnchors = null;
    @Transient private CertificateVS certificateVS;
    @Transient private EnvironmentVS environmentMode;
    @Transient private String certificateURL;
    @Transient private String voteVSInfoURL;
    @Transient private List<ControlCenterVS> controlCenters;
    @Transient private X509Certificate timeStampCert = null;

    public ActorVS() { }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public String getNameNormalized () {
        return name.replaceAll("[\\/:.]", "");
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = StringUtils.checkURL(serverURL);
    }

    public String getTimeStampServiceURL() {
        return getTimeStampServerURL() + "/timeStamp";
    }

    public static String getTimeStampServiceURL(String serverURL) {
        return StringUtils.checkURL(serverURL) + "/timeStamp";
    }

    public String getTimeStampServerURL() {
        return urlTimeStampServer;
    }

    public void setUrlTimeStampServer(String urlTimeStampServer) {
        this.urlTimeStampServer = urlTimeStampServer;
    }

    public String getServerURL() {
        return serverURL;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public CertificateVS getCertificateVS() {
        return certificateVS;
    }

    public void setCertificateVS(CertificateVS certificateVS) {
        this.certificateVS = certificateVS;
    }

    public Type getType() { return serverType; }

    public void setType(Type serverType) { this.serverType = serverType; }

    public String getVoteVSInfoURL() {
        return voteVSInfoURL;
    }

    public void setVoteVSInfoURL(String voteVSInfoURL) {
        this.voteVSInfoURL = voteVSInfoURL;
    }

    public EnvironmentVS getEnvironmentVS() {
        return environmentMode;
    }

    public void setEnvironmentVS(EnvironmentVS environmentMode) {
        this.environmentMode = environmentMode;
    }

    public String getCertificateURL() {return certificateURL;}

    public void setCertificateURL(String certificateURL) {
        this.certificateURL = certificateURL;
    }

    public X509Certificate getTimeStampCert() {
        return timeStampCert;
    }

    public void setTimeStampCert(X509Certificate timeStampCert) {
        this.timeStampCert = timeStampCert;
    }

    public Set<TrustAnchor> getTrustAnchors() {
        return trustAnchors;
    }

    public void setTrustAnchors(Set<TrustAnchor> trustAnchors) {
        this.trustAnchors = trustAnchors;
    }

    public String getCertChainPEM() {
        return certChainPEM;
    }

    public void setTimeStampCertPEM(String timeStampPEM) throws Exception {
        timeStampCert = CertUtil.fromPEMToX509CertCollection(timeStampPEM.getBytes()).iterator().next();
    }

    public String getWebSocketURL() {
        return webSocketURL;
    }

    public void setWebSocketURL(String webSocketURL) {
        this.webSocketURL = webSocketURL;
    }

    public void setCertChainPEM(String certChainPEM) throws Exception {
        certChain = CertUtil.fromPEMToX509CertCollection(certChainPEM.getBytes());
        x509Certificate = certChain.iterator().next();
        trustAnchors = new HashSet<TrustAnchor>();
        for (X509Certificate cert:certChain) {
            trustAnchors.add(new TrustAnchor(cert, null));
        }
        this.certChainPEM = certChainPEM;
    }

    public Collection<X509Certificate> getCertChain() {
        return certChain;
    }

    public List<ControlCenterVS> getControlCenters() {
        return controlCenters;
    }

    public void setControlCenters(List<ControlCenterVS> controlCenters) {
        this.controlCenters = controlCenters;
    }

    @Transient public String getRootCAServiceURL() {
        return serverURL + "/certificateVS/addCertificateAuthority";
    }

    public static String getRootCAServiceURL(String serverURL) {
        return serverURL + "/certificateVS/addCertificateAuthority";
    }

    @Transient public String getServerInfoURL() {
        return serverURL + "/serverInfo";
    }

    @Transient public String getMultiSignedMessageTestServiceURL() {
        return serverURL + "/encryptor/getMultiSignedMessage";
    }

    @Transient public static String getServerInfoURL(String serverURL) {
        return serverURL + "/serverInfo";
    }

    @Transient public String getVicketRequestServiceURL() {
        return getServerURL() + "/model/request";
    }

    @Transient public String getVicketDepositServiceURL() {
        return getServerURL() + "/model/deposit";
    }

    @Transient public String getUserCertServiceURL() {
        return getServerURL() + "/userVS";
    }

    @Transient public String getMessageVSInboxURL() {
        return getServerURL() + "/messageVS/inbox";
    }

    @Transient public String getUserBaseInitServiceURL() {
        return getServerURL() + "/userVS/prepareUserBaseData";
    }

    @Transient public Map getDataMap() {
        log.debug("getDataMap");
        Map map = new HashMap();
        map.put("id", id);
        map.put("serverURL", serverURL);
        map.put("name", name);
        map.put("voteVSInfoURL", voteVSInfoURL);
        return map;
    }

    public static Map getAssociationDocumentMap(String serverURL) {
        log.debug("getAssociationDocumentMap");
        Map map = new HashMap();
        map.put("operation", "CONTROL_CENTER_ASSOCIATION");
        map.put("serverURL", serverURL.trim());
        map.put("UUID", UUID.randomUUID().toString());
        return map;
    }

    public static ActorVS populate(Map actorVSMap) throws Exception {
        ActorVS actorVS = null;
        Type serverType = null;
        if(actorVSMap.get("serverType") != null && !"null".equals(actorVSMap.get("serverType").toString())) {
            Object serverTypeObject = actorVSMap.get("serverType");
            if(serverTypeObject instanceof String) serverType = Type.valueOf((String) serverTypeObject);
            else if(serverTypeObject instanceof Type) serverType = (Type) serverTypeObject;
        }
        log.debug("populate - serverType: " + serverType);
        switch (serverType) {
            case CONTROL_CENTER:
                actorVS = new ControlCenterVS();
                break;
            case ACCESS_CONTROL:
                actorVS =  new AccessControlVS();
                if (actorVSMap.get("controlCenters") != null) {
                    List<ControlCenterVS> controlCenters = new ArrayList<ControlCenterVS>();
                    for (Map controlCenterMap : (List<Map>) actorVSMap.get("controlCenters")) {
                        ControlCenterVS controlCenter = new ControlCenterVS();
                        controlCenter.setName((String) controlCenterMap.get("name"));
                        controlCenter.setServerURL((String) controlCenterMap.get("serverURL"));
                        controlCenter.setId(((Integer) controlCenterMap.get("id")).longValue());
                        if (controlCenterMap.get("state") != null) {
                            controlCenter.setState(State.valueOf((String) controlCenterMap.get("state")));
                        }
                        controlCenters.add(controlCenter);
                    }
                    actorVS.setControlCenters(controlCenters);
                }
                break;
            case VICKETS:
                actorVS = new VicketServer();
                break;
            default:
                actorVS = new ActorVS();
                actorVS.setType(serverType);
                break;
        }
        if(actorVSMap.containsKey("id") && !"null".equals(actorVSMap.get("id").toString()))
            actorVS.setId(((Integer) actorVSMap.get("id")).longValue());
        if (actorVSMap.containsKey("environmentMode")) {
            actorVS.setEnvironmentVS(EnvironmentVS.valueOf((String) actorVSMap.get("environmentMode")));
        }
        if (actorVSMap.containsKey("certChainURL"))
            actorVS.setCertificateURL((String) actorVSMap.get("certChainURL"));
        if (actorVSMap.containsKey("serverURL")) {
            String serverURL = StringUtils.checkURL((String) actorVSMap.get("serverURL"));
            actorVS.setServerURL(serverURL);
        }
        if (actorVSMap.containsKey("webSocketURL")) {
            actorVS.setWebSocketURL((String) actorVSMap.get("webSocketURL"));
        }
        if (actorVSMap.containsKey("name")) actorVS.setName((String) actorVSMap.get("name"));
        if(actorVSMap.containsKey("voteVSInfoURL"))
            actorVS.setVoteVSInfoURL((String) actorVSMap.get("voteVSInfoURL"));
        if (actorVSMap.containsKey("certChainPEM")) {
            actorVS.setCertChainPEM((String) actorVSMap.get("certChainPEM"));
        }
        if (actorVSMap.containsKey("timeStampCertPEM")) {
            actorVS.setTimeStampCertPEM((String) actorVSMap.get("timeStampCertPEM"));
        }
        if(actorVSMap.containsKey("urlTimeStampServer")) {
            String urlTimeStampServer = StringUtils.checkURL((String) actorVSMap.get("urlTimeStampServer"));
            actorVS.setUrlTimeStampServer(urlTimeStampServer);
        }
        return actorVS;
    }

}
