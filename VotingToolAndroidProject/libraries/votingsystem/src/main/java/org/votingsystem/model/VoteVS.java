package org.votingsystem.model;

import android.content.Context;
import android.util.Log;

import org.votingsystem.android.R;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertificationRequestVS;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VoteVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

	public static final String TAG = "VoteVS";
    
    private Long id;
    private int statusCode = ResponseVS.SC_CANCELLED;
    private String message;
    private String eventURL;
    private Long eventVSElectionId;
    private Long optionSelectedId;
    private TypeVS typeVS;
    private ActorVS actorVS;
    private String accessControlServerURL;
    private boolean isValid = false;
    private SMIMEMessageWrapper voteReceipt;
    private SMIMEMessageWrapper cancelVoteReceipt;
    private byte[] encryptedKey = null;
    private boolean isCanceled = false;
    private CertificationRequestVS certificationRequest;
    private PrivateKey certVotePrivateKey;
    private EventVS eventVS;
    private Date dateCreated;
    private Date dateUpdated;
    private FieldEventVS optionSelected;
    private String voteUUID;
    private String originHashCertVote;
    private String hashCertVoteHex;
    private String hashCertVSBase64;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;

    public VoteVS () {}

    public VoteVS (EventVS eventVS, FieldEventVS optionSelected) {
        this.eventVS = eventVS;
        this.optionSelected = optionSelected;
    }

    public void genVote() throws NoSuchAlgorithmException {
        Log.d(TAG + ".genVote()", "");
        originHashAccessRequest = UUID.randomUUID().toString();
        setHashAccessRequestBase64(CMSUtils.getHashBase64(originHashAccessRequest,
                ContextVS.VOTING_DATA_DIGEST));
        originHashCertVote = UUID.randomUUID().toString();
        hashCertVSBase64 = CMSUtils.getHashBase64(originHashCertVote, ContextVS.VOTING_DATA_DIGEST);
    }

    public HashMap getVoteDataMap() {
        Log.d(TAG + ".getVoteDataMap()", "");
        Map map = new HashMap();
        map.put("operation", TypeVS.SEND_SMIME_VOTE.toString());
        map.put("eventURL", eventVS.getURL());
        HashMap optionSelectedMap = new HashMap();
        optionSelectedMap.put("id", optionSelected.getId());
        optionSelectedMap.put("content", optionSelected.getContent());
        map.put("optionSelected", optionSelectedMap);
        map.put("UUID", UUID.randomUUID().toString());
        return new HashMap(map);
    }

    public HashMap getAccessRequestDataMap() {
        Log.d(TAG + ".getAccessRequestDataMap()", "");
        Map map = new HashMap();
        map.put("operation", TypeVS.ACCESS_REQUEST.toString());
        map.put("eventId", eventVS.getId());
        map.put("eventURL", eventVS.getURL());
        map.put("UUID", UUID.randomUUID().toString());
        map.put("hashAccessRequestBase64", getHashAccessRequestBase64());
        return new HashMap(map);
    }

    public HashMap getCancelVoteDataMap() {
        Log.d(TAG + ".getCancelVoteDataMap()", "");
        Map map = new HashMap();
        map.put("operation", TypeVS.CANCEL_VOTE.toString());
        map.put("originHashCertVote", originHashCertVote);
        map.put("hashCertVSBase64", getHashCertVSBase64());
        map.put("originHashAccessRequest", originHashAccessRequest);
        map.put("hashAccessRequestBase64", getHashAccessRequestBase64());
        map.put("UUID", UUID.randomUUID().toString());
        map.put("eventURL", eventVS.getURL());
        HashMap dataMap = new HashMap(map);
        return dataMap;
    }

    public Map getDataMap() {
        Log.d(TAG + ".getDataMap()", "");
        Map resultMap = new HashMap();
        try {
            if(optionSelected != null) {
                HashMap opcionHashMap = new HashMap();
                opcionHashMap.put("id", optionSelected.getId());
                opcionHashMap.put("content", optionSelected.getContent());
                resultMap.put("optionSelected", opcionHashMap);
            }
            if(getHashCertVSBase64() != null) {
                resultMap.put("hashCertVSBase64", getHashCertVSBase64());
                resultMap.put("hashCertVoteHex", CMSUtils.getBase64ToHexStr(getHashCertVSBase64()));
            }
            if(getHashAccessRequestBase64() != null) {
                resultMap.put("hashAccessRequestBase64", getHashAccessRequestBase64());
                resultMap.put("hashSolicitudAccesoHex", CMSUtils.getBase64ToHexStr(getHashAccessRequestBase64()));
            }

            if (eventVS != null) resultMap.put("eventId", eventVS.getId());
            if (id != null) resultMap.put("id", id);
            //map.put("UUID", UUID.randomUUID().toString());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return resultMap;
    }

    public void setOptionSelected(FieldEventVS optionSelected) {
        this.optionSelected = optionSelected;
    }

    public FieldEventVS getOptionSelected() {
        return optionSelected;
    }

    public boolean isValid () throws Exception {
        return isValid;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getEventVSElectionId() {
        return eventVSElectionId;
    }

    public void setEventVSElectionId(Long eventVSElectionId) {
        this.eventVSElectionId = eventVSElectionId;
    }

    public Long getOptionSelectedId() {
        return optionSelectedId;
    }

    public void setOptionSelectedId(Long optionSelectedId) {
        this.optionSelectedId = optionSelectedId;
    }

    public ActorVS getActorVS() {
        return actorVS;
    }

    public void setActorVS(ActorVS actorVS) {
        this.actorVS = actorVS;
    }

    public String getAccessControlServerURL() {
        return accessControlServerURL;
    }

    public void setAccessControlServerURL(String accessControlServerURL) {
        this.accessControlServerURL = accessControlServerURL;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage(Context context) {
        if (ResponseVS.SC_ERROR_REQUEST_REPEATED == statusCode) {//vote repeated
            return context.getString(R.string.vote_repeated_msg,
                    eventVS.getSubject(), optionSelected.getContent());
        }
        if (!optionSelectedId.equals(optionSelected.getId())) {
            return context.getString(R.string.option_error_msg);
        }
        if (voteReceipt.isValidSignature()) {
            return context.getString(R.string.vote_ok_msg, eventVS.getSubject(),
                    optionSelected.getContent());
        }
        return null;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEventURL() {
        return eventURL;
    }

    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    public EventVS getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

	public SMIMEMessageWrapper getCancelVoteReceipt() {
		return cancelVoteReceipt;
	}

	public void setCancelVoteReceipt(SMIMEMessageWrapper cancelVoteReceipt) {
		this.cancelVoteReceipt = cancelVoteReceipt;
		if(cancelVoteReceipt != null) isCanceled = true;
	}

    public SMIMEMessageWrapper getVoteReceipt() {
        return voteReceipt;
    }

    public void setVoteReceipt(SMIMEMessageWrapper voteReceipt) {
        this.voteReceipt = voteReceipt;
    }

	public Date getDateUpdated() {
		return dateUpdated;
	}

	public void setDateUpdated(Date dateUpdated) {
		this.dateUpdated = dateUpdated;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public boolean isCanceled() {
		return isCanceled;
	}

	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}

	public byte[] getEncryptedKey() {
		return encryptedKey;
	}

	public void setEncryptedKey(byte[] encryptedKey) {
		this.encryptedKey = encryptedKey;
	}

	public CertificationRequestVS getPkcs10WrapperClient() {
		return certificationRequest;
	}

	public void setPkcs10WrapperClient(CertificationRequestVS certificationRequest) {
		this.certificationRequest = certificationRequest;
	}

	public PrivateKey getCertVotePrivateKey() {
		return certVotePrivateKey;
	}

	public void setCertVotePrivateKey(PrivateKey certVotePrivateKey) {
		this.certVotePrivateKey = certVotePrivateKey;
	}

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
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
            if(eventMap.containsKey("eventURL")) eventVS.setURL((String) eventMap.get("eventURL"));
            if(eventMap.containsKey("hashAccessRequestBase64")) voteVS.setHashAccessRequestBase64(
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
            ex.printStackTrace();
        }
        return voteVS;
    }

    public String getHashAccessRequestBase64() {
        return hashAccessRequestBase64;
    }

    public void setHashAccessRequestBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public String getVoteUUID() {
        return voteUUID;
    }

    public void setVoteUUID(String voteUUID) {
        this.voteUUID = voteUUID;
    }
}