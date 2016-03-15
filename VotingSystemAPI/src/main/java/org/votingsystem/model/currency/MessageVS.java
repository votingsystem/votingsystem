package org.votingsystem.model.currency;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.User;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.TypeVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;


/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="MessageVS")
public class MessageVS extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(MessageVS.class.getName());

    private static final long serialVersionUID = 1L;

    public enum State {PENDING, CONSUMED, ERROR, USED, UNKNOWN, REJECTED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="type") @Enumerated(EnumType.STRING) private TypeVS type;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @Column(name="content") private byte[] content;
    
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fromUser") private User fromUser;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="toUser") private User toUser;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23, insertable=true) private Date lastUpdated;

    @Column(name="metaInf", columnDefinition="TEXT")  private String metaInf;
    @Column(name="reason", columnDefinition="TEXT") private String reason;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="cmsParent") private CMSMessage senderCMSMessage;


    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

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

	public void setType(TypeVS type) {
		this.type = type;
	}

	public TypeVS getType() {
		return type;
	}

	public String getMetaInf() { return metaInf; }

	public void setMetaInf(String metaInf) {
		this.metaInf = metaInf;
	}

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }

    public CMSMessage getSenderCMSMessage() {
        return senderCMSMessage;
    }

    public void setSenderCMSMessage(CMSMessage senderCMSMessage) {
        this.senderCMSMessage = senderCMSMessage;
    }

    public State getState() {
        return state;
    }

    public MessageVS setState(State state) {
        this.state = state;
        return this;
    }

}
