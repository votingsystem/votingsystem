package org.sistemavotacion.controlacceso.modelo;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="RepresentationDocument")
public class RepresentationDocument {
	
	public enum State {OK, CANCELLED, ERROR}
	
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = LoggerFactory.getLogger(RepresentationDocument.class);
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="state", nullable=false)
    private State state;
    @OneToOne
    private MensajeSMIME activationSMIME;
	
    @OneToOne
    private MensajeSMIME cancellationSMIME;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userId")
    private Usuario user;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="representativeId")
    private Usuario representative;
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCancelacion", length=23)
    private Date dateCanceled;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;

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

	public MensajeSMIME getActivationSMIME() {
		return activationSMIME;
	}

	public void setActivationSMIME(MensajeSMIME activationSMIME) {
		this.activationSMIME = activationSMIME;
	}

	public MensajeSMIME getCancellationSMIME() {
		return cancellationSMIME;
	}

	public void setCancellationSMIME(MensajeSMIME cancellationSMIME) {
		this.cancellationSMIME = cancellationSMIME;
	}

	public Usuario getRepresentative() {
		return representative;
	}

	public void setRepresentative(Usuario representative) {
		this.representative = representative;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
}
