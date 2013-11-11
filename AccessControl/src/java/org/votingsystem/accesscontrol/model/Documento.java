package org.votingsystem.accesscontrol.model;

import javax.persistence.GeneratedValue;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.bouncycastle.tsp.TimeStampToken;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="Documento")
public class Documento implements java.io.Serializable {

	 private static final long serialVersionUID = 1L;
	 
	 public enum Estado {MANIFIESTO, MANIFIESTO_VALIDADO, FIRMA_DE_MANIFIESTO, 
		 FIRMA_MANIFIESTO_VALIDADA, CON_ERRORES, MODIFICADO, SOLICITUD_COPIA,
		 SOLICITUD_COPIA_ERROR, VALIDADO}
	 
	 @Id @GeneratedValue(strategy=IDENTITY)
	 @Column(name="id", unique=true, nullable=false)
	 private Long id;
	 @Enumerated(EnumType.STRING)
	 @Column(name="estado", nullable=false)
	 private Estado estado;
	 @ManyToOne(fetch=FetchType.LAZY)
	 @JoinColumn(name="usuarioId")
	 private Usuario usuario;
     @ManyToOne(fetch=FetchType.LAZY)
     @JoinColumn(name="eventoId")
     private Evento evento;
     @Lob @Column(name="pdf")
     private byte[] pdf;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="signDate", length=23)
     private Date signDate;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="fechaCreacion", length=23)
     private Date dateCreated;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="lastUpdated", length=23)
     private Date lastUpdated;
     
     @Transient
     private TimeStampToken timeStampToken;
     
     public Documento() { }


     public Long getId() {
		return id;
     }


	public void setId(Long id) {
		this.id = id;
	}
	
	public Date getSignDate() {
		return signDate;
	}


	public void setSignDate(Date signDate) {
		this.signDate = signDate;
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

	public byte[] getPdf() {
		return pdf;
	}


	public void setPdf(byte[] pdf) {
		this.pdf = pdf;
	}

	public Evento getEvento() {
		return evento;
	}


	public void setEvento(Evento evento) {
		this.evento = evento;
	}


	public Estado getEstado() {
		return estado;
	}


	public void setEstado(Estado estado) {
		this.estado = estado;
	}


	public Usuario getUsuario() {
		return usuario;
	}


	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}


	public TimeStampToken getTimeStampToken() {
		return timeStampToken;
	}


	public void setTimeStampToken(TimeStampToken timeStampToken) {
		this.timeStampToken = timeStampToken;
	}


}