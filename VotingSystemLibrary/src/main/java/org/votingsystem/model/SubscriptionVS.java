package org.votingsystem.model;

import javax.persistence.*;

import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="SubscriptionVS")
public class SubscriptionVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public enum State {ACTIVE, PENDING, CANCELLED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="groupVS") private GroupVS groupVS;

    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @Column(name="reason") private String reason;

    @OneToOne private MessageSMIME subscriptionSMIME;

    @OneToOne private MessageSMIME activationSMIME;

    @OneToOne private MessageSMIME cancellationSMIME;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCancelled", length=23) private Date dateCancelled;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateActivated", length=23) private Date dateActivated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public GroupVS getGroupVS() {
        return groupVS;
    }

    public void setGroupVS(GroupVS groupVS) {
        this.groupVS = groupVS;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public MessageSMIME getSubscriptionSMIME() {
        return subscriptionSMIME;
    }

    public void setSubscriptionSMIME(MessageSMIME subscriptionSMIME) {
        this.subscriptionSMIME = subscriptionSMIME;
    }

    public MessageSMIME getActivationSMIME() {
        return activationSMIME;
    }

    public void setActivationSMIME(MessageSMIME activationSMIME) {
        this.activationSMIME = activationSMIME;
    }

    public MessageSMIME getCancellationSMIME() {
        return cancellationSMIME;
    }

    public void setCancellationSMIME(MessageSMIME cancellationSMIME) {
        this.cancellationSMIME = cancellationSMIME;
    }

    public Date getDateCancelled() {
        return dateCancelled;
    }

    public void setDateCancelled(Date dateCancelled) {
        this.dateCancelled = dateCancelled;
    }

    public Date getDateActivated() {
        return dateActivated;
    }

    public void setDateActivated(Date dateActivated) {
        this.dateActivated = dateActivated;
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

}