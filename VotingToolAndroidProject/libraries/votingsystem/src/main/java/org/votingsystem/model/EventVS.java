package org.votingsystem.model;

import android.util.Log;
import org.bouncycastle2.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.util.DateUtils;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;

/**
 *
 * @author jgzornoza
 */
public class EventVS {

    private static final long serialVersionUID = 1L;

    public static final String TAG = "EventVS";

    public enum State {ACTIVE, TERMINATED, CANCELLED, AWAITING, PENDING_SIGNATURE, DELETED_FROM_SYSTEM}

    public enum Cardinality { MULTIPLE, EXCLUSIVE}


    private Long id;
    private Long eventId;
    private TypeVS typeVS;
    private Cardinality cardinality;
    private String content;
    private String subject;
    private String URL;
    private Integer numSignaturesCollected;
    private Integer numVotesCollected;
    private Boolean signed;
    private ControlCenterVS controlCenter;
    private UserVS userVS;
    private AccessControlVS accessControlVS;
    private Integer numeroComentarios = 0;

    private Set<OptionVS> fieldsEventVS = new HashSet<OptionVS>(0);
    private Set<OptionVS> campos = new HashSet<OptionVS>(0);
    private Set<EventTagVS> eventTagVSes = new HashSet<EventTagVS>(0);
    private Set<CommentVS> commentVSes = new HashSet<CommentVS>(0);

    private Date dateBegin;
    private Date dateFinish;
    private Date dateCreated;
    private Date lastUpdated;

    private String originHashCertVote;
    private String hashCertVSBase64;
    private String originHashAccessRequest;
    private String hashAccessRequestBase64;

    private String[] tags;

    private OptionVS optionSelected;
    private String state;

