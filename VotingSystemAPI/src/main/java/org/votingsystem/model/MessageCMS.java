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
@Entity @Table(name="MessageCMS")
@NamedQueries({
        @NamedQuery(name = "findMessageCMSByBase64ContentDigest", query =
                "SELECT m FROM MessageCMS m WHERE m.base64ContentDigest =:base64ContentDigest")
})
public class MessageCMS extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(MessageCMS.class.getName());

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="type") @Enumerated(EnumType.STRING) private TypeVS type;
    @Column(name="content") private byte[] contentPEM;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="cmsParent") private MessageCMS cmsParent;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;
    
    @Column(name="metaInf", columnDefinition="TEXT")  private String metaInf;
    
    //To avoid repeated messages
    @Column(name="base64ContentDigest", unique=true) 
    private String base64ContentDigest;

    @Column(name="reason", columnDefinition="TEXT") private String reason;
    @Column(name="signedContent", columnDefinition="TEXT") private String signedContent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23, insertable=true)
    private Date lastUpdated;

    private static ThreadLocal<MessageCMS> instance = new ThreadLocal() {
        protected MessageCMS initialValue() {
            return null;
        }
    };

    public static MessageCMS getCurrentMessageCMS() {
        return (MessageCMS)instance.get();
    }

    public static void setCurrentInstance(MessageCMS messageCMS) {
        if(messageCMS == null) {
            instance.remove();
        } else {
            instance.set(messageCMS);
        }
    }

    @Transient private transient CMSSignedMessage cmsMessage;
    @Transient private transient Set<UserVS> signers;
    @Transient private transient UserVS anonymousSigner;

    public byte[] getContentPEM() {
        return contentPEM;
    }

    public MessageCMS() {}

    public MessageCMS(String reason, TypeVS typeVS, String metaInf, byte[] contentPEM) throws Exception {
        this.reason = reason;
        this.type = typeVS;
        this.metaInf = metaInf;
        setCMS(CMSSignedMessage.FROM_PEM(contentPEM));
    }

    public MessageCMS(CMSSignedMessage cms, CMSDto dto, TypeVS type) throws Exception {
        this.type = type;
        this.userVS = dto.getSigner();
        this.anonymousSigner = dto.getAnonymousSigner();
        this.signers = dto.getSigners();
        setCMS(cms);
    }

    public MessageCMS(CMSSignedMessage cms, UserVS userVS, TypeVS type) throws Exception {
        this.userVS = userVS;
        this.type = type;
        setCMS(cms);
    }

    public MessageCMS(CMSSignedMessage cms, TypeVS typeVS, MessageCMS cmsParent) throws Exception {
        this.type = typeVS;
        this.cmsParent = cmsParent;
        setCMS(cms);
    }


    public MessageCMS(CMSSignedMessage cms, TypeVS typeVS) throws Exception {
        this.type = typeVS;
        setCMS(cms);
    }

    public MessageCMS setContentPEM(byte[] content) {
        this.contentPEM = content;
        return this;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
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

	public MessageCMS setType(TypeVS type) {
		this.type = type;
        return this;
	}

	public TypeVS getType() {
		return type;
	}

	public MessageCMS getCMSParent() {
		return cmsParent;
	}

	public void setCMSParent(MessageCMS cmsParent) {
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

	public MessageCMS setCMS(CMSSignedMessage cmsMessage) throws Exception {
		this.cmsMessage = cmsMessage;
        this.contentPEM = cmsMessage.toPEM();
        this.base64ContentDigest = cmsMessage.getContentDigestStr();
        this.signedContent = cmsMessage.getSignedContentStr();
        return this;
	}

    public Set<UserVS> getSigners() {
		return signers;
	}

	public void setSigners(Set<UserVS> signers) {
		this.signers = signers;
	}

    public UserVS getAnonymousSigner() {
        return anonymousSigner;
    }

    public void setAnonymousSigner(UserVS anonymousSigner) {
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

    public MessageCMS setSignedContent(String signedContent) {
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
