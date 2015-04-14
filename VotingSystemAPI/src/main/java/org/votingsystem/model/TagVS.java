package org.votingsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="TagVS")
@NamedQueries({
        @NamedQuery(name = "findTagByName", query = "SELECT tag FROM TagVS tag WHERE tag.name =:name")
})
@JsonIgnoreProperties(ignoreUnknown = true)
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
    @ManyToMany(mappedBy = "tagVSSet", fetch = FetchType.LAZY) @JsonIgnore private Set<UserVS> userVSSet;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;

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

}