    public String getContentOpcion (Long optionSelected) {
        String resultado = null;
        for (OptionVS opcion : fieldsEventVS) {
            if (optionSelected.equals(opcion.getId())) {
                resultado = opcion.getContent();
                break;
            }
        }
        return resultado;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public String getContent () {
        return content;
    }

    public void setContent (String content) {
        this.content = content;
    }

    public String getSubject() { return subject; }

    public void setSubject (String subject) {
        this.subject = subject;
    }

    /**
     * @return the typeEleccion
     */
    public Cardinality getElectionType() {
        return cardinality;
    }

    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    /**
     * @return the fieldsEventVS
     */
    public Set<OptionVS> getFieldsEventVS() {
        return fieldsEventVS;
    }

    /**
     * @param fieldsEventVS the fieldsEventVS to set
     */
    public void getFieldsEventVSiones(Set<OptionVS> fieldsEventVS) {
        this.fieldsEventVS = fieldsEventVS;
    }

    public void setEventTagVSes(Set<EventTagVS> eventTagVSes) {
        this.eventTagVSes = eventTagVSes;
    }

    public Set<EventTagVS> getEventTagVSes() {
        return eventTagVSes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Boolean getSigned() {
        return signed;
    }

    public void setEventVSId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getEventVSId() {
        return eventId;
    }

    /**
     * @return the tags
     */
    public String[] getEtiquetas() {
        return tags;
    }

    public void setEtiquetas(String[] tags) {
        if (tags.length == 0) return;
        ArrayList<String> arrayEtiquetas = new ArrayList<String>();
        for (String tag:tags) {
            arrayEtiquetas.add(tag.toLowerCase());
        }
        this.tags = arrayEtiquetas.toArray(tags);
    }

    public void setOptionSelected(OptionVS optionSelected) {
        this.optionSelected = optionSelected;
    }

    public OptionVS getOptionSelected() {
        return optionSelected;
    }

    public void setCampos(Set<OptionVS> campos) {
        this.fieldsEventVS = campos;
    }

    public Set<OptionVS> getCampos() {
        return campos;
    }

    public void setCommentVSes(Set<CommentVS> commentVSes) {
        this.commentVSes = commentVSes;
    }

    public Set<CommentVS> getCommentVSes() {
        return commentVSes;
    }

    public void setNumeroComentarios(int numeroComentarios) {
        this.numeroComentarios = numeroComentarios;
    }

    public int getNumeroComentarios() {
        return numeroComentarios;
    }

    public ControlCenterVS getControlCenter() {
        return controlCenter;
    }

    public void setControlCenter(ControlCenterVS controlCenter) {
        this.controlCenter = controlCenter;
    }

    public Integer getNumSignaturesCollected() {
        return numSignaturesCollected;
    }

    public void setNumSignaturesCollected(Integer numSignaturesCollected) {
        this.numSignaturesCollected = numSignaturesCollected;
    }

    public Integer getNumVotesCollected() {
        return numVotesCollected;
    }

    public void setNumVotesCollected(Integer numVotesCollected) {
        this.numVotesCollected = numVotesCollected;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public Date getDateBegin() {
        return dateBegin;
    }

    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public void setdateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }

    public AccessControlVS getAccessControl() {
        return accessControlVS;
    }

    public void setAccessControlVS(AccessControlVS accessControlVS) {
        this.accessControlVS = accessControlVS;
    }

    public State getStateEnumValue () {
        if(state == null) return null;
        else return State.valueOf(state);
    }

    /*public void checkDates(String accessControlURL) {
    	if(state == null) return;
        Date fecha = DateUtils.getTodayDate();
        State stateEnum = State.valueOf(state);
        if(!(fecha.after(dateBegin)
        		&& fecha.before(dateFinish))){
        	if(stateEnum == State.ACTIVE){
        		final String checkURL = ;
                Runnable runnable = new Runnable() {
                    public void run() {
                    	try {
							HttpHelper.getData(checkURL, null);
						} catch (Exception e) {
							e.printStackTrace();
						}
                    }
                };
                new Thread(runnable).start();
        	}
        }
    }*/

    public String getOriginHashCertVote() {
        return originHashCertVote;
    }

    public void setOriginHashCertVote(String originHashCertVote) {
        this.originHashCertVote = originHashCertVote;
    }

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
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

    public EventVS initVoteData() throws NoSuchAlgorithmException {
        this.originHashAccessRequest = UUID.randomUUID().toString();
        this.hashAccessRequestBase64 = CMSUtils.getHashBase64(
                this.originHashAccessRequest, ContextVS.SIG_HASH);
        this.originHashCertVote = UUID.randomUUID().toString();
        this.hashCertVSBase64 = CMSUtils.getHashBase64(
                this.originHashCertVote, ContextVS.SIG_HASH);
        return this;
    }

    public String getCancelVoteData() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("eventURL", URL);
        jsonObject.put("originHashCertVote",
                getOriginHashCertVote());
        jsonObject.put("hashCertVSBase64",
                getHashCertVSBase64());
        jsonObject.put("originHashAccessRequest",
                getOriginHashAccessRequest());
        jsonObject.put("hashAccessRequestBase64",
                getAccessRequestHashBase64());
        return jsonObject.toString();
    }

    public String getURLStatistics() {
        String basePath = accessControlVS.getServerURL();
        switch(this.getTypeVS()) {
            case VOTING_EVENT:
                basePath = basePath + "/eventVSElection/";
                break;
            case CLAIM_EVENT:
                basePath = basePath + "/eventVSClaim/";
                break;
            case MANIFEST_EVENT:
                basePath = basePath + "/eventVSManifest/";
                break;
        }
        return basePath + id + "/statistics";
    }


    public void setAccessRequestHashBase64(String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public boolean isActive() {
        Date todayDate = DateUtils.getTodayDate();
        if (todayDate.after(dateBegin) && todayDate.before(dateFinish)) return true;
        else return false;
    }

    public JSONObject getVoteJSON() {
        Log.d(TAG + ".getVoteJSON", "getVoteJSON");
        Map map = new HashMap();
        map.put("operation", TypeVS.SEND_SMIME_VOTE.toString());
        map.put("eventURL", URL);
        map.put("optionSelectedId", optionSelected.getId());
        map.put("optionSelectedContent", optionSelected.getContent());
        map.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(map);
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return this.state;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String uRL) {
        URL = uRL;
    }

    public JSONObject getSignatureContentJSON() throws JSONException {
        Log.d(TAG + ".getSignatureData(...)", "");
        Map<String, Object> map = new HashMap<String, Object>();
        if(accessControlVS != null) {
            Map<String, Object> accessControlMap = new HashMap<String, Object>();
            accessControlMap.put("serverURL", accessControlVS.getServerURL());
            accessControlMap.put("name", accessControlVS.getName());
            map.put("accessControlVS", accessControlMap);
        }
        map.put("id", id);
        if(eventId != null) map.put("eventId", eventId);
        else map.put("eventId", id);
        map.put("subject", subject);
        map.put("content", content);
        map.put("UUID", UUID.randomUUID().toString());
        map.put("URL", URL);
        if(TypeVS.CLAIM_EVENT == typeVS) {
            map.put("operation", TypeVS.SMIME_CLAIM_SIGNATURE);
        }
        JSONObject jsonObject = new JSONObject(map);
        if (campos != null) {
            JSONArray jsonArray = new JSONArray();
            for (OptionVS campo : campos) {
                Map<String, Object> campoMap = new HashMap<String, Object>();
                campoMap.put("id", campo.getId());
                campoMap.put("content", campo.getContent());
                campoMap.put("value", campo.getValue());
                JSONObject camposJSON = new JSONObject(campoMap);
                jsonArray.put(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        return jsonObject;
    }

    public static EventVS parse(String eventoStr) throws ParseException, JSONException  {
        Log.d(TAG + ".parse(...)", eventoStr);
        return parse(new JSONObject(eventoStr));
    }

    public JSONObject getAccessRequestJSON() {
        Log.d(TAG + ".getAccessRequestJSON(...)", "getAccessRequestJSON");
        Map map = new HashMap();
        map.put("operation", TypeVS.ACCESS_REQUEST.toString());
        if(eventId != null) map.put("eventId", eventId);
        else map.put("eventId", id);
        map.put("eventURL", URL);
        map.put("UUID", UUID.randomUUID().toString());
        map.put("hashAccessRequestBase64", hashAccessRequestBase64);
        return new JSONObject(map);
    }

    public static EventVS parse(JSONObject eventoJSON) throws ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        EventVS androidEventVS = new EventVS();
        if (eventoJSON.has("URL"))
            androidEventVS.setURL(eventoJSON.getString("URL"));
        if (eventoJSON.has("content"))
            androidEventVS.setContent(eventoJSON.getString("content"));
        if (eventoJSON.has("subject"))
            androidEventVS.setSubject(eventoJSON.getString("subject"));
        if (eventoJSON.has("id")) {
            androidEventVS.setId(eventoJSON.getLong("id"));
            androidEventVS.setEventVSId(eventoJSON.getLong("id"));
        }
        if (eventoJSON.has("eventId"))
            androidEventVS.setEventVSId(eventoJSON.getLong("eventId"));
        if (eventoJSON.has("userVS")) {
            UserVS userVS = new UserVS();
            userVS.setFullName(eventoJSON.getString("userVS"));
            androidEventVS.setUserVS(userVS);
        }
        if (eventoJSON.has("numSignaturesCollected"))
            androidEventVS.setNumSignaturesCollected(eventoJSON.getInt("numSignaturesCollected"));
        if (eventoJSON.has("numVotesCollected"))
            androidEventVS.setNumSignaturesCollected(eventoJSON.getInt("numVotesCollected"));
        if (eventoJSON.has("dateCreated"))
            androidEventVS.setDateCreated(DateUtils.getDateFromString(eventoJSON.getString("dateCreated")));
        if (eventoJSON.has("dateBegin"))
            androidEventVS.setDateBegin(DateUtils.getDateFromString(eventoJSON.getString("dateBegin")));
        if (eventoJSON.has("dateFinish") && !eventoJSON.isNull("dateFinish"))
            androidEventVS.setdateFinish(DateUtils.getDateFromString(eventoJSON.getString("dateFinish")));
        if (eventoJSON.has("accessControl") && eventoJSON.getJSONObject("accessControl") != null) {
            jsonObject = eventoJSON.getJSONObject("accessControl");
            AccessControlVS accessControlVS = new AccessControlVS();
            accessControlVS.setServerURL(jsonObject.getString("serverURL"));
            accessControlVS.setName(jsonObject.getString("name"));
            androidEventVS.setAccessControlVS(accessControlVS);
        }
        if (eventoJSON.has("tags") && eventoJSON.getJSONArray("tags") != null) {
            List<String> tags = new ArrayList<String>();
            jsonArray = eventoJSON.getJSONArray("tags");
            for (int i = 0; i< jsonArray.length(); i++) {
                tags.add(jsonArray.getString(i));
            }
            androidEventVS.setEtiquetas(tags.toArray(new String[jsonArray.length()]));
        }
        if (eventoJSON.has("campos")) {
            Set<OptionVS> campos = new HashSet<OptionVS>();
            jsonArray = eventoJSON.getJSONArray("campos");
            for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                OptionVS campo = new OptionVS();
                campo.setId(jsonObject.getLong("id"));
                campo.setContent(jsonObject.getString("content"));
                campos.add(campo);
            }
            androidEventVS.setCampos(campos);
        }
        if (eventoJSON.has("fieldsEventVS")) {
            Set<OptionVS> fieldsEventVS = new HashSet<OptionVS>();
            jsonArray = eventoJSON.getJSONArray("fieldsEventVS");
            for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                OptionVS opcion = new OptionVS();
                opcion.setId(jsonObject.getLong("id"));
                opcion.setContent(jsonObject.getString("content"));
                fieldsEventVS.add(opcion);
            }
            androidEventVS.setCampos(fieldsEventVS);
        }
        if (eventoJSON.has("controlCenter")) {
            jsonObject = eventoJSON.getJSONObject("controlCenter");
            ControlCenterVS controlCenter = new ControlCenterVS();
            if(jsonObject.has("id")) controlCenter.setId(jsonObject.getLong("id"));
            if(jsonObject.has("serverURL")) controlCenter.setServerURL(jsonObject.getString("serverURL"));
            if(jsonObject.has("name")) controlCenter.setName(jsonObject.getString("name"));
            androidEventVS.setControlCenter(controlCenter);
        }
        if (eventoJSON.has("state")) {
            androidEventVS.setState(eventoJSON.getString("state"));
        }
        if (eventoJSON.has("hashAccessRequestBase64")) {
            androidEventVS.setAccessRequestHashBase64(eventoJSON.getString("hashAccessRequestBase64"));
        }
        if (eventoJSON.has("originHashAccessRequest")) {
            androidEventVS.setOriginHashAccessRequest(eventoJSON.getString("originHashAccessRequest"));
        }
        if (eventoJSON.has("hashCertVSBase64")) {
            androidEventVS.setHashCertVSBase64(eventoJSON.
                    getString("hashCertVSBase64"));
        }
        if (eventoJSON.has("originHashCertVote")) {
            androidEventVS.setOriginHashCertVote(eventoJSON.getString("originHashCertVote"));
        }
        if (eventoJSON.has("optionSelected")) {
            jsonObject = eventoJSON.getJSONObject("optionSelected");
            OptionVS optionSelected = new OptionVS();
            optionSelected.setId(jsonObject.getLong("id"));
            optionSelected.setContent(jsonObject.getString("content"));
            androidEventVS.setOptionSelected(optionSelected);
        }
        return androidEventVS;
    }

    public JSONObject toJSON() throws JSONException{
        Log.d(TAG + ".toJSON(...)", " - toJSON -");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("subject", subject);
        jsonObject.put("content", content);
        if(dateBegin != null)
            jsonObject.put("dateBegin",DateUtils.getStringFromDate(dateBegin));
        if(dateFinish != null)
            jsonObject.put("dateFinish", DateUtils.getStringFromDate(dateFinish));
        if (typeVS != null) jsonObject.put("type", typeVS.toString());
        if (eventId != null) jsonObject.put("eventId", eventId);
        if (tags != null) {
            JSONArray jsonArray = new JSONArray();
            for (String etiqueta : tags) {
                jsonArray.put(etiqueta);
            }
            jsonObject.put("tags", jsonArray);
        }
        if (controlCenter != null) {
            Map<String, Object> controlCenterMap = new HashMap<String, Object>();
            controlCenterMap.put("id", controlCenter.getId());
            controlCenterMap.put("name", controlCenter.getName());
            controlCenterMap.put("serverURL", controlCenter.getServerURL());
            JSONObject controlCenterJSON = new JSONObject(controlCenterMap);
            jsonObject.put("controlCenter", controlCenterJSON);
        }
        if (fieldsEventVS != null) {
            JSONArray jsonArray = new JSONArray();
            Map<String, Object> opcionMap = new HashMap<String, Object>();
            for (OptionVS opcion : fieldsEventVS) {
                opcionMap.put("id", opcion.getId());
                opcionMap.put("content", opcion.getContent());
                JSONObject opcionJSON = new JSONObject(opcionMap);
                jsonArray.put(opcionJSON);
            }
            jsonObject.put("fieldsEventVS", jsonArray);
        }
        if (campos != null) {
            JSONArray jsonArray = new JSONArray();
            for (OptionVS campo : campos) {
                Map<String, Object> campoMap = new HashMap<String, Object>();
                campoMap.put("content", campo.getContent());
                campoMap.put("value", campo.getValue());
                campoMap.put("id", campo.getId());
                JSONObject camposJSON = new JSONObject(campoMap);
                jsonArray.put(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        if (cardinality != null) jsonObject.put("cardinality", cardinality.toString());
        if (optionSelected != null) {
            Map<String, Object> optionSelectedMap = new HashMap<String, Object>();
            optionSelectedMap.put("id", optionSelected.getId());
            optionSelectedMap.put("content", optionSelected.getContent());
            JSONObject optionSelectedJSON = new JSONObject(optionSelectedMap);
            jsonObject.put("optionSelected", optionSelectedJSON);
        }
        if(hashAccessRequestBase64 != null) jsonObject.put("hashAccessRequestBase64", hashAccessRequestBase64);
        if(originHashAccessRequest != null) jsonObject.put("originHashAccessRequest", originHashAccessRequest);
        if(hashCertVSBase64 != null) jsonObject.put("hashCertVSBase64", hashCertVSBase64);
        if(originHashCertVote != null) jsonObject.put("originHashCertVote", originHashCertVote);
        return jsonObject;
    }


}
