package org.votingsystem.model;

import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.EnvironmentVS;
import org.votingsystem.util.StringUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="ActorVS")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn( name="serverType", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("ActorVS")
@NamedQueries({
        @NamedQuery(name = "findActorVSByServerURL", query =
                "SELECT a FROM ActorVS a WHERE a.serverURL =:serverURL")
})
public class ActorVS extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(ActorVS.class.getSimpleName());

    public static final long serialVersionUID = 1L;

    public enum Type {CONTROL_CENTER, ACCESS_CONTROL, CURRENCY, TIMESTAMP_SERVER;}

    public enum State { SUSPENDED, OK, PAUSED;}

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
    @Transient private String timeStampServerURL;
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

    public ActorVS(String serverURL) {
        this.serverURL = serverURL;
    }

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

    public ActorVS setState(State state) {
        this.state = state;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = StringUtils.checkURL(serverURL);
    }

    public String getTimeStampServiceURL() {
        return getTimeStampServerURL() + "/timestamp";
    }

    public static String getTimeStampServiceURL(String serverURL) {
        return StringUtils.checkURL(serverURL) + "/timestamp";
    }

    public String getTimeStampDiscreteServiceURL() {
        return getTimeStampServerURL() + "/timestamp/discrete";
    }

    public static String getTimeStampDiscreteServiceURL(String serverURL) {
        return StringUtils.checkURL(serverURL) + "/timestamp/discrete";
    }

    public String getTimeStampServerURL() {
        return timeStampServerURL;
    }

    public void setUrlTimeStampServer(String timeStampServerURL) {
        this.timeStampServerURL = timeStampServerURL;
    }

    public String getServerURL() {
        return serverURL;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public ActorVS setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
        return this;
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
        timeStampCert = CertUtils.fromPEMToX509CertCollection(timeStampPEM.getBytes()).iterator().next();
    }

    public String getWebSocketURL() {
        return webSocketURL;
    }

    public void setWebSocketURL(String webSocketURL) {
        this.webSocketURL = webSocketURL;
    }

    public void setCertChainPEM(String certChainPEM) throws Exception {
        certChain = CertUtils.fromPEMToX509CertCollection(certChainPEM.getBytes());
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

    public String getRootCAServiceURL() {
        return serverURL + "/rest/certificateVS/addCertificateAuthority";
    }

    public static String getRootCAServiceURL(String serverURL) {
        return serverURL + "/rest/certificateVS/addCertificateAuthority";
    }

    public String getServerInfoURL() {
        return serverURL + "/rest/serverInfo";
    }

    public String getMultiSignedMessageTestServiceURL() {
        return serverURL + "/rest/encryptor/getMultiSignedMessage";
    }

    public String getEncryptionServiceURL() {
        return getServerURL() + "/rest/encryptor";
    }

    public static String getServerInfoURL(String serverURL) {
        return serverURL + "/rest/serverInfo";
    }

    public String getUserCertServiceURL() {
        return getServerURL() + "/rest/development/adduser";
    }

    public String getUserBaseInitServiceURL() {
        return getServerURL() + "/rest/userVS/prepareUserBaseData";
    }

    public String getReceiptViewerURL() {
        return getServerURL() + "/rest/messageSMIME/contentViewer";
    }

    public static String getReceiptViewerURL(String serverURL) {
        return serverURL + "/rest/messageSMIME/contentViewer";
    }

    public String getGroupURL(Long id) {
        return getServerURL() + "/rest/groupVS/id/" + String.valueOf(id);
    }

    public String getUserVSURL(Long id) {
        return getServerURL() + "/rest/userVS/id/" + String.valueOf(id);
    }


    public String getDeviceListByNifServiceURL(String nif) {
        return getServerURL() + "/deviceVS/" + nif + "/list";
    }

    public String getConnectedDeviceListByNifServiceURL(String nif) {
        return getServerURL() + "/deviceVS/" + nif + "/connected";
    }

    public Map getDataMap() {
        log.info("getDataMap");
        Map map = new HashMap();
        map.put("id", id);
        map.put("serverURL", serverURL);
        map.put("name", name);
        map.put("voteVSInfoURL", voteVSInfoURL);
        return map;
    }

    public static ActorVS parse(Map actorVSMap) throws Exception {
        ActorVS actorVS = null;
        Type serverType = null;
        if(actorVSMap.get("serverType") != null && !"null".equals(actorVSMap.get("serverType").toString())) {
            Object serverTypeObject = actorVSMap.get("serverType");
            if(serverTypeObject instanceof String) serverType = Type.valueOf((String) serverTypeObject);
            else if(serverTypeObject instanceof Type) serverType = (Type) serverTypeObject;
        }
        log.info("parse - serverType: " + serverType);
        switch (serverType) {
            case CONTROL_CENTER:
                actorVS = new ControlCenterVS();
                break;
            case ACCESS_CONTROL:
                actorVS =  new AccessControlVS();
                if (actorVSMap.get("controlCenter") != null) {
                    Map controlCenterMap = (Map) actorVSMap.get("controlCenter");
                    List<ControlCenterVS> controlCenters = new ArrayList<ControlCenterVS>();
                    ControlCenterVS controlCenter = new ControlCenterVS();
                    controlCenter.setName((String) controlCenterMap.get("name"));
                    controlCenter.setServerURL((String) controlCenterMap.get("serverURL"));
                    controlCenter.setId(((Integer) controlCenterMap.get("id")).longValue());
                    if (controlCenterMap.get("state") != null) {
                        controlCenter.setState(State.valueOf((String) controlCenterMap.get("state")));
                    }
                    controlCenters.add(controlCenter);
                    actorVS.setControlCenters(controlCenters);
                }
                break;
            case CURRENCY:
                actorVS = new CurrencyServer();
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
        if(actorVSMap.containsKey("timeStampServerURL")) {
            String timeStampServerURL = StringUtils.checkURL((String) actorVSMap.get("timeStampServerURL"));
            actorVS.setUrlTimeStampServer(timeStampServerURL);
        }
        return actorVS;
    }

}