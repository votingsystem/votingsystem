package org.votingsystem.model;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="VicketTagVS")
public class VicketTagVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final String WILDTAG = "WILDTAG";


    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="name", unique=true, nullable=false) private String name;
    @Column(name="frequency") private Long frequency;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;
    @ManyToMany(mappedBy = "tagVSSet", fetch = FetchType.LAZY) private Set<UserVS> userVSSet;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public VicketTagVS() { }

    public VicketTagVS(String name) {
        this.name = name;
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

    public Set<UserVS> getUserVSSet() {
        return userVSSet;
    }

    public void setUserVSSet(Set<UserVS> userVSSet) {
        this.userVSSet = userVSSet;
    }

    public void beforeInsert() {
        setName(name.toUpperCase());
    }

    public static VicketTagVS parse(JSONObject jsonData) throws Exception {
        VicketTagVS tagVS = new VicketTagVS();
        tagVS.setName(jsonData.getString("name"));
        if(jsonData.has("id")) tagVS.setId(jsonData.getLong("id"));
        return tagVS;
    }

    public static List<VicketTagVS> parse(JSONArray jsonArray) throws Exception {
        List<VicketTagVS> result = new ArrayList<VicketTagVS>();
        for(int i = 0; i < jsonArray.size(); i++) {
            result.add(parse((JSONObject) jsonArray.get(i)));
        }
        return result;
    }

    public JSONObject toJSON() throws Exception {
        JSONObject jsonData = new JSONObject();
        jsonData.put("id", id);
        jsonData.put("name", name);
        return jsonData;
    }

}