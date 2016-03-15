package org.votingsystem.model.voting;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.User;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity @Table(name="RepresentationDocument")
public class RepresentationDocument extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(RepresentationDocument.class.getName());

    private static final long serialVersionUID = 1L;

    public enum State {OK, CANCELED, CANCELED_BY_REPRESENTATIVE, ERROR}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="state", nullable=false) private State state;
    @OneToOne
    @JoinColumn(name="activationCMS") private CMSMessage activationCMS;

    @OneToOne
    @JoinColumn(name="cancellationCMS") private CMSMessage cancellationCMS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user") private User user;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="representative") private User representative;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCanceled", length=23) private Date dateCanceled;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public RepresentationDocument() {}

    public RepresentationDocument(CMSMessage cmsMessage, User user, User representative,
                                  State state) {
        this.activationCMS = cmsMessage;
        this.user = user;
        this.representative = representative;
        this.state = state;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDateCanceled() {
        return dateCanceled;
    }

    public RepresentationDocument setDateCanceled(Date dateCanceled) {
        this.dateCanceled = dateCanceled;
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

    public CMSMessage getActivationCMS() {
        return activationCMS;
    }

    public void setActivationCMS(CMSMessage activationCMS) {
        this.activationCMS = activationCMS;
    }

    public CMSMessage getCancellationCMS() {
        return cancellationCMS;
    }

    public RepresentationDocument setCancellationCMS(CMSMessage cancellationCMS) {
        this.cancellationCMS = cancellationCMS;
        return this;
    }

    public User getRepresentative() {
        return representative;
    }

    public void setRepresentative(User representative) {
        this.representative = representative;
    }

    public State getState() {
        return state;
    }

    public RepresentationDocument setState(State state) {
        this.state = state;
        return this;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
