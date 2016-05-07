package org.votingsystem.model.voting;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.User;
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


    @Column(name="originHashAnonymousDelegation", unique=true) private String originHashAnonymousDelegation;
    @Column(name="hashAnonymousDelegation", unique=true) private String hashAnonymousDelegation;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userId") private User user;
    @OneToOne private CMSMessage delegationCMS;

    @OneToOne private CMSMessage cancellationCMS;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateFrom", nullable=false)
    private Date dateFrom;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateTo", nullable=false)
    private Date dateTo;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCancelled")
    private Date dateCancelled;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", insertable=true)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", insertable=true)
    private Date lastUpdated;

    public AnonymousDelegation() {}

    public AnonymousDelegation(Status status, CMSMessage delegationCMS, User user, Date dateFrom,
                               Date dateTo) {
        this.status = status;
        this.delegationCMS = delegationCMS;
        this.user = user;
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

    public CMSMessage getDelegationCMS() {
        return delegationCMS;
    }

    public void setDelegationCMS(CMSMessage delegationCMS) {
        this.delegationCMS = delegationCMS;
    }

    public CMSMessage getCancellationCMS() {
        return cancellationCMS;
    }

    public AnonymousDelegation setCancellationCMS(CMSMessage cancellationCMS) {
        this.cancellationCMS = cancellationCMS;
        return this;
    }

    public Date getDateCancelled() {
        return dateCancelled;
    }

    public AnonymousDelegation setDateCancelled(Date dateCancelled) {
        this.dateCancelled = dateCancelled;
        return this;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getOriginHashAnonymousDelegation() {
        return originHashAnonymousDelegation;
    }

    public AnonymousDelegation setOriginHashAnonymousDelegation(String originHashAnonymousDelegation) {
        this.originHashAnonymousDelegation = originHashAnonymousDelegation;
        return this;
    }

    public String getHashAnonymousDelegation() {
        return hashAnonymousDelegation;
    }

    public void setHashAnonymousDelegation(String hashAnonymousDelegation) {
        this.hashAnonymousDelegation = hashAnonymousDelegation;
    }

}