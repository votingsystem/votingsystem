package org.votingsystem.vicket.model;

import org.votingsystem.model.EventVS;
import org.votingsystem.model.UserVS;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="BalanceTagVS")
public class BalanceTagVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="name", unique=true, nullable=false) private String name;
    @Column(name="frequency") private Long frequency;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;
    @ManyToMany(mappedBy = "tagVSSet", fetch = FetchType.LAZY) private Set<UserVSAccount> userVSAccountSet;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public BalanceTagVS() { }
   
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


    public Set<UserVSAccount> getUserVSAccountSet() {
        return userVSAccountSet;
    }

    public void setUserVSAccountSet(Set<UserVSAccount> userVSAccountSet) {
        this.userVSAccountSet = userVSAccountSet;
    }
}