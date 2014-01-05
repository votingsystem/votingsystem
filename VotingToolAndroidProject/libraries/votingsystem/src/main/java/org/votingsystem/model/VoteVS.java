package org.votingsystem.model;

import android.util.Log;
import org.json.JSONObject;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertificationRequestVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
public class VoteVS implements java.io.Serializable, ReceiptContainer {

    private static final long serialVersionUID = 1L;

	public static final String TAG = "VoteVS";
    
    private Long id;
    private transient SMIMEMessageWrapper voteReceipt;
    private transient SMIMEMessageWrapper cancelVoteReceipt;
    private byte[] encryptedKey = null;
    private transient CertificationRequestVS certificationRequest;
    private PrivateKey certVotePrivateKey;
    private EventVS eventVS;
    private FieldEventVS optionSelected;
    private String voteUUID;
    private String originHashCertVote;
    private String hashCertVoteHex;
    private String hashCertVSBase64;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;
    private Date dateCreated;
    private Date dateUpdated;

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
        map.put("hashAccessRequestBase64", hashAccessRequestBase64);
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
    
    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
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
	}

    public SMIMEMessageWrapper getVoteReceipt() {
        return voteReceipt;
    }

    public void setVoteReceipt(SMIMEMessageWrapper voteReceipt) throws Exception {
        JSONObject receiptContentJSON = new JSONObject(voteReceipt.getSignedContent());
        JSONObject receiptOptionSelected = receiptContentJSON.getJSONObject("optionSelected");
        if(optionSelected.getId() != receiptOptionSelected.getLong("id") ||
                !optionSelected.getContent().equals(receiptOptionSelected.getString("content"))) {
            throw new Exception("Receipt option doesn't match vote option !!!");
        }
        if (!voteReceipt.isValidSignature()) {
            throw new Exception("Receipt with signature errors!!!");
        }
        this.voteReceipt = voteReceipt;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateUpdated() {
        return dateUpdated;
	}

    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    @Override public Date getValidFrom() {
        return eventVS.getDateBegin();
    }

    @Override public Date getValidTo() {
        return eventVS.getDateFinish();
    }

    @Override public String getSubject() {
        return eventVS.getSubject();
    }

    @Override public TypeVS getType() {
        return TypeVS.VOTEVS;
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


    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(voteReceipt != null) s.writeObject(voteReceipt.getBytes());
            else s.writeObject(null);
            if(cancelVoteReceipt != null) s.writeObject(cancelVoteReceipt.getBytes());
            else s.writeObject(null);
            s.writeObject(eventVS);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        try {
            byte[] voteReceiptBytes = (byte[]) s.readObject();
            if(voteReceiptBytes != null) voteReceipt = new SMIMEMessageWrapper(null,
                    new ByteArrayInputStream(voteReceiptBytes), null);
            byte[] cancelReceiptBytes = (byte[]) s.readObject();
            if(cancelReceiptBytes != null) cancelVoteReceipt = new SMIMEMessageWrapper(null,
                    new ByteArrayInputStream(cancelReceiptBytes), null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

}