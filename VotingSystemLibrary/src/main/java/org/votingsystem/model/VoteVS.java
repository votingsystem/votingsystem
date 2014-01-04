package org.votingsystem.model;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CMSUtils;

import javax.persistence.*;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="VoteVS")
public class VoteVS implements Serializable {

    private static Logger logger = Logger.getLogger(VoteVS.class);

    public enum State{OK, CANCELLED, ERROR}

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne private MessageSMIME messageSMIME;
    @OneToOne private CertificateVS certificateVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="optionSelected") private FieldEventVS optionSelected;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVSElection") private EventVS eventVS;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @Column(name="dateCreated", length=23) @Temporal(TemporalType.TIMESTAMP) private Date dateCreated;

    @Transient private String voteUUID;
    @Transient private String originHashCertVote;
    @Transient private String hashCertVoteHex;
    @Transient private String hashCertVSBase64;
    @Transient private String originHashAccessRequest;
    @Transient private String hashAccessRequestBase64;
    @Transient private String accessControlURL;
    @Transient private String representativeURL;
    @Transient private X509Certificate x509Certificate;
    @Transient private TimeStampToken timeStampToken;
    @Transient private Set<X509Certificate> serverCerts = new HashSet<X509Certificate>();
    @Transient private SMIMEMessageWrapper receipt;
    @Transient private boolean isValid = false;

    public VoteVS () {}

    public VoteVS (EventVS eventVS) {
        this.eventVS = eventVS;
    }

    private VoteVS (X509Certificate x509Certificate, TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
        this.x509Certificate = x509Certificate;
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

    public String getAccessRequestHashBase64() {
        return hashAccessRequestBase64;
    }

    public void setAccessRequestHashBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
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

	public void setState(State state) {
		this.state = state;
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

    public SMIMEMessageWrapper getReceipt() { return receipt; }

    public void setReceipt(SMIMEMessageWrapper receipt) { this.receipt = receipt; }

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

    @Transient public void genVote() throws NoSuchAlgorithmException {
        logger.debug(" --- genVote ---");
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

    public static VoteVS getInstance(Map contentMap, X509Certificate x509Certificate, TimeStampToken timeStampToken)
            throws IOException {
        VoteVS voteVS = VoteVS.populate(contentMap);
        voteVS.setTimeStampToken(timeStampToken);
        voteVS.setX509Certificate(x509Certificate);
        byte[] voteExtensionValue = x509Certificate.getExtensionValue(ContextVS.VOTE_OID);
        if(voteExtensionValue != null) {
            DERTaggedObject voteCertDataDER = (DERTaggedObject) X509ExtensionUtil.fromExtensionValue(voteExtensionValue);
            JSONObject voteCertData = (JSONObject) JSONSerializer.toJSON(
                    ((DERUTF8String) voteCertDataDER.getObject()).toString());
            EventVS eventVS = new EventVS();
            eventVS.setId(Long.valueOf(voteCertData.getString("eventId")));
            voteVS.setEventVS(eventVS);
            voteVS.setAccessControlURL(voteCertData.getString("accessControlURL"));
            voteVS.setHashCertVSBase64(voteCertData.getString("hashCertVS"));
        }
        byte[] representativeURLExtensionValue = x509Certificate.getExtensionValue(ContextVS.REPRESENTATIVE_VOTE_OID);
        if(representativeURLExtensionValue != null) {
            DERTaggedObject representativeURL_DER = (DERTaggedObject)X509ExtensionUtil.fromExtensionValue(
                    representativeURLExtensionValue);
            voteVS.setRepresentativeURL(((DERUTF8String)representativeURL_DER.getObject()).toString());
        }
        return voteVS;
    }

    @Transient public HashMap getVoteDataMap() {
        logger.debug("getVoteDataMap");
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

    @Transient public HashMap getAccessRequestDataMap() {
        logger.debug("getAccessRequestDataMap");
        Map map = new HashMap();
        map.put("operation", TypeVS.ACCESS_REQUEST.toString());
        map.put("eventId", eventVS.getId());
        map.put("eventURL", eventVS.getUrl());
        map.put("UUID", UUID.randomUUID().toString());
        map.put("hashAccessRequestBase64", hashAccessRequestBase64);
        return new HashMap(map);
    }

    @Transient public HashMap getCancelVoteDataMap() {
        logger.debug("getCancelVoteDataMap");
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

    @Transient public Map getDataMap() {
        logger.debug("getDataMap");
        Map resultMap = new HashMap();
        if(optionSelected != null) {
            HashMap opcionHashMap = new HashMap();
            opcionHashMap.put("id", optionSelected.getId());
            opcionHashMap.put("content", optionSelected.getContent());
            resultMap.put("optionSelected", opcionHashMap);
        }
        if(hashCertVSBase64 != null) {
            resultMap.put("hashCertVSBase64", hashCertVSBase64);
            resultMap.put("hashCertVoteHex", CMSUtils.getBase64ToHexStr(hashCertVSBase64));
        }
        if(hashAccessRequestBase64 != null) {
            resultMap.put("hashAccessRequestBase64", hashAccessRequestBase64);
            resultMap.put("hashSolicitudAccesoHex", CMSUtils.getBase64ToHexStr(hashAccessRequestBase64));
        }

        if (eventVS != null) resultMap.put("eventId", eventVS.getId());
        if (id != null) resultMap.put("id", id);
        //map.put("UUID", UUID.randomUUID().toString());
        return resultMap;
    }

    public static VoteVS populate (Map eventMap) {
        VoteVS voteVS = null;
        try {
            voteVS = new VoteVS();
            EventVS eventVS = new EventVS();
            if(eventMap.containsKey("eventId")) {
                eventVS.setId(((Integer) eventMap.get("eventId")).longValue());
            }
            if(eventMap.containsKey("UUID")) {
                voteVS.setVoteUUID((String) eventMap.get("UUID"));
            }
            if(eventMap.containsKey("eventURL")) eventVS.setUrl((String) eventMap.get("eventURL"));
            if(eventMap.containsKey("hashAccessRequestBase64")) voteVS.setAccessRequestHashBase64(
                    (String) eventMap.get("hashAccessRequestBase64"));
            if(eventMap.containsKey("optionSelectedId")) {
                FieldEventVS optionSelected = new FieldEventVS();
                optionSelected.setId(((Integer) eventMap.get("optionSelectedId")).longValue());
                if(eventMap.containsKey("optionSelectedContent")) {
                    optionSelected.setContent((String) eventMap.get("optionSelectedContent"));
                }
                voteVS.setOptionSelected(optionSelected);
            }
            if(eventMap.containsKey("optionSelected")) {
                voteVS.setOptionSelected(FieldEventVS.populate((Map) eventMap.get("optionSelected")));
            }
            voteVS.setEventVS(eventVS);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return voteVS;
    }

}