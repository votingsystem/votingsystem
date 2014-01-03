package org.votingsystem.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author jgzornoza
 */
public class EventVS implements Serializable {

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
    private ControlCenterVS controlCenter;
    private UserVS userVS;
    private AccessControlVS accessControlVS;
    private Integer numComments = 0;

    private Set<FieldEventVS> fieldsEventVS = new HashSet<FieldEventVS>(0);
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
    private FieldEventVS optionSelected;
    private State state;

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

    public Cardinality getElectionType() {
        return cardinality;
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

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    public Set<FieldEventVS> getFieldsEventVS() {
        return fieldsEventVS;
    }

    public void setFieldsEventVS(Set<FieldEventVS> fieldsEventVS) {
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

    public void setEventVSId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getEventVSId() {
        return eventId;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        if (tags.length == 0) return;
        ArrayList<String> arrayTags = new ArrayList<String>();
        for (String tag:tags) {
            arrayTags.add(tag.toLowerCase());
        }
        this.tags = arrayTags.toArray(tags);
    }

    public void setOptionSelected(FieldEventVS optionSelected) {
        this.optionSelected = optionSelected;
    }

    public FieldEventVS getOptionSelected() {
        return optionSelected;
    }

    public void setCommentVSes(Set<CommentVS> commentVSes) {
        this.commentVSes = commentVSes;
    }

    public Set<CommentVS> getCommentVSes() {
        return commentVSes;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    public int getNumComments() {
        return numComments;
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

    public static String getURLPart(TypeVS typeVS) {
        switch(typeVS) {
            case CLAIM_EVENT: return "/eventVSClaim";
            case MANIFEST_EVENT: return "/eventVSManifest";
            case VOTING_EVENT: return "/eventVSElection";
        }
        return "/eventVS";
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

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
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
        if (fieldsEventVS != null) {
            JSONArray jsonArray = new JSONArray();
            for (FieldEventVS field : fieldsEventVS) {
                Map<String, Object> campoMap = new HashMap<String, Object>();
                campoMap.put("id", field.getId());
                campoMap.put("content", field.getContent());
                campoMap.put("value", field.getValue());
                JSONObject camposJSON = new JSONObject(campoMap);
                jsonArray.put(camposJSON);
            }
            jsonObject.put("fieldsEventVS", jsonArray);
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

    public static EventVS parse(JSONObject jsonData) throws ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        EventVS androidEventVS = new EventVS();
        if (jsonData.has("URL"))
            androidEventVS.setURL(jsonData.getString("URL"));
        if (jsonData.has("content"))
            androidEventVS.setContent(jsonData.getString("content"));
        if (jsonData.has("subject"))
            androidEventVS.setSubject(jsonData.getString("subject"));
        if (jsonData.has("id")) {
            androidEventVS.setId(jsonData.getLong("id"));
            androidEventVS.setEventVSId(jsonData.getLong("id"));
        }
        if(jsonData.has("typeVS"))  androidEventVS.setTypeVS(TypeVS.valueOf(
                jsonData.getString("typeVS")));
        if (jsonData.has("eventId"))
            androidEventVS.setEventVSId(jsonData.getLong("eventId"));
        if (jsonData.has("userVS")) {
            UserVS userVS = new UserVS();
            userVS.setFullName(jsonData.getString("userVS"));
            androidEventVS.setUserVS(userVS);
        }
        if (jsonData.has("numSignaturesCollected"))
            androidEventVS.setNumSignaturesCollected(jsonData.getInt("numSignaturesCollected"));
        if (jsonData.has("numVotesCollected"))
            androidEventVS.setNumSignaturesCollected(jsonData.getInt("numVotesCollected"));
        if (jsonData.has("dateCreated"))
            androidEventVS.setDateCreated(DateUtils.getDateFromString(jsonData.getString("dateCreated")));
        if (jsonData.has("dateBegin"))
            androidEventVS.setDateBegin(DateUtils.getDateFromString(jsonData.getString("dateBegin")));
        if (jsonData.has("dateFinish") && !jsonData.isNull("dateFinish"))
            androidEventVS.setdateFinish(DateUtils.getDateFromString(jsonData.getString("dateFinish")));
        if (jsonData.has("accessControl") && jsonData.getJSONObject("accessControl") != null) {
            jsonObject = jsonData.getJSONObject("accessControl");
            AccessControlVS accessControlVS = new AccessControlVS();
            accessControlVS.setServerURL(jsonObject.getString("serverURL"));
            accessControlVS.setName(jsonObject.getString("name"));
            androidEventVS.setAccessControlVS(accessControlVS);
        }
        if (jsonData.has("tags") && jsonData.getJSONArray("tags") != null) {
            List<String> tags = new ArrayList<String>();
            jsonArray = jsonData.getJSONArray("tags");
            for (int i = 0; i< jsonArray.length(); i++) {
                tags.add(jsonArray.getString(i));
            }
            androidEventVS.setTags(tags.toArray(new String[jsonArray.length()]));
        }
        if (jsonData.has("fieldsEventVS")) {
            Set<FieldEventVS> fieldsEventVS = new HashSet<FieldEventVS>();
            jsonArray = jsonData.getJSONArray("fieldsEventVS");
            for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                FieldEventVS opcion = new FieldEventVS();
                opcion.setId(jsonObject.getLong("id"));
                opcion.setContent(jsonObject.getString("content"));
                fieldsEventVS.add(opcion);
            }
            androidEventVS.setFieldsEventVS(fieldsEventVS);
        }
        if (jsonData.has("controlCenter")) {
            jsonObject = jsonData.getJSONObject("controlCenter");
            ControlCenterVS controlCenter = new ControlCenterVS();
            if(jsonObject.has("id")) controlCenter.setId(jsonObject.getLong("id"));
            if(jsonObject.has("serverURL")) controlCenter.setServerURL(jsonObject.getString("serverURL"));
            if(jsonObject.has("name")) controlCenter.setName(jsonObject.getString("name"));
            androidEventVS.setControlCenter(controlCenter);
        }
        if (jsonData.has("state")) {
            androidEventVS.setState(State.valueOf(jsonData.getString("state")));
        }
        if (jsonData.has("hashAccessRequestBase64")) {
            androidEventVS.setAccessRequestHashBase64(jsonData.getString("hashAccessRequestBase64"));
        }
        if (jsonData.has("originHashAccessRequest")) {
            androidEventVS.setOriginHashAccessRequest(jsonData.getString("originHashAccessRequest"));
        }
        if (jsonData.has("hashCertVSBase64")) {
            androidEventVS.setHashCertVSBase64(jsonData.getString("hashCertVSBase64"));
        }
        if (jsonData.has("originHashCertVote")) {
            androidEventVS.setOriginHashCertVote(jsonData.getString("originHashCertVote"));
        }
        if (jsonData.has("optionSelected")) {
            jsonObject = jsonData.getJSONObject("optionSelected");
            FieldEventVS optionSelected = new FieldEventVS();
            optionSelected.setId(jsonObject.getLong("id"));
            optionSelected.setContent(jsonObject.getString("content"));
            androidEventVS.setOptionSelected(optionSelected);
        }
        return androidEventVS;
    }

    public JSONObject toJSON() throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("subject", subject);
        jsonObject.put("content", content);
        if(state != null) jsonObject.put("state", state.toString());
        if(URL != null) jsonObject.put("URL", URL);
        if(dateBegin != null)
            jsonObject.put("dateBegin",DateUtils.getStringFromDate(dateBegin));
        if(dateFinish != null)
            jsonObject.put("dateFinish", DateUtils.getStringFromDate(dateFinish));
        if (typeVS != null) jsonObject.put("typeVS", typeVS.toString());
        if (id != null) jsonObject.put("id", eventId);
        if (eventId != null) jsonObject.put("eventId", eventId);
        if (tags != null) {
            JSONArray jsonArray = new JSONArray();
            for (String tag : tags) {
                jsonArray.put(tag);
            }
            jsonObject.put("tags", jsonArray);
        }
        if(userVS != null) jsonObject.put("userVS", userVS.getFullName());
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
            Map<String, Object> fieldMap = new HashMap<String, Object>();
            for (FieldEventVS field : fieldsEventVS) {
                fieldMap.put("id", field.getId());
                fieldMap.put("content", field.getContent());
                fieldMap.put("value", field.getValue());
                JSONObject fieldJSON = new JSONObject(fieldMap);
                jsonArray.put(fieldJSON);
            }
            jsonObject.put("fieldsEventVS", jsonArray);
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
