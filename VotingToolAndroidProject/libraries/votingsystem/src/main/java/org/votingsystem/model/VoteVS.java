package org.votingsystem.model;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertificationRequestVS;

import java.security.PrivateKey;
import java.util.Date;
import java.util.Random;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VoteVS {

	public static final String TAG = "VoteVS";
    
    private int id;
    private int notificationId;
    private int statusCode = 0;
    private String message;
    private String eventURL;
    private Long eventVSElectionId;
    private Long optionSelectedId;
    private TypeVS typeVS;
    private ActorVS actorVS;
    private String accessControlServerURL;
    private boolean isValid = false;
    private SMIMEMessageWrapper smimeMessage;
    private SMIMEMessageWrapper cancelVoteReceipt;
    private byte[] encryptedKey = null;
    private boolean isCanceled = false;
    private CertificationRequestVS certificationRequest;
    private PrivateKey certVotePrivateKey;
    private EventVS eventVS;
    private Date dateCreated;
    private Date dateUpdated;
    
    public int initNotificationId() {
        Random randomGenerator = new Random();
        this.notificationId = randomGenerator.nextInt(100);
        return notificationId;
    }
    
    public int getNotificationId() {
        return notificationId;
    }
    
    public VoteVS(int statusCode,SMIMEMessageWrapper voteReceipt, EventVS eventVS)throws Exception {
        this.smimeMessage = voteReceipt;
        this.statusCode = statusCode;
        this.eventVS = eventVS;
        String receiptContent = voteReceipt.getSignedContent();
        JSONObject receiptContentJSON = new JSONObject(receiptContent);
        this.optionSelectedId = receiptContentJSON.getLong("optionSelectedId");
        this.eventURL = receiptContentJSON.getString("eventURL");
        if (smimeMessage.isValidSignature()) {
            isValid = true;
        }
        if (ResponseVS.SC_ERROR_REQUEST_REPEATED == statusCode) {//vote repeated
            isValid = false;
        }
        if (!optionSelectedId.equals(eventVS.getOptionSelected().getId())) {
            isValid = false;
        }
    }
    
    public VoteVS(int statusCode, EventVS eventVS) throws Exception {
        this.statusCode = statusCode;
        this.eventVS = eventVS;
    }
    
    public JSONObject toJSON() throws JSONException {
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("statusCode", statusCode);
        if(eventVS != null) jsonObject.put("vote", eventVS.toJSON());
        jsonObject.put("isCanceled", isCanceled);
        return jsonObject;
    }
    
    public static VoteVS parse(String jsonVoteReceipt) throws Exception {
    	if(jsonVoteReceipt == null) return null;
    	int statusCode = 0;
    	boolean isCanceled = false;
    	EventVS eventVS = null;
    	JSONObject jsonObject = new JSONObject (jsonVoteReceipt);
        if(jsonObject.has("statusCode")) statusCode = jsonObject.getInt("statusCode");
        if(jsonObject.has("isCanceled")) isCanceled = jsonObject.getBoolean("isCanceled");
        if(jsonObject.has("vote")) eventVS = EventVS.parse(jsonObject.getJSONObject("vote"));
        VoteVS voteVS = new VoteVS(statusCode, eventVS);
        voteVS.setCanceled(isCanceled);
    	return voteVS;
    }
    
    public boolean isValid () throws Exception {
        return isValid;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
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
                    eventVS.getSubject(), eventVS.getOptionSelected().getContent());
        }
        if (!optionSelectedId.equals(eventVS.getOptionSelected().getId())) {
            return context.getString(R.string.option_error_msg);
        }
        if (smimeMessage.isValidSignature()) {
            return context.getString(R.string.vote_ok_msg,
                    eventVS.getSubject(), eventVS.getOptionSelected().getContent());
        }
        return null;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

    public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    public String getEventURL() {
        return eventURL;
    }

    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    public EventVS getVote() {
        return eventVS;
    }

    public void setVote(EventVS eventVS) {
        this.eventVS = eventVS;
    }

	public SMIMEMessageWrapper getCancelVoteReceipt() {
		return cancelVoteReceipt;
	}

	public void setCancelVoteReceipt(SMIMEMessageWrapper cancelVoteReceipt) {
		this.cancelVoteReceipt = cancelVoteReceipt;
		if(cancelVoteReceipt != null) isCanceled = true;
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
	
}