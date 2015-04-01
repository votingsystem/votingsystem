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
@Table(name="AnonymousDelegation")
public class AnonymousDelegation extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {OK, PENDING, FINISHED, CANCELED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private Status status;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @OneToOne private MessageSMIME delegationSMIME;

    @OneToOne private MessageSMIME cancellationSMIME;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateFrom", length=23, nullable=false)
    private Date dateFrom;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateTo", length=23, nullable=false)
    private Date dateTo;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCancelled", length=23)
    private Date dateCancelled;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23, insertable=true)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23, insertable=true)
    private Date lastUpdated;

    public AnonymousDelegation() {}

    public AnonymousDelegation(Status status, MessageSMIME delegationSMIME, UserVS userVS, Date dateFrom,
                               Date dateTo) {
        this.status = status;
        this.delegationSMIME = delegationSMIME;
        this.userVS = userVS;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }


    public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}

    public Status getStatus() {
        return status;
    }

    public AnonymousDelegation setStatus(Status status) {
        this.status = status;
        return this;
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

    public MessageSMIME getDelegationSMIME() {
        return delegationSMIME;
    }

    public void setDelegationSMIME(MessageSMIME delegationSMIME) {
        this.delegationSMIME = delegationSMIME;
    }

    public MessageSMIME getCancellationSMIME() {
        return cancellationSMIME;
    }

    public AnonymousDelegation setCancellationSMIME(MessageSMIME cancellationSMIME) {
        this.cancellationSMIME = cancellationSMIME;
        return this;
    }

    public Date getDateCancelled() {
        return dateCancelled;
    }

    public AnonymousDelegation setDateCancelled(Date dateCancelled) {
        this.dateCancelled = dateCancelled;
        return this;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

}