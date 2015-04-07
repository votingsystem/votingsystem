package org.votingsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;
import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;
import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="VoteVS")
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoteVS extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(VoteVS.class.getSimpleName());

    public String getEventURL() {
        return eventURL;
    }

    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    public enum State{OK, CANCELED, ERROR}

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne @JsonIgnore private MessageSMIME messageSMIME;
    @OneToOne @JsonIgnore private CertificateVS certificateVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="optionSelected") private FieldEventVS optionSelected;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVSElection") private @JsonIgnore EventVS eventVS;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @Column(name="dateCreated", length=23) @Temporal(TemporalType.TIMESTAMP) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @JsonProperty("UUID")
    @Transient private String voteUUID;
    @Transient private String originHashCertVote;
    @Transient private String hashCertVoteHex;
    @Transient private String hashCertVSBase64;
    @Transient private String originHashAccessRequest;
    @Transient private String hashAccessRequestBase64;
    @Transient private String accessControlURL;
    @Transient private String eventURL;
    @Transient private String representativeURL;
    @Transient @JsonIgnore private X509Certificate x509Certificate;
    @Transient @JsonIgnore private TimeStampToken timeStampToken;
    @Transient @JsonIgnore private Set<X509Certificate> serverCerts = new HashSet<X509Certificate>();
    @Transient @JsonIgnore private SMIMEMessage receipt;
    @Transient private boolean isValid = false;

    public VoteVS () {}

    public VoteVS (EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public VoteVS (X509Certificate x509Certificate, TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
        this.x509Certificate = x509Certificate;
    }

    public VoteVS (FieldEventVS optionSelected, EventVS eventVS, State state, CertificateVS certificateVS,
                    MessageSMIME messageSMIME) {
        this.optionSelected = optionSelected;
        this.eventVS = eventVS;
        this.state = state;
        this.certificateVS = certificateVS;
        this.messageSMIME = messageSMIME;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public FieldEventVS getOptionSelected() {
        return optionSelected;
    }

    public void setOptionSelected(FieldEventVS optionSelected) {
        this.optionSelected = optionSelected;
    }

    public String getOriginHashCertVote() {
        return originHashCertVote;
    }

    public void setOriginHashCertVote(String originHashCertVote) {
        this.originHashCertVote = originHashCertVote;
    }

    public String getOriginHashAccessRequest() {
        return originHashAccessRequest;
    }

    public void setOriginHashAccessRequest(String originHashAccessRequest) {
        this.originHashAccessRequest = originHashAccessRequest;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setEventVS(EventVS eventVS) {
		this.eventVS = eventVS;
	}

	public EventVS getEventVS() {
		return eventVS;
	}

	public void setFieldEventVS(FieldEventVS optionSelected) {
		this.setOptionSelected(optionSelected);
	}

	public FieldEventVS getFieldEventVS() {
		return getOptionSelected();
	}

	public MessageSMIME getMessageSMIME() {
		return messageSMIME;
	}

	public void setMessageSMIME(MessageSMIME messageSMIME) {
		this.messageSMIME = messageSMIME;
	}

	public State getState() {
		return state;
	}

	public VoteVS setState(State state) {
		this.state = state;
        return this;
	}

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

	public CertificateVS getCertificateVS() {
		return certificateVS;
	}

	public void setCertificateVS(CertificateVS certificateVS) {
		this.certificateVS = certificateVS;
	}

    public void setHashCertVoteHex(String hashCertVoteHex) {
        this.hashCertVoteHex = hashCertVoteHex;
    }

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public Set<X509Certificate> getServerCerts() {
        return serverCerts;
    }

    public boolean isValid() { return isValid; }

    public void setValid(boolean isValid) { this.isValid = isValid; }

    public SMIMEMessage getReceipt() { return receipt; }

    public void setReceipt(SMIMEMessage receipt) { this.receipt = receipt; }

    public void setServerCerts(Set<X509Certificate> serverCerts) { this.serverCerts = serverCerts; }

    public X509Certificate getX509Certificate() { return x509Certificate; }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    public String getRepresentativeURL() {
        return representativeURL;
    }

    public void setRepresentativeURL(String representativeURL) {
        this.representativeURL = representativeURL;
    }

    public String getVoteUUID() {
        return voteUUID;
    }

    public void setVoteUUID(String voteUUID) {
        this.voteUUID = voteUUID;
    }

    private void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    private void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }

    public void genVote() throws NoSuchAlgorithmException {
        log.info(" --- genVote ---");
        originHashAccessRequest = UUID.randomUUID().toString();
        hashAccessRequestBase64 = CMSUtils.getHashBase64(originHashAccessRequest, ContextVS.VOTING_DATA_DIGEST);
        originHashCertVote = UUID.randomUUID().toString();
        hashCertVSBase64 = CMSUtils.getHashBase64(originHashCertVote, ContextVS.VOTING_DATA_DIGEST);
    }

    public static VoteVS genRandomVote (String digestAlg, EventVS eventVS)throws NoSuchAlgorithmException {
        VoteVS voteVS = new VoteVS(eventVS);
        voteVS.setEventVS(eventVS);
        voteVS.genVote();
        voteVS.setOptionSelected(getRandomOption(eventVS.getFieldsEventVS()));
        return voteVS;
    }

    private static FieldEventVS getRandomOption (Set<FieldEventVS> options) {
        int item = new Random().nextInt(options.size()); // In real life, the Random object should be rather more shared than this
        return (FieldEventVS) options.toArray()[item];
    }

    public void loadSignatureData(X509Certificate x509Certificate, TimeStampToken timeStampToken) throws IOException {
        this.timeStampToken = timeStampToken;
        this.x509Certificate = x509Certificate;
        byte[] voteExtensionValue = x509Certificate.getExtensionValue(ContextVS.VOTE_OID);
        if(voteExtensionValue != null) {
            DERTaggedObject voteCertDataDER = (DERTaggedObject) X509ExtensionUtil.fromExtensionValue(voteExtensionValue);
            Map<String,String> voteCertData = new ObjectMapper().readValue(((DERUTF8String) voteCertDataDER.getObject()).toString(),
                    new TypeReference<HashMap<String, String>>() {});
            EventVS eventVS = new EventVS();
            eventVS.setId(Long.valueOf(voteCertData.get("eventId")));
            eventVS.setUrl(eventURL);
            setEventVS(eventVS);
            setAccessControlURL(voteCertData.get("accessControlURL"));
            setHashCertVSBase64(voteCertData.get("hashCertVS"));
        }
        byte[] representativeURLExtensionValue = x509Certificate.getExtensionValue(ContextVS.REPRESENTATIVE_VOTE_OID);
        if(representativeURLExtensionValue != null) {
            DERTaggedObject representativeURL_DER = (DERTaggedObject)X509ExtensionUtil.fromExtensionValue(
                    representativeURLExtensionValue);
            setRepresentativeURL(((DERUTF8String) representativeURL_DER.getObject()).toString());
        }
    }

    @JsonIgnore
    public HashMap getVoteDataMap() {
        log.info("getVoteDataMap");
        Map map = new HashMap();
        map.put("operation", TypeVS.SEND_SMIME_VOTE.toString());
        map.put("eventURL", eventVS.getUrl());
        HashMap optionSelectedMap = new HashMap();
        optionSelectedMap.put("id", optionSelected.getId());
        optionSelectedMap.put("content", optionSelected.getContent());
        map.put("optionSelected", optionSelectedMap);
        map.put("UUID", UUID.randomUUID().toString());
        return new HashMap(map);
    }

    @JsonIgnore
    public HashMap getAccessRequestDataMap() {
        log.info("getAccessRequestDataMap");
        Map map = new HashMap();
        map.put("operation", TypeVS.ACCESS_REQUEST.toString());
        map.put("eventId", eventVS.getId());
        map.put("eventURL", eventVS.getUrl());
        map.put("UUID", UUID.randomUUID().toString());
        map.put("hashAccessRequestBase64", hashAccessRequestBase64);
        return new HashMap(map);
    }

    @JsonIgnore
    public HashMap getCancelVoteDataMap() {
        log.info("getCancelVoteDataMap");
        Map map = new HashMap();
        map.put("operation", TypeVS.CANCEL_VOTE.toString());
        map.put("originHashCertVote", originHashCertVote);
        map.put("hashCertVSBase64", hashCertVSBase64);
        map.put("originHashAccessRequest", originHashAccessRequest);
        map.put("hashAccessRequestBase64", hashAccessRequestBase64);
        map.put("UUID", UUID.randomUUID().toString());
        HashMap dataMap = new HashMap(map);
        return dataMap;
    }

    @JsonIgnore
    public Map getDataMap() {
        log.info("getDataMap");
        Map resultMap = new HashMap();
        if(optionSelected != null) {
            HashMap opcionHashMap = new HashMap();
            opcionHashMap.put("id", optionSelected.getId());
            opcionHashMap.put("content", optionSelected.getContent());
            resultMap.put("optionSelected", opcionHashMap);
        }
        if(hashCertVSBase64 != null) {
            resultMap.put("hashCertVSBase64", hashCertVSBase64);
            resultMap.put("hashCertVoteHex", StringUtils.toHex(hashCertVSBase64));
        }
        if(hashAccessRequestBase64 != null) {
            resultMap.put("hashAccessRequestBase64", hashAccessRequestBase64);
            resultMap.put("hashSolicitudAccesoHex", StringUtils.toHex(hashAccessRequestBase64));
        }

        if (eventVS != null) resultMap.put("eventId", eventVS.getId());
        if (id != null) resultMap.put("id", id);
        //map.put("UUID", UUID.randomUUID().toString());
        return resultMap;
    }

}