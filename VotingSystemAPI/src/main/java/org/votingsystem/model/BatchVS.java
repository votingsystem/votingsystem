package org.votingsystem.model;

import org.votingsystem.util.EntityVS;
import org.votingsystem.util.TypeVS;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="BatchVS")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn( name="batchType", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("BatchVS")
public class BatchVS extends EntityVS implements Serializable  {

    private static Logger log = Logger.getLogger(BatchVS.class.getName());

    public static final long serialVersionUID = 1L;

    public enum State { OK, ERROR;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @Column(name="reason") private String reason;
    @Column(name="type") @Enumerated(EnumType.STRING) private TypeVS type;
    @Column(name="content") private byte[] content;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public BatchVS(byte[] content) throws IOException {
        this.content = content;
    }

    public BatchVS() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public State getState() {
        return state;
    }

    public BatchVS setState(State state) {
        this.state = state;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public TypeVS getType() {
        return type;
    }

    public BatchVS setType(TypeVS type) {
        this.type = type;
        return this;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

}