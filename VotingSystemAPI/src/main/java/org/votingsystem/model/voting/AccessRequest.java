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
@Entity @Table(name="AccessRequest")
public class AccessRequest extends EntityVS implements Serializable {

    public enum State {OK, CANCELED, ERROR}

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVS") private EventVS eventVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userId") private User user;
    @OneToOne private CMSMessage cmsMessage;
    @OneToOne(mappedBy="accessRequest") private VoteCanceler voteCanceler;
    @Column(name="hashAccessRequestBase64") private String hashAccessRequestBase64;
    @Column(name="metainf") private String metaInf;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated") private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated") private Date lastUpdated;

    public AccessRequest() {}

    public AccessRequest(User user, CMSMessage cmsMessage, State state, String hashAccessRequestBase64,
                         EventVS eventVS) {
        this.user = user;
        this.cmsMessage = cmsMessage;
        this.state = state;
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
        this.eventVS = eventVS;
    }

    public void setId(Long id) { this.id = id; }

    public Long getId() {return id;}

    public void setEventElection(EventVS eventVS) {  this.eventVS = eventVS; }

    public EventVS getEventElection() { return eventVS; }

    public AccessRequest setState(State state) {
        this.state = state;
        return this;
    }

    public State getState() { return state; }

    public String getAccessRequestHashBase64() { return hashAccessRequestBase64; }

    public void setAccessRequestHashBase64( String hashAccessRequestBase64) {
        this.hashAccessRequestBase64 = hashAccessRequestBase64;
    }

    public CMSMessage getCmsMessage() { return cmsMessage;  }

    public void setCmsMessage(CMSMessage cmsMessage) { this.cmsMessage = cmsMessage; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public VoteCanceler getVoteCanceler() { return voteCanceler; }

    public void setVoteCanceler(VoteCanceler voteCanceler) { this.voteCanceler = voteCanceler; }

    public Date getDateCreated() { return dateCreated; }

    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated;  }

    public Date getLastUpdated() { return lastUpdated; }

    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getMetaInf() { return metaInf;  }

    public void setMetaInf(String metaInf) { this.metaInf = metaInf; }

}
