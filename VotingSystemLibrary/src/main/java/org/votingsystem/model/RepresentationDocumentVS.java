package org.votingsystem.model;

import org.apache.log4j.Logger;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;


/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="RepresentationDocumentVS")
public class RepresentationDocumentVS implements Serializable {

    private static Logger log = Logger.getLogger(RepresentationDocumentVS.class);

    private static final long serialVersionUID = 1L;

	public enum State {OK, CANCELLED, CANCELLED_BY_REPRESENTATIVE, ERROR}
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="state", nullable=false) private State state;
    @OneToOne
    @JoinColumn(name="activationSMIME") private MessageSMIME activationSMIME;
	
    @OneToOne
    @JoinColumn(name="cancellationSMIME") private MessageSMIME cancellationSMIME;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="representative") private UserVS representative;
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCanceled", length=23) private Date dateCanceled;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;

	public Date getDateCanceled() {
		return dateCanceled;
	}

	public void setDateCanceled(Date dateCanceled) {
		this.dateCanceled = dateCanceled;
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

	public MessageSMIME getActivationSMIME() {
		return activationSMIME;
	}

	public void setActivationSMIME(MessageSMIME activationSMIME) {
		this.activationSMIME = activationSMIME;
	}

	public MessageSMIME getCancellationSMIME() {
		return cancellationSMIME;
	}

	public RepresentationDocumentVS setCancellationSMIME(MessageSMIME cancellationSMIME) {
		this.cancellationSMIME = cancellationSMIME;
        return this;
	}

	public UserVS getRepresentative() {
		return representative;
	}

	public void setRepresentative(UserVS representative) {
		this.representative = representative;
	}

	public State getState() {
		return state;
	}

	public RepresentationDocumentVS setState(State state) {
		this.state = state;
        return this;
	}

	public UserVS getUserVS() {
		return userVS;
	}

	public void setUserVS(UserVS user) {
		this.userVS = user;
	}
}
