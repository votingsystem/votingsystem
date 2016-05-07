package org.votingsystem.model.voting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.KeyStore;
import org.votingsystem.model.User;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.CertUtils;

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

    private static Logger log = Logger.getLogger(EventVS.class.getName());

    private static final long serialVersionUID = 1L;

    public List<String> getTagList() {
        return tagList;
    }

    public void setTagList(List<String> tagList) {
        this.tagList = tagList;
    }

    public enum Cardinality { EXCLUSIVE, MULTIPLE }

    public enum Type {ELECTION }

    public enum State { ACTIVE, TERMINATED, CANCELED, ERROR, PENDING, DELETED_FROM_SYSTEM }


    @Id @GeneratedValue(strategy=IDENTITY) 
    @Column(name="id", unique=true, nullable=false)
    //@DocumentId
    private Long id;
    @Column(name="accessControlEventId", unique=true)
    private Long accessControlEventId;//id of the event in the Access Control
    @Column(name="content", columnDefinition="TEXT")
    //@Analyzer(definition = "customAnalyzer")
    private String content;
    @Column(name="metaInf", columnDefinition="TEXT")
    private String metaInf = "{}"; 
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="controlCenter")
    private ControlCenter controlCenter;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="accessControl")
    private AccessControl accessControl;
    @Column(name="subject", length=1000)
    //@Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)
    private String subject;
    @Enumerated(EnumType.STRING)
    @Column(name="state")
    //@Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)
    private State state;
    @Column(name="cardinality") @Enumerated(EnumType.STRING)
    private Cardinality cardinality = Cardinality.EXCLUSIVE;
    @OneToOne private Certificate certificate;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userId")
    private User user;
    @OneToOne private CMSMessage cmsMessage;
    @OneToOne private KeyStore keyStore;
    @Column(name="backupAvailable") private Boolean backupAvailable = Boolean.TRUE;
    @ElementCollection
    private List<String> tagList;
    @Column(name="url")
    private String url;
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="eventVS")
    private Set<FieldEvent> fieldsEventVS;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateBegin", nullable=false)
    //@Field(index = Index.NO, analyze=Analyze.NO, store = Store.YES)
    //@DateBridge(resolution = Resolution.HOUR)
    private Date dateBegin;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateFinish", nullable=false)
    //@Field(index = Index.NO, analyze=Analyze.NO, store = Store.YES)
    //@DateBridge(resolution = Resolution.HOUR)
    private Date dateFinish;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCanceled")
    private Date dateCanceled;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated")
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated")
    private Date lastUpdated;

    @Transient private Type type = null;
    @Transient private Integer numSignaturesCollected;
    @Transient private Integer numVotesCollected;
    @Transient private Boolean signed;
    @Transient private Vote vote;

    public EventVS() {}

    public EventVS(User user, String subject, String content, Boolean backupAvailable, Cardinality cardinality,
                   Date dateFinish) {
        this.user = user;
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

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }

    public EventVS setCmsMessage(CMSMessage publishRequestCMS) {
        this.cmsMessage = publishRequestCMS;
        return this;
    }

    public Integer getNumSignaturesCollected() {
        return numSignaturesCollected;
    }

    public void setNumSignaturesCollected(Integer numSignaturesCollected) {
        this.numSignaturesCollected = numSignaturesCollected;
    }

    public Vote getVote() {
        return vote;
    }

    public void setVote(Vote vote) {
        this.vote = vote;
    }

    public Integer getNumVotesCollected() {
        return numVotesCollected;
    }

    public void setNumVotesCollected(Integer numVotesCollected) {
        this.numVotesCollected = numVotesCollected;
    }

    public Set<FieldEvent> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public void setFieldsEventVS(Set<FieldEvent> fieldsEventVS) {
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

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
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

    public ControlCenter getControlCenter() {
        return controlCenter;
    }

    public void setControlCenter(ControlCenter controlCenter) {
        this.controlCenter = controlCenter;
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

    public AccessControl getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(AccessControl accessControl) {
        this.accessControl = accessControl;
    }

    public Long getAccessControlEventId() {
        return accessControlEventId;
    }

    public void setAccessControlEventId(Long accessControlEventId) {
        this.accessControlEventId = accessControlEventId;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public static String getURL(TypeVS type, String serverURL, Long id) {
        if(type == null) return serverURL + "/eventVS/" + id;
        switch (type) {
            case VOTING_EVENT: return serverURL + "/eventElection/" + id;
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
        if(eventVS instanceof EventElection) return Type.ELECTION;
        return null;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public EventVS setCertificate(Certificate certificate) {
        this.certificate = certificate;
        return this;
    }

    @JsonIgnore
    public Set<X509Certificate> getTrustedCerts() throws Exception {
        Set<X509Certificate> eventTrustedCerts = new HashSet<X509Certificate>();
        eventTrustedCerts.add(CertUtils.loadCertificate(certificate.getContent()));
        return eventTrustedCerts;
    }

}