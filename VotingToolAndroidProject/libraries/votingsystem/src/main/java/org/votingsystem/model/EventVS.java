package org.votingsystem.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.util.DateUtils;

import java.io.Serializable;
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
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = "EventVS";

    public enum State {ACTIVE, TERMINATED, CANCELLED, PENDING, PENDING_SIGNATURE, DELETED_FROM_SYSTEM}

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
    private AccessControlVS accessControl;
    private Integer numComments = 0;

    private Set<FieldEventVS> fieldEventVSSet = new HashSet<FieldEventVS>(0);
    private Set<EventTagVS> eventTagVSSet = new HashSet<EventTagVS>(0);
    private Set<CommentVS> commentVSSet = new HashSet<CommentVS>(0);

    private Date dateBegin;
    private Date dateFinish;
    private Date dateCreated;
    private Date lastUpdated;
    private String[] tags;
    private State state;
    private VoteVS vote;

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
        return fieldEventVSSet;
    }

    public void setFieldsEventVS(Set<FieldEventVS> fieldsEventVS) {
        this.fieldEventVSSet = fieldsEventVS;
    }

    public void setEventTagVSes(Set<EventTagVS> eventTagVSSet) {
        this.eventTagVSSet = eventTagVSSet;
    }

    public Set<EventTagVS> getEventTagVSes() {
        return eventTagVSSet;
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

    public void setCommentVSes(Set<CommentVS> commentVSSet) {
        this.commentVSSet = commentVSSet;
    }

    public Set<CommentVS> getCommentVSes() {
        return commentVSSet;
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

    public void setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }

    public AccessControlVS getAccessControl() {
        return accessControl;
    }

    public void setAccessControlVS(AccessControlVS accessControl) {
        this.accessControl = accessControl;
    }


    public void setVote(VoteVS vote) {
        this.vote = vote;
    }

    public VoteVS getVote() {
        return vote;
    }

    public String getURLStatistics() {
        String basePath = accessControl.getServerURL();
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

    public boolean isActive() {
        Date todayDate = java.util.Calendar.getInstance().getTime();
        if (todayDate.after(dateBegin) && todayDate.before(dateFinish)) return true;
        else return false;
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

        Map<String, Object> map = new HashMap<String, Object>();
        if(accessControl != null) {
            Map<String, Object> accessControlMap = new HashMap<String, Object>();
            accessControlMap.put("serverURL", accessControl.getServerURL());
            accessControlMap.put("name", accessControl.getName());
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
            map.put("operation", TypeVS.SMIME_CLAIM_SIGNATURE.toString());
        }
        JSONObject jsonObject = new JSONObject(map);
        if (fieldEventVSSet != null) {
            JSONArray jsonArray = new JSONArray();
            for (FieldEventVS field : fieldEventVSSet) {
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
            androidEventVS.setDateFinish(DateUtils.getDateFromString(jsonData.getString("dateFinish")));
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
        if(jsonData.has("voteVS")) {
            VoteVS voteVS = VoteVS.parse((Map) jsonData.get("voteVS"));
            androidEventVS.setVote(voteVS);
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
            jsonObject.put("dateBegin",DateUtils.getDateStr(dateBegin));
        if(dateFinish != null)
            jsonObject.put("dateFinish", DateUtils.getDateStr(dateFinish));
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
        if (fieldEventVSSet != null) {
            JSONArray jsonArray = new JSONArray();
            Map<String, Object> fieldMap = new HashMap<String, Object>();
            for (FieldEventVS field : fieldEventVSSet) {
                fieldMap.put("id", field.getId());
                fieldMap.put("content", field.getContent());
                fieldMap.put("value", field.getValue());
                JSONObject fieldJSON = new JSONObject(fieldMap);
                jsonArray.put(fieldJSON);
            }
            jsonObject.put("fieldsEventVS", jsonArray);
        }
        if (cardinality != null) jsonObject.put("cardinality", cardinality.toString());
        return jsonObject;
    }

}