package org.votingsystem.model;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.SMIMEDto;
import org.votingsystem.signature.smime.SMIMEMessage;
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
@Entity @Table(name="MessageSMIME")
@NamedQueries({
        @NamedQuery(name = "findMessageSMIMEByBase64ContentDigest", query =
                "SELECT m FROM MessageSMIME m WHERE m.base64ContentDigest =:base64ContentDigest")
})
public class MessageSMIME extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(MessageSMIME.class.getName());

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="type") @Enumerated(EnumType.STRING) private TypeVS type;
    @Column(name="content") private byte[] content;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="smimeParent") private MessageSMIME smimeParent;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;
    
    @Column(name="metaInf", columnDefinition="TEXT")  private String metaInf;
    
    //To avoid repeated messages
    @Column(name="base64ContentDigest", unique=true) 
    private String base64ContentDigest;

    @Column(name="reason", columnDefinition="TEXT") private String reason;
    @Column(name="messageSubject") private String messageSubject;
    @Column(name="signedContent", columnDefinition="TEXT") private String signedContent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23, insertable=true)
    private Date lastUpdated;

    private static ThreadLocal<MessageSMIME> instance = new ThreadLocal() {
        protected MessageSMIME initialValue() {
            return null;
        }
    };

    public static MessageSMIME getCurrentMessageSMIME() {
        return (MessageSMIME)instance.get();
    }

    public static void setCurrentInstance(MessageSMIME messageSMIME) {
        if(messageSMIME == null) {
            instance.remove();
        } else {
            instance.set(messageSMIME);
        }
    }

    @Transient private transient SMIMEMessage smimeMessage;
    @Transient private transient Set<UserVS> signers;
    @Transient private transient UserVS anonymousSigner;

    public byte[] getContent() {
        return content;
    }

    public MessageSMIME() {}

    public MessageSMIME(String reason, TypeVS typeVS, String metaInf, byte[] content) {
        this.reason = reason;
        this.type = typeVS;
        this.metaInf = metaInf;
        this.content = content;
    }

    public MessageSMIME(SMIMEMessage smime, SMIMEDto smimeDto, TypeVS type) throws Exception {
        this.smimeMessage = smime;
        this.content = smimeMessage.getBytes();
        this.base64ContentDigest = smime.getContentDigestStr();
        this.type = type;
        this.userVS = smimeDto.getSigner();
        this.anonymousSigner = smimeDto.getAnonymousSigner();
        this.signers = smimeDto.getSigners();
    }

    public MessageSMIME(SMIMEMessage smime, UserVS userVS, TypeVS type) throws Exception {
        this.smimeMessage = smime;
        this.userVS = userVS;
        this.type = type;
    }

    public MessageSMIME(SMIMEMessage smime, TypeVS typeVS, MessageSMIME smimeParent) throws Exception {
        this.smimeMessage = smime;
        this.smimeParent = smimeParent;
        this.type = typeVS;
    }


    public MessageSMIME(SMIMEMessage smime, TypeVS typeVS) throws Exception {
        this.smimeMessage = smime;
        this.type = typeVS;
    }

    public MessageSMIME setContent(byte[] content) {
        this.content = content;
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

	public MessageSMIME setType(TypeVS type) {
		this.type = type;
        return this;
	}

	public TypeVS getType() {
		return type;
	}

	public MessageSMIME getSmimeParent() {
		return smimeParent;
	}

	public void setSmimeParent(MessageSMIME smimeParent) {
		this.smimeParent = smimeParent;
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

    public SMIMEMessage getSMIME() throws Exception {
		if(smimeMessage == null && content != null) {
			smimeMessage = new SMIMEMessage(content);
            smimeMessage.isValidSignature();
		}
		return smimeMessage;
	}

	public MessageSMIME setSMIME(SMIMEMessage smimeMessage) throws Exception {
		this.smimeMessage = smimeMessage;
        this.content = smimeMessage.getBytes();
        return this;
	}

    public MessageSMIME refresh() throws Exception {
        getSMIME().setMessageID("/messageSMIME/" + getId());
        setContent(getSMIME().getBytes());
        if(type != null) setType(type);
        if(reason != null) setReason(reason);
        if(metaInf != null) setMetaInf(metaInf);
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

    public String getMessageSubject() {
        return messageSubject;
    }

    public void setMessageSubject(String messageSubject) {
        this.messageSubject = messageSubject;
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

    public MessageSMIME setSignedContent(String signedContent) {
        this.signedContent = signedContent;
        return this;
    }

    @PrePersist
    public void prePersist() {
        Date date = new Date();
        setDateCreated(date);
        setLastUpdated(date);
        try {
            setSignedContent(getSMIME().getSignedContent());
            setMessageSubject(getSMIME().getSubject());
            setContent(getSMIME().getBytes());
            setBase64ContentDigest(getSMIME().getContentDigestStr());
        }catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @PreUpdate
    public void preUpdate() {
        setLastUpdated(new Date());
    }

}
