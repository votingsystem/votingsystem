package org.votingsystem.model;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.CMSDto;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;


/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="CMSMessage")
@NamedQueries({
        @NamedQuery(name = "findcmsMessageByBase64ContentDigest", query =
                "SELECT m FROM CMSMessage m WHERE m.base64ContentDigest =:base64ContentDigest")
})
public class CMSMessage extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(CMSMessage.class.getName());

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="type") @Enumerated(EnumType.STRING) private TypeVS type;
    @Column(name="content") private byte[] contentPEM;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userId") private User user;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="cmsParent") private CMSMessage cmsParent;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated") private Date dateCreated;
    
    @Column(name="metaInf", columnDefinition="TEXT")  private String metaInf;
    
    //To avoid repeated messages
    @Column(name="base64ContentDigest", unique=true) 
    private String base64ContentDigest;

    @Column(name="reason", columnDefinition="TEXT") private String reason;
    @Column(name="signedContent", columnDefinition="TEXT") private String signedContent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated")
    private Date lastUpdated;

    private static ThreadLocal<CMSMessage> instance = new ThreadLocal() {
        protected CMSMessage initialValue() {
            return null;
        }
    };

    public static CMSMessage getCurrent() {
        return (CMSMessage)instance.get();
    }

    public static void setCurrentInstance(CMSMessage cmsMessage) {
        if(cmsMessage == null) {
            instance.remove();
        } else {
            instance.set(cmsMessage);
        }
    }

    @Transient private transient CMSSignedMessage cmsMessage;
    @Transient private transient Set<User> signers;
    @Transient private transient User anonymousSigner;

    public byte[] getContentPEM() {
        return contentPEM;
    }

    public CMSMessage() {}

    public CMSMessage(String reason, TypeVS typeVS, String metaInf, byte[] contentPEM) throws Exception {
        this.reason = reason;
        this.type = typeVS;
        this.metaInf = metaInf;
        setCMS(CMSSignedMessage.FROM_PEM(contentPEM));
    }

    public CMSMessage(CMSSignedMessage cms, CMSDto dto, TypeVS type) throws Exception {
        this.type = type;
        this.user = dto.getSigner();
        this.anonymousSigner = dto.getAnonymousSigner();
        this.signers = dto.getSigners();
        setCMS(cms);
    }

    public CMSMessage(CMSSignedMessage cms, User user, TypeVS type) throws Exception {
        this.user = user;
        this.type = type;
        setCMS(cms);
    }

    public CMSMessage(CMSSignedMessage cms, TypeVS typeVS, CMSMessage cmsParent) throws Exception {
        this.type = typeVS;
        this.cmsParent = cmsParent;
        setCMS(cms);
    }


    public CMSMessage(CMSSignedMessage cms, TypeVS typeVS) throws Exception {
        this.type = typeVS;
        setCMS(cms);
    }

    public CMSMessage setContentPEM(byte[] content) {
        this.contentPEM = content;
        return this;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

	public CMSMessage setType(TypeVS type) {
		this.type = type;
        return this;
	}

	public TypeVS getType() {
		return type;
	}

	public CMSMessage getCMSParent() {
		return cmsParent;
	}

	public void setCMSParent(CMSMessage cmsParent) {
		this.cmsParent = cmsParent;
	}


	public String getMetaInf() { return metaInf; }

	public void setMetaInf(String metaInf) {
		this.metaInf = metaInf;
	}

	public String getBase64ContentDigest() {
		return base64ContentDigest;
	}

	public void setBase64ContentDigest(String base64ContentDigest) {
		this.base64ContentDigest = base64ContentDigest;
	}

    public CMSSignedMessage getCMS() throws Exception {
		if(cmsMessage == null && contentPEM != null) cmsMessage = CMSSignedMessage.FROM_PEM(contentPEM);
		return cmsMessage;
	}

	public CMSMessage setCMS(CMSSignedMessage cmsMessage) throws Exception {
		this.cmsMessage = cmsMessage;
        this.contentPEM = cmsMessage.toPEM();
        this.base64ContentDigest = cmsMessage.getContentDigestStr();
        this.signedContent = cmsMessage.getSignedContentStr();
        return this;
	}

    public Set<User> getSigners() {
		return signers;
	}

	public void setSigners(Set<User> signers) {
		this.signers = signers;
	}

    public User getAnonymousSigner() {
        return anonymousSigner;
    }

    public void setAnonymousSigner(User anonymousSigner) {
        this.anonymousSigner = anonymousSigner;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSignedContent() {
        return signedContent;
    }

    public Map<String, Object> getSignedContentMap() throws Exception {
        return JSON.getMapper().readValue(signedContent, new TypeReference<HashMap<String, Object>>() {});
    }

    public <T> T getSignedContent(Class<T> type) throws Exception {
        return JSON.getMapper().readValue(signedContent, type);
    }

    public CMSMessage setSignedContent(String signedContent) {
        this.signedContent = signedContent;
        return this;
    }

    @PrePersist
    public void prePersist() {
        Date date = new Date();
        setDateCreated(date);
        setLastUpdated(date);
        try {
            setCMS(cmsMessage);
        }catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @PreUpdate
    public void preUpdate() {
        setLastUpdated(new Date());
    }

}
