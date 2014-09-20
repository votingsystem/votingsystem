package org.votingsystem.model;

import org.json.JSONArray;
import org.json.JSONObject;

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
public class TagVS {

    public static final String WILDTAG = "WILDTAG";

    private Long id;
    private String name;
    private Long frequency;
    private Date dateCreated;
    private Date lastUpdated;


    private Set<EventTagVS> eventTagVSes = new HashSet<EventTagVS>(0);

    public TagVS() { }
   
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

    public static Map<String, BigDecimal> parseTagVSBalanceMap(JSONObject jsonData) throws Exception {
        Map<String, BigDecimal> result = new HashMap<String, BigDecimal>();
        Iterator tagIterator = jsonData.keys();
        while(tagIterator.hasNext()) {
            String tagStr = (String) tagIterator.next();
            BigDecimal tagBalance = new BigDecimal(jsonData.getString(tagStr));
            result.put(tagStr, tagBalance);
        }
        return result;
    }

    public static TagVS parse(JSONObject jsonData) throws Exception {
        TagVS tagVS = new TagVS();
        tagVS.setName(jsonData.getString(jsonData.getString("name")));
        tagVS.setId(jsonData.getLong("id"));
        return tagVS;
    }

    public static List<TagVS> parse(JSONArray jsonArray) throws Exception {
        List<TagVS> result = new ArrayList<TagVS>();
        for(int i = 0; i < jsonArray.length(); i++) {
            result.add(parse((JSONObject) jsonArray.get(i)));
        }
        return result;
    }

}