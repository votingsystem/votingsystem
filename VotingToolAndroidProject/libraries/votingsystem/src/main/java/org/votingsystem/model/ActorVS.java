package org.votingsystem.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.StringUtils;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class ActorVS implements java.io.Serializable {
	
	public static final String TAG = "ActorVS";

    private static final long serialVersionUID = 1L;

    public EnvironmentVS getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentVS environment) {
        this.environment = environment;
    }

    public enum Type {CONTROL_CENTER, ACCESS_CONTROL, TICKETS, TIMESTAMP_SERVER;}

    public enum State {CANCELLED, ACTIVE, PAUSED, RUNNING}

    private Long id;
    private EnvironmentVS environment;
    private String serverURL;
    private String name;
    private String urlTimeStampServer;
    private String urlBlog;
    private Date dateCreated;
    private Date lastUpdated;
    private State state;
    private Type type;
    private String certificateURL;
    private String certificatePEM;
    private X509Certificate certificate;
    private Collection<X509Certificate> certChain;
    private X509Certificate timeStampCert = null;

    public void setServerURL(String serverURL) {
        this.serverURL = StringUtils.checkURL(serverURL);
    }

    public String getServerURL() {
            return serverURL;
    }

    public String getCertChainURL () {
        return serverURL + "/certificateVS/certChain";
    }


    public void setUrlBlog(String urlBlog) {
            this.urlBlog = urlBlog;
    }

    public String getURLBlog() {
            return urlBlog;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
    public String getNameNormalized () { return name.replaceAll("[\\/:.]", ""); }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public State getState() {
        return state;    }

    public void setState(State state) { this.state = state; }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getCertificateURL() {
        return certificateURL;
    }

    public void setCertificateURL(String certificateURL) {
        this.certificateURL = certificateURL;
    }

    public String getCertificatePEM() {
        return certificatePEM;
    }

    public void setCertificatePEM(String certificatePEM) {
        this.certificatePEM = certificatePEM;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public Collection<X509Certificate> getCertChain() {
		return certChain;
	}

	public void setCertChain(Collection<X509Certificate> certChain) {
		this.certChain = certChain;
	}
	
    public void setTimeStampCertPEM(String timeStampPEM) throws Exception {
        timeStampCert = CertUtil.fromPEMToX509CertCollection(timeStampPEM.getBytes()).iterator().next();
    }

    public String getTimeStampServiceURL() {
        return getUrlTimeStampServer() + "/timeStamp";
    }

    public String getUrlTimeStampServer() {
        return urlTimeStampServer;
    }

    public void setUrlTimeStampServer(String urlTimeStampServer) {
        this.urlTimeStampServer = StringUtils.checkURL(urlTimeStampServer);
    }

    public X509Certificate getTimeStampCert() {
    	return timeStampCert;
    }

    public static String getServerInfoURL (String serverURL) {
        return StringUtils.checkURL(serverURL) + "/serverInfo";
    }

    public static ActorVS parse(JSONObject actorVSJSON) throws Exception {
        JSONObject jsonObject = null;
        JSONArray jsonArray;
        Type serverType = Type.valueOf(actorVSJSON.getString("serverType"));
        ActorVS actorVS = null;
        switch(serverType) {
            case ACCESS_CONTROL:
                actorVS = new AccessControlVS();
                break;
            case CONTROL_CENTER:
                actorVS = new ControlCenterVS();
                break;
            case TICKETS:
                actorVS = new TicketServer();
                break;
            case TIMESTAMP_SERVER:
                actorVS = new ActorVS();
                break;
        }
        actorVS.setType(serverType);
        if (actorVSJSON.has("urlBlog")) actorVS.setUrlBlog(actorVSJSON.getString("urlBlog"));
        if (actorVSJSON.has("serverURL")) actorVS.setServerURL(actorVSJSON.getString("serverURL"));
        if (actorVSJSON.has("name")) actorVS.setName(actorVSJSON.getString("name"));
        if (actorVSJSON.has("urlTimeStampServer")) actorVS.setUrlTimeStampServer(
                actorVSJSON.getString("urlTimeStampServer"));
        if (actorVSJSON.has("environmentMode")) actorVS.setEnvironment(EnvironmentVS.valueOf(
                actorVSJSON.getString("environmentMode")));
        if (actorVSJSON.has("certChainPEM")) {
            Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(
                    actorVSJSON.getString("certChainPEM").getBytes());
            actorVS.setCertChain(certChain);
            X509Certificate serverCert = certChain.iterator().next();
            Log.d(TAG + ".getActorConIP(..) ", " - actorVS Cert: " +
                    serverCert.getSubjectDN().toString());
            actorVS.setCertificate(serverCert);
        }
        if (actorVSJSON.has("timeStampCertPEM")) {
            actorVS.setTimeStampCertPEM(actorVSJSON.getString("timeStampCertPEM"));
        }
        return actorVS;
    }
}
