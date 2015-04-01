package org.votingsystem.model;

import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="IncidenceVS")
public class IncidenceVS extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {REQUEST_ERROR}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable=false)
    private Type type;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23)
    private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23)
    private Date lastUpdated;
	
    public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public Type getType() {
		return type;
	}

    public Date getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
