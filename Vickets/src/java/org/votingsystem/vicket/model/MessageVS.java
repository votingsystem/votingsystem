package org.votingsystem.vicket.model;

import org.apache.log4j.Logger;
import org.votingsystem.model.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;


/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="MessageVS")
public class MessageVS implements Serializable {

    private static Logger logger = Logger.getLogger(MessageVS.class);

    private static final long serialVersionUID = 1L;

    public enum State {PENDING, CONSUMED, ERROR, USED, UNKNOWN, REJECTED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="type") @Enumerated(EnumType.STRING) private TypeVS type;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @Column(name="content") @Lob private byte[] content;
    
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fromUserVS") private UserVS fromUserVS;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="toUserVS") private UserVS toUserVS;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23, insertable=true) private Date lastUpdated;

    @Column(name="metaInf", columnDefinition="TEXT")  private String metaInf;
    @Column(name="reason", columnDefinition="TEXT") private String reason;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="smimeParent") private MessageSMIME senderMessageSMIME;


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

    public UserVS getFromUserVS() {
        return fromUserVS;
    }

    public void setFromUserVS(UserVS fromUserVS) {
        this.fromUserVS = fromUserVS;
    }

    public UserVS getToUserVS() {
        return toUserVS;
    }

    public void setToUserVS(UserVS toUserVS) {
        this.toUserVS = toUserVS;
    }

    public MessageSMIME getSenderMessageSMIME() {
        return senderMessageSMIME;
    }

    public void setSenderMessageSMIME(MessageSMIME senderMessageSMIME) {
        this.senderMessageSMIME = senderMessageSMIME;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

}
