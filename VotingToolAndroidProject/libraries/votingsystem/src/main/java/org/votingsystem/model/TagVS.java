package org.votingsystem.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class TagVS  implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String WILDTAG = "WILDTAG";

    private Long id;
    private String name;
    private BigDecimal total;
    private BigDecimal timeLimited;
    private Long frequency;
    private Date dateCreated;
    private Date lastUpdated;


    private Set<EventTagVS> eventTagVSes = new HashSet<EventTagVS>(0);

    public TagVS() { }

    public TagVS(String name) {
        this.name = name;
    }

    public TagVS(String name, BigDecimal total, BigDecimal timeLimited) {
        this.name = name;
        this.total = total;
        this.timeLimited = timeLimited;
    }
   
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public Set<EventTagVS> getEventTagVSes() {
        return this.eventTagVSes;
    }
    
    public void setEventTagVSes(Set<EventTagVS> eventTagVSes) {
        this.eventTagVSes = eventTagVSes;
    }

    public void setFrequency(Long frequency) {
        this.frequency = frequency;
    }

    public Long getFrequency() {
        return frequency;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public static Map<String, TagVS> parseTagVSBalanceMap(JSONObject jsonData) throws Exception {
        Map<String, TagVS> result = new HashMap<String, TagVS>();
        Iterator tagIterator = jsonData.keys();
        while(tagIterator.hasNext()) {
            String tagStr = (String) tagIterator.next();
            Object tagData = jsonData.get(tagStr);
            if(tagData instanceof String || tagData instanceof Double) {
                result.put(tagStr, new TagVS(tagStr, new BigDecimal(tagData.toString()), null));
            } else {
                BigDecimal tagTotal = new BigDecimal(((JSONObject)tagData).getString("total"));
                BigDecimal tagTimeLimited = new BigDecimal(((JSONObject)tagData).getString("timeLimited"));
                result.put(tagStr, new TagVS(tagStr, tagTotal, tagTimeLimited));
            }
        }
        return result;
    }

    public static TagVS parse(JSONObject jsonData) throws Exception {
        TagVS tagVS = new TagVS();
        tagVS.setName(jsonData.getString("name"));
        if(jsonData.has("id")) tagVS.setId(jsonData.getLong("id"));
        return tagVS;
    }

    public JSONObject toJSON() throws Exception {
        JSONObject jsonData = new JSONObject();
        jsonData.put("id", id);
        jsonData.put("name", name);
        return jsonData;
    }

    public static List<TagVS> parse(JSONArray jsonArray) throws Exception {
        List<TagVS> result = new ArrayList<TagVS>();
        for(int i = 0; i < jsonArray.length(); i++) {
            result.add(parse((JSONObject) jsonArray.get(i)));
        }
        return result;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(BigDecimal timeLimited) {
        this.timeLimited = timeLimited;
    }
}