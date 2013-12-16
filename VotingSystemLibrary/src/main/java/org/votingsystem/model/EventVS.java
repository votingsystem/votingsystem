package org.votingsystem.model;

import org.apache.log4j.Logger;
import org.apache.solr.analysis.HTMLStripCharFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.votingsystem.util.DateUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;
import org.hibernate.search.annotations.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Indexed
@Entity @Table(name="EventVS")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="eventVSType", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("EventVS")
@AnalyzerDef(name="customAnalyzer",
	charFilters = { @CharFilterDef(factory = HTMLStripCharFilterFactory.class) }, 
	tokenizer =  @TokenizerDef(factory = StandardTokenizerFactory.class))
public class EventVS implements Serializable {

    private static Logger logger = Logger.getLogger(EventVS.class);

    private static final long serialVersionUID = 1L;

    public enum Cardinality { EXCLUSIVE, MULTIPLE }

    public enum Type { CLAIM, MANIFEST, ELECTION }

    public enum State { ACTIVE, TERMINATED, CANCELLED, ERROR, AWAITING, PENDING_SIGNATURE, DELETED_FROM_SYSTEM }


    @Id @GeneratedValue(strategy=IDENTITY) 
    @Column(name="id", unique=true, nullable=false)
    @DocumentId
    private Long id;
    @Column(name="accessControlEventVSId", unique=true)
    private Long accessControlEventVSId;//id of the event in the Access Control
    @Column(name="content", columnDefinition="TEXT") @Analyzer(definition = "customAnalyzer") private String content;
    @Column(name="metaInf", columnDefinition="TEXT")
    private String metaInf = "{}"; 
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="controlCenter")
    private ControlCenterVS controlCenterVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="accessControl")
    private AccessControlVS accessControlVS;
    @Column(name="subject", length=1000)
    @Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)    
    private String subject;
    @Enumerated(EnumType.STRING)
    @Column(name="state")
    @Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)    
    private State state;
    @Column(name="cardinality") @Enumerated(EnumType.STRING)
    private Cardinality cardinality = Cardinality.EXCLUSIVE;
    @OneToOne(mappedBy="eventVS")
    private KeyStoreVS keyStoreVS;
    @Lob @Column(name="pdf")
    private byte[] pdf;    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS")
    private UserVS userVS;
    @Column(name="backupAvailable") private Boolean backupAvailable = Boolean.TRUE;
    //Owning Entity side of the relationship
    @JoinTable(name="TagVSEventVS",
        joinColumns = { @JoinColumn(name = "event", referencedColumnName = "id")},
     	inverseJoinColumns = {@JoinColumn(name = "tag", referencedColumnName = "id")})
    @ManyToMany
    private Set<TagVS> tagVSSet;
    @Column(name="url")
    private String url;
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="eventVS")
    private Set<FieldEventVS> fieldsEventVS;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateBegin", length=23, nullable=false)
    @Field(index = Index.NO, analyze=Analyze.NO, store = Store.YES)
    @DateBridge(resolution = Resolution.HOUR)
    private Date dateBegin;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateFinish", length=23, nullable=false)
    @Field(index = Index.NO, analyze=Analyze.NO, store = Store.YES)
    @DateBridge(resolution = Resolution.HOUR)
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
    @Transient private String[] tags;
    @Transient private String dateBeginStr;
    @Transient private String dateFinishStr;
    @Transient private Integer numSignaturesCollected;
    @Transient private Integer numVotesCollected;
    @Transient private Boolean signed;
    @Transient private VoteVS voteVS;

    @Transient public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Transient public Integer getNumSignaturesCollected() {
        return numSignaturesCollected;
    }

    public void setNumSignaturesCollected(Integer numSignaturesCollected) {
        this.numSignaturesCollected = numSignaturesCollected;
    }

    @Transient public VoteVS getVoteVS() {
        return voteVS;
    }

    public void setVoteVS(VoteVS voteVS) {
        this.voteVS = voteVS;
    }

    @Transient public Integer getNumVotesCollected() {
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

    public void setTags(String[] tags) {
        this.tags = tags;
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

    public void setState(State state) {
        this.state = state;
    }

    public ControlCenterVS getControlCenterVS() {
        return controlCenterVS;
    }

    public void setControlCenterVS(ControlCenterVS controlCenterVS) {
        this.controlCenterVS = controlCenterVS;
    }

	public KeyStoreVS getKeyStoreVS() {
		return keyStoreVS;
	}

	public void setKeyStoreVS(KeyStoreVS keyStoreVS) {
		this.keyStoreVS = keyStoreVS;
	}

	public byte[] getPdf() {
		return pdf;
	}

	public void setPdf(byte[] pdf) {
		this.pdf = pdf;
	}

	public Boolean getBackupAvailable() { return backupAvailable; }

	public void setBackupAvailable(Boolean backupAvailable) {
		this.backupAvailable = backupAvailable;
	}

	public Set<TagVS> getTagVSSet() {
		return tagVSSet;
	}

	public void setTagVSSet(Set<TagVS> tagVSSet) {
		this.tagVSSet = tagVSSet;
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

	public void setMetaInf(String metaInf) {
		this.metaInf = metaInf;
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

    public static String getURL(TypeVS type, String serverURL, Long id) {
        if(type == null) return serverURL + "/eventVS/" + id;
        switch (type) {
            case MANIFEST_EVENT: return serverURL + "/eventVSManifest/" + id;
            case CLAIM_EVENT: return serverURL + "/eventVSClaim/" + id;
            case VOTING_EVENT: return serverURL + "/eventVSElection/" + id;
            default: return serverURL + "/eventVS/" + id;
        }
    }

    @Transient public String[] getTags() {
        return tags;
    }

    @Transient public Date getDateFinish() {
		if(dateCanceled != null) return dateCanceled;
		else return dateFinish;
	}

    @Transient public boolean isActive(Date selectedDate) {
        if(selectedDate == null) return false;
        boolean result = false;
        if (selectedDate.after(dateBegin) && selectedDate.before(dateFinish)) result = true;
        if(state != null && (state == State.CANCELLED || state == State.DELETED_FROM_SYSTEM)) result = false;
        return result;
    }

    @Transient public String getDateBeginStr() {
        return dateBeginStr;
    }

    public void setDateBeginStr(String dateBeginStr) throws ParseException {
        this.dateBeginStr = dateBeginStr;
        this.dateBegin = DateUtils.getDateFromString(dateBeginStr);
    }

    @Transient public String getDateFinishStr() {
        return dateFinishStr;
    }

    public void setDateFinishStr(String dateFinishStr) throws ParseException {
        this.dateFinishStr = dateFinishStr;
        this.dateFinish = DateUtils.getDateFromString(dateFinishStr);
    }

    @Transient public Map getDataMap() {
        logger.debug("getDataMap");
        Map map = new HashMap();
        map.put("subject", subject);
        map.put("content", content);
        map.put("dateBegin", DateUtils.getStringFromDate(dateBegin));
        map.put("dateFinish", DateUtils.getStringFromDate(dateFinish));
        if(url != null) map.put("url", url);
        map.put("backupAvailable", backupAvailable);
        if(userVS != null) {
            Map userVSHashMap = new HashMap();
            userVSHashMap.put("nif", userVS.getNif());
            userVSHashMap.put("email", userVS.getEmail());
            userVSHashMap.put("name", userVS.getName());
            map.put("userVS", userVSHashMap);
        }
        if(voteVS != null) map.put("voteVS", voteVS.getDataMap());
        if(accessControlEventVSId != null) map.put("accessControlEventVSId", accessControlEventVSId);

        if (type != null) map.put("type", type.toString());
        if (getAccessControlEventVSId() != null) map.put("accessControlEventVSId", getAccessControlEventVSId());
        if (id != null) map.put("id", id);
        //map.put("UUID", UUID.randomUUID().toString());
        HashMap resultMap = new HashMap(map);
        if (tagVSSet != null && !tagVSSet.isEmpty()) {
            List<String> tagList = new ArrayList<String>();
            for (TagVS tag : tagVSSet) {
                tagList.add(tag.getName());
            }
            resultMap.put("tags", tagList);
        }
        if (controlCenterVS != null) {
            Map controlCenterMap = new HashMap();
            controlCenterMap.put("id", controlCenterVS.getId());
            controlCenterMap.put("name", controlCenterVS.getName());
            controlCenterMap.put("serverURL", controlCenterVS.getServerURL());
            resultMap.put("controlCenter", controlCenterMap);
        }
        if (getFieldsEventVS() != null && !getFieldsEventVS().isEmpty()) {
            List<Map> fieldList = new ArrayList<Map>();
            for (FieldEventVS opcion : getFieldsEventVS()) {
                Map field = new HashMap();
                field.put("content", opcion.getContent());
                field.put("value", opcion.getValue());
                field.put("id", opcion.getId());
                fieldList.add(field);
            }
            resultMap.put("fieldsEventVS", fieldList);
        }
        if (cardinality != null) map.put("cardinality", cardinality.toString());
        return resultMap;
    }

    public static Type getType(EventVS eventVS) {
        if(eventVS instanceof EventVSClaim) return Type.CLAIM;
        if(eventVS instanceof EventVSElection) return Type.ELECTION;
        if(eventVS instanceof EventVSManifest) return Type.MANIFEST;
        return null;
    }

    @Transient public HashMap getChangeEventDataMap(String serverURL, State state) {
        logger.debug("getCancelEventDataMap");
        Map map = new HashMap();
        map.put("operation", TypeVS.EVENT_CANCELLATION.toString());
        map.put("accessControlURL", serverURL);
        if(getAccessControlEventVSId() != null) map.put("eventId", getAccessControlEventVSId());
        else map.put("eventId", id);
        map.put("state", state.toString());
        map.put("UUID", UUID.randomUUID().toString());
        HashMap jsonObject = new HashMap(map);
        return jsonObject;
    }

    public static EventVS populate (Map eventMap) {
        EventVS eventVS = null;
        try {
            eventVS = new EventVS();
            if(eventMap.get("id") != null &&
                    !"null".equals(eventMap.get("id").toString())) {
                eventVS.setId(((Integer) eventMap.get("id")).longValue());
            }
            if(eventMap.containsKey("accessControlEventVSId")) eventVS.setAccessControlEventVSId(
                    ((Integer) eventMap.get("accessControlEventVSId")).longValue());
            if(eventMap.containsKey("URL")) eventVS.setUrl((String) eventMap.get("URL"));
            if(eventMap.containsKey("controlCenter")) {
                Map controlCenterMap = (Map) eventMap.get("controlCenter");
                controlCenterMap.put("serverType", ActorVS.Type.CONTROL_CENTER);
                eventVS.setControlCenterVS((ControlCenterVS) ActorVS.populate(controlCenterMap));
            }
            if(eventMap.containsKey("accessControl")) {
                Map accessControlMap = (Map) eventMap.get("accessControl");
                accessControlMap.put("serverType", ActorVS.Type.ACCESS_CONTROL);
                eventVS.setAccessControlVS((AccessControlVS) ActorVS.populate(accessControlMap));
            }
            if(eventMap.containsKey("subject")) eventVS.setSubject((String) eventMap.get("subject"));
            if(eventMap.containsKey("voteVS")) {
                VoteVS voteVS = VoteVS.populate((Map) eventMap.get("voteVS"));
                eventVS.setVoteVS(voteVS);
            }
            if(eventMap.containsKey("state")) {
                State state = State.valueOf((String) eventMap.get("state"));
                eventVS.setState(state);
            }
            if(eventMap.containsKey("content")) eventVS.setContent((String) eventMap.get("content"));
            if(eventMap.containsKey("dateBegin")) eventVS.setDateBeginStr((String) eventMap.get("dateBegin"));
            if(eventMap.containsKey("dateFinish")) eventVS.setDateFinishStr((String) eventMap.get("dateFinish"));
            if (eventMap.containsKey("numSignaturesCollected"))
                eventVS.setNumSignaturesCollected((Integer) eventMap.get("numSignaturesCollected"));
            if (eventMap.containsKey("numVotesCollected"))eventVS.setNumVotesCollected((Integer) eventMap.get("numVotesCollected"));
            if(eventMap.containsKey("cardinality")) {
                eventVS.setCardinality(Cardinality.valueOf((String) eventMap.get("cardinality")));
            }
            if (eventMap.containsKey("userVS")) {
                Object userVSData = eventMap.get("userVS");
                if(userVSData instanceof String) {
                    UserVS userVS = new UserVS();
                    userVS.setName((String) eventMap.get("userVS"));
                } else eventVS.setUserVS(UserVS.populate((Map) userVSData));
            }
            if (eventMap.containsKey("backupAvailable")) {
                eventVS.setBackupAvailable((Boolean) eventMap.get("backupAvailable"));
            }
            if(eventMap.containsKey("fieldsEventVS")) {
                Set<FieldEventVS> fieldsEventVS =  new HashSet<FieldEventVS>();
                for(Map option : (List<Map>) eventMap.get("fieldsEventVS")) {
                    fieldsEventVS.add(FieldEventVS.populate(option));
                }
                eventVS.setFieldsEventVS(fieldsEventVS);
            }
            if(eventMap.get("tags") != null && !"null".equals(eventMap.get("tags").toString())) {
                List<String> labelList = (List<String>)eventMap.get("tags");
                eventVS.setTags(labelList.toArray(new String[labelList.size()]));
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return eventVS;
    }

}