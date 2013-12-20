package org.votingsystem.model;

import android.content.Context;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertificationRequestVS;

import java.io.File;
import java.io.FileOutputStream;
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
    private String mensaje;
    private String eventURL;
    private Long eventVSElectionId;
    private Long optionSelectedId;
    private TypeVS typeVS;
    private ActorVS actorVS;
    private String accessControlServerURL;
    private boolean esValido = false;
    private SMIMEMessageWrapper smimeMessage;
    private SMIMEMessageWrapper cancelVoteReceipt;
    private byte[] encryptedKey = null;
    private boolean isCanceled = false;
    private CertificationRequestVS certificationRequest;
    private PrivateKey certVotePrivateKey;
    private EventVS voto;
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
    
    public VoteVS(int statusCode,
                  SMIMEMessageWrapper votoValidado, EventVS voto) throws Exception {
        this.smimeMessage = votoValidado;
        this.statusCode = statusCode;
        this.voto = voto;
        String receiptContent = votoValidado.getSignedContent();
        JSONObject receiptContentJSON = new JSONObject(receiptContent);
        this.optionSelectedId = receiptContentJSON.getLong("optionSelectedId");
        this.eventURL = receiptContentJSON.getString("eventURL");
        if (smimeMessage.isValidSignature()) {
            esValido = true;
        }
        if (ResponseVS.SC_ERROR_VOTE_REPEATED == statusCode) {//voto repetido
            esValido = false;
        }
        if (!optionSelectedId.equals(voto.getOptionSelected().getId())) {
            esValido = false;
        }
    }
    
    public VoteVS(int statusCode, EventVS voto) throws Exception {
        this.statusCode = statusCode;
        this.voto = voto;
    }
    
    public String toJSONString() throws JSONException {
    	Log.d(TAG + ".toJSONString(...)", " --- voto.getAccessRequestHashBase64(): "
    			+ voto.getAccessRequestHashBase64());
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("statusCode", statusCode);
        if(voto != null) jsonObject.put("voto", voto.toJSON());
        jsonObject.put("isCanceled", isCanceled);
        return jsonObject.toString();
    }
    
    public static VoteVS parse(String jsonVoteReceipt) throws Exception {
    	//Log.d(TAG + ".parse(...)", "- jsonVoteReceipt: '" + jsonVoteReceipt + "'");
    	if(jsonVoteReceipt == null) return null;
    	int statusCode = 0;
    	boolean isCanceled = false;
    	EventVS voto = null;
    	Log.d(TAG + ".parse(...)", " - parse(...)");
    	JSONObject jsonObject = new JSONObject (jsonVoteReceipt);
        if(jsonObject.has("statusCode"))
        	statusCode = jsonObject.getInt("statusCode");
        if(jsonObject.has("isCanceled"))
        	isCanceled = jsonObject.getBoolean("isCanceled");
        if(jsonObject.has("voto"))
        	voto = EventVS.parse(jsonObject.getJSONObject("voto"));
        VoteVS voteVS = new VoteVS(statusCode, voto);
        voteVS.setCanceled(isCanceled);
    	return voteVS;
    }
    
    public boolean esValido () throws Exception {
        return esValido;
    }

    public void writoToFile(File file) throws Exception {
    	if(file == null) throw new Exception("File null");
    	if(smimeMessage == null) throw new Exception("Receipt null");
    	smimeMessage.writeTo(new FileOutputStream(file));
    }
    
    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }

    /**
     * @return the eventVSElectionId
     */
    public Long getEventVSElectionId() {
        return eventVSElectionId;
    }

    /**
     * @param eventVSElectionId the eventVSElectionId to set
     */
    public void setEventVSElectionId(Long eventVSElectionId) {
        this.eventVSElectionId = eventVSElectionId;
    }

    /**
     * @return the optionSelectedId
     */
    public Long getOptionSelectedId() {
        return optionSelectedId;
    }

    /**
     * @param optionSelectedId the optionSelectedId to set
     */
    public void setOptionSelectedId(Long optionSelectedId) {
        this.optionSelectedId = optionSelectedId;
    }

    /**
     * @return the actorVS
     */
    public ActorVS getActorVS() {
        return actorVS;
    }

    /**
     * @param actorVS the actorVS to set
     */
    public void setActorVS(ActorVS actorVS) {
        this.actorVS = actorVS;
    }

    /**
     * @return the accessControlServerURL
     */
    public String getAccessControllServerURL() {
        return accessControlServerURL;
    }

    /**
     * @param accessControlServerURL the accessControlServerURL to set
     */
    public void setAccessControlServerURL(String accessControlServerURL) {
        this.accessControlServerURL = accessControlServerURL;
    }

    /**
     * @return the typeVS
     */
    public TypeVS getTypeVS() {
        return typeVS;
    }

    /**
     * @param typeVS the typeVS to set
     */
    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the mensaje
     */
    public String getMensaje(Context context) {
        if (ResponseVS.SC_ERROR_VOTE_REPEATED == statusCode) {//voto repetido
            return context.getString(R.string.vote_repeated_msg,
                    voto.getSubject(), voto.getOptionSelected().getContent());
        }
        if (!optionSelectedId.equals(voto.getOptionSelected().getId())) {
            return context.getString(R.string.option_error_msg);
        }
        if (smimeMessage.isValidSignature()) {
            return context.getString(R.string.vote_ok_msg,
                    voto.getSubject(), voto.getOptionSelected().getContent());
        }
        return null;
    }

    /**
     * @param mensaje the mensaje to set
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    /**
     * @return the smimeMessage
     */
    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

    /**
     * @param smimeMessage the smimeMessage to set
     */
    public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    /**
     * @return the eventURL
     */
    public String getEventURL() {
        return eventURL;
    }

    /**
     * @param eventURL the eventURL to set
     */
    public void setEventURL(String eventURL) {
        this.eventURL = eventURL;
    }

    /**
     * @return the voto
     */
    public EventVS getVoto() {
        return voto;
    }

    /**
     * @param voto the voto to set
     */
    public void setVoto(EventVS voto) {
        this.voto = voto;
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