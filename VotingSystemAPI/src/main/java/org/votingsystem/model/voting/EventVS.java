package org.votingsystem.model.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.KeyStoreVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.TypeVS;

import javax.persistence.*;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

//import org.apache.solr.analysis.HTMLStripCharFilterFactory;
//import org.apache.solr.analysis.StandardTokenizerFactory;
/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
//@Indexed
@Entity @Table(name="EventVS")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="eventVSType", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("EventVS")
/*@AnalyzerDef(name="eventVSAnalyzer",
	charFilters = { @CharFilterDef(factory = HTMLStripCharFilterFactory.class) }, 
	tokenizer =  @TokenizerDef(factory = StandardTokenizerFactory.class))*/
public class EventVS extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(EventVS.class.getSimpleName());

    private static final long serialVersionUID = 1L;

    public List<String> getTagList() {
        return tagList;
    }

    public void setTagList(List<String> tagList) {
        this.tagList = tagList;
    }

    public enum Cardinality { EXCLUSIVE, MULTIPLE }

    public enum Type {ELECTION }

    public enum State { ACTIVE, TERMINATED, CANCELED, ERROR, PENDING, PENDING_SIGNATURE, DELETED_FROM_SYSTEM }


    @Id @GeneratedValue(strategy=IDENTITY) 
    @Column(name="id", unique=true, nullable=false)
    //@DocumentId
    private Long id;
    @Column(name="accessControlEventVSId", unique=true)
    private Long accessControlEventVSId;//id of the event in the Access Control
    @Column(name="content", columnDefinition="TEXT")
    //@Analyzer(definition = "customAnalyzer")
    private String content;
    @Column(name="metaInf", columnDefinition="TEXT")
    private String metaInf = "{}"; 
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="controlCenter")
    private ControlCenterVS controlCenterVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="accessControl")
    private AccessControlVS accessControlVS;
    @Column(name="subject", length=1000)
    //@Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)
    private String subject;
    @Enumerated(EnumType.STRING)
    @Column(name="state")
    //@Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)
    private State state;
    @Column(name="cardinality") @Enumerated(EnumType.STRING)
    private Cardinality cardinality = Cardinality.EXCLUSIVE;
    @OneToOne private CertificateVS certificateVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS")
    private UserVS userVS;
    @OneToOne private MessageSMIME publishRequestSMIME;
    @OneToOne private KeyStoreVS keyStoreVS;
    @Column(name="backupAvailable") private Boolean backupAvailable = Boolean.TRUE;
    @ElementCollection
    private List<String> tagList;
    @Column(name="url")
    private String url;
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventVS")
    private Set<FieldEventVS> fieldsEventVS;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateBegin", length=23, nullable=false)
    //@Field(index = Index.NO, analyze=Analyze.NO, store = Store.YES)
    //@DateBridge(resolution = Resolution.HOUR)
    private Date dateBegin;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateFinish", length=23, nullable=false)
    //@Field(index = Index.NO, analyze=Analyze.NO, store = Store.YES)
    //@DateBridge(resolution = Resolution.HOUR)
    private Date dateFinish;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCanceled", length=23)
    private Date dateCanceled;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23)
    private Date lastUpdated;

    @Transient private Type type = null;
    @Transient private Integer numSignaturesCollected;
    @Transient private Integer numVotesCollected;
    @Transient private Boolean signed;
    @Transient private VoteVS voteVS;

    public EventVS() {}

    public EventVS(UserVS userVS, String subject, String content, Boolean backupAvailable, Cardinality cardinality,
                   Date dateFinish) {
        this.userVS = userVS;
        this.subject = subject;
        this.content = content;
        this.backupAvailable = backupAvailable;
        this.cardinality = cardinality;
        this.dateFinish = dateFinish;
    }

    public Type getType() {
        return type;
    }

    public EventVS setType(Type type) {
        this.type = type;
        return this;
    }

    public MessageSMIME getPublishRequestSMIME() {
        return publishRequestSMIME;
    }

    public EventVS setPublishRequestSMIME(MessageSMIME publishRequestSMIME) {
        this.publishRequestSMIME = publishRequestSMIME;
        return this;
    }

    public Integer getNumSignaturesCollected() {
        return numSignaturesCollected;
    }

    public void setNumSignaturesCollected(Integer numSignaturesCollected) {
        this.numSignaturesCollected = numSignaturesCollected;
    }

    public VoteVS getVoteVS() {
        return voteVS;
    }

    public void setVoteVS(VoteVS voteVS) {
        this.voteVS = voteVS;
    }

    public Integer getNumVotesCollected() {
        return numVotesCollected;
    }

    public void setNumVotesCollected(Integer numVotesCollected) {
        this.numVotesCollected = numVotesCollected;
    }

    public Set<FieldEventVS> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public void setFieldsEventVS(Set<FieldEventVS> fieldsEventVS) {
        this.fieldsEventVS = fieldsEventVS;
    }

    public Cardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent () {
        return content;
    }
    
    public void setContent (String content) {
        this.content = content;
    }

    public String getSubject() {
    	return subject;
    }
    
    public void setSubject (String subject) {
        this.subject = subject;
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

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public void setDateFinish(Date dateFinish) { this.dateFinish = dateFinish; }

    public State getState() {
        return state;
    }

    public EventVS setState(State state) {
        this.state = state;
        return this;
    }

    public ControlCenterVS getControlCenterVS() {
        return controlCenterVS;
    }

    public void setControlCenterVS(ControlCenterVS controlCenterVS) {
        this.controlCenterVS = controlCenterVS;
    }


	public Boolean getBackupAvailable() { return backupAvailable; }

	public void setBackupAvailable(Boolean backupAvailable) {
		this.backupAvailable = backupAvailable;
	}

	public Date getDateCanceled() {
		return dateCanceled;
	}

	public void setDateCanceled(Date dateCanceled) {
		this.dateCanceled = dateCanceled;
	}

	public String getMetaInf() {
		return metaInf;
	}

	public EventVS setMetaInf(String metaInf) {
		this.metaInf = metaInf;
        return this;
	}

    public AccessControlVS getAccessControlVS() {
        return accessControlVS;
    }

    public void setAccessControlVS(AccessControlVS accessControlVS) {
        this.accessControlVS = accessControlVS;
    }

    public Long getAccessControlEventVSId() {
        return accessControlEventVSId;
    }

    public void setAccessControlEventVSId(Long accessControlEventVSId) {
        this.accessControlEventVSId = accessControlEventVSId;
    }

    public KeyStoreVS getKeyStoreVS() {
        return keyStoreVS;
    }

    public void setKeyStoreVS(KeyStoreVS keyStoreVS) {
        this.keyStoreVS = keyStoreVS;
    }

    public static String getURL(TypeVS type, String serverURL, Long id) {
        if(type == null) return serverURL + "/eventVS/" + id;
        switch (type) {
            case VOTING_EVENT: return serverURL + "/eventVSElection/" + id;
            default: return serverURL + "/eventVS/" + id;
        }
    }

    public Date getDateFinish() {
		if(dateCanceled != null) return dateCanceled;
		else return dateFinish;
	}

    public boolean isActive(Date selectedDate) {
        if(selectedDate == null) return false;
        boolean result = false;
        if (selectedDate.after(dateBegin) && selectedDate.before(dateFinish)) result = true;
        if(state != null && (state == State.CANCELED || state == State.DELETED_FROM_SYSTEM)) result = false;
        return result;
    }

    public static Type getType(EventVS eventVS) {
        if(eventVS instanceof EventVSElection) return Type.ELECTION;
        return null;
    }

    public CertificateVS getCertificateVS() {
        return certificateVS;
    }

    public EventVS setCertificateVS(CertificateVS certificateVS) {
        this.certificateVS = certificateVS;
        return this;
    }

    @JsonIgnore
    public Set<X509Certificate> getTrustedCerts() throws Exception {
        Set<X509Certificate> eventTrustedCerts = new HashSet<X509Certificate>();
        eventTrustedCerts.add(CertUtils.loadCertificate(certificateVS.getContent()));
        return eventTrustedCerts;
    }

}