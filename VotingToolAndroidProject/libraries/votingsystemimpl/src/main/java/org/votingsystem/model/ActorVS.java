package org.votingsystem.model;

import org.votingsystem.signature.util.CertUtil;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

public class ActorVS implements java.io.Serializable {
	
	public static final String TAG = "ActorVS";

    private static final long serialVersionUID = 1L;
        
    public enum Type {CONTROL_CENTER, ACCESS_CONTROL}

    public enum State {CANCELLED, ACTIVE, PAUSED}

    private Long id;
    private String serverURL;
    private String name;
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

    public void setServerURL(String serverURL) {this.serverURL = serverURL;}

    public String getServerURL() {
            return serverURL;
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

    public X509Certificate getTimeStampCert() {
    	return timeStampCert;
    }

}
