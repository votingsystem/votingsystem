package org.votingsystem.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="TagVS")
@NamedQueries({
        @NamedQuery(name = "findTagByName", query = "SELECT tag FROM TagVS tag WHERE tag.name =:name")
})
public class TagVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String WILDTAG = "WILDTAG";

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="name", nullable=false, length=50) private String name;
    @Column(name="frequency") private Long frequency;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;
    @ManyToMany(mappedBy = "tagVSSet", fetch = FetchType.LAZY) private Set<EventVS> eventVSSet;
    @ManyToMany(mappedBy = "tagVSSet", fetch = FetchType.LAZY) private Set<UserVS> userVSSet;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;
    private int JSON;

    public TagVS() { }

    public TagVS(String name) {
        this.name = name;
    }

    public TagVS(String name, Long frequency) {
        this.name = name;
        this.frequency = frequency;
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

    public Set<EventVS> getEventVSSet() {
        return eventVSSet;
    }

    public void setEventVSSet(Set<EventVS> eventVSSet) {
        this.eventVSSet = eventVSSet;
    }

    public Set<UserVS> getUserVSSet() {
        return userVSSet;
    }

    public void setUserVSSet(Set<UserVS> userVSSet) {
        this.userVSSet = userVSSet;
    }

    @PrePersist
    public void prePersist() {
        Date date = new Date();
        setDateCreated(date);
        setLastUpdated(date);
        setName(name.toUpperCase());
    }

    @PreUpdate
    public void preUpdate() {
        setLastUpdated(new Date());
    }

    public static TagVS parse(Map dataMap) throws Exception {
        TagVS tagVS = new TagVS();
        tagVS.setName((String) dataMap.get("name"));
        if(dataMap.containsKey("id")) tagVS.setId((Long) dataMap.get("id"));
        return tagVS;
    }

    public static List<TagVS> parse(List<Map> mapList) throws Exception {
        List<TagVS> result = new ArrayList<TagVS>();
        for(Map map: mapList) {
            result.add(parse(map));
        }
        return result;
    }

    public Map toMap() {
        Map dataMap = new HashMap<>();
        dataMap.put("id", id);
        dataMap.put("name", name);
        return dataMap;
    }

}