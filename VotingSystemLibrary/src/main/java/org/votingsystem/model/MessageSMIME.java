package org.votingsystem.model;

import org.apache.log4j.Logger;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import static javax.persistence.GenerationType.IDENTITY;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity @Table(name="MessageSMIME")
public class MessageSMIME implements Serializable {

    private static Logger logger = Logger.getLogger(MessageSMIME.class);

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="type") @Enumerated(EnumType.STRING) private TypeVS type;
    @Column(name="content") @Lob private byte[] content;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVS") private EventVS eventVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="batchRequest") private BatchRequest batchRequest;

    @OneToOne(mappedBy="messageSMIME") private AccessRequestVS accessRequestVS;
    
    @OneToOne(mappedBy="messageSMIME") private VoteVS voteVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="smimeParent") private MessageSMIME smimeParent;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;
    
    @Column(name="metaInf", columnDefinition="TEXT")  private String metaInf;
    
    //To avoid repeated messages
    @Column(name="base64ContentDigest", unique=true) 
    private String base64ContentDigest;

    @Column(name="reason")
    private String reason;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23, insertable=true)
    private Date lastUpdated;

    @Transient private transient SMIMEMessageWrapper smimeMessage;
    @Transient private transient Set<UserVS> signers;
    @Transient private transient UserVS anonymousSigner;

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
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

	public void setType(TypeVS type) {
		this.type = type;
	}

	public TypeVS getType() {
		return type;
	}

	public void setAccessRequestVS(AccessRequestVS accessRequestVS) {
		this.accessRequestVS = accessRequestVS;
	}

	public AccessRequestVS getAccessRequestVS() {
		return accessRequestVS;
	}

	public VoteVS getVoteVS() {
		return voteVS;
	}

	public void setVoteVS(VoteVS voteVS) {
		this.voteVS = voteVS;
	}

	public MessageSMIME getSmimeParent() {
		return smimeParent;
	}

	public void setSmimeParent(MessageSMIME smimeParent) {
		this.smimeParent = smimeParent;
	}

	public EventVS getEventVS() {
		return eventVS;
	}

	public void setEventVS(EventVS eventVS) {
		this.eventVS = eventVS;
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

    @Transient public SMIMEMessageWrapper getSmimeMessage() throws Exception {
		if(smimeMessage == null && content != null) {
			smimeMessage = new SMIMEMessageWrapper(
				new ByteArrayInputStream(content));
		}
		return smimeMessage;
	}

	public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) {
		this.smimeMessage = smimeMessage;
	}

    @Transient public Set<UserVS> getSigners() {
		return signers;
	}

	public void setSigners(Set<UserVS> signers) {
		this.signers = signers;
	}

    @Transient public UserVS getAnonymousSigner() {
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

    public BatchRequest getBatchRequest() {
        return batchRequest;
    }

    public void setBatchRequest(BatchRequest batchRequest) {
        this.batchRequest = batchRequest;
    }
}
