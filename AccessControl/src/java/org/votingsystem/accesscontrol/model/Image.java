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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="Image")
public class Image implements java.io.Serializable {

	 private static final long serialVersionUID = 1L;
	 
	 public enum Type {REPRESENTATIVE, REPRESENTATIVE_CANCELLED}
	 
	 @Id @GeneratedValue(strategy=IDENTITY)
	 @Column(name="id", unique=true, nullable=false)
	 private Long id;
	 @Enumerated(EnumType.STRING)
	 @Column(name="type", nullable=false)
	 private Type type;
	 @ManyToOne(fetch=FetchType.LAZY)
	 @JoinColumn(name="usuarioId")
	 private Usuario usuario;
     @Lob @Column(name="fileBytes")
     private byte[] fileBytes;
     @OneToOne
     private MessageSMIME messageSMIME;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="fechaCreacion", length=23)
     private Date dateCreated;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="lastUpdated", length=23)
     private Date lastUpdated;
     
     public Image() { }


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

	public Usuario getUsuario() {
		return usuario;
	}


	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}


	public Type getType() {
		return type;
	}


	public void setType(Type type) {
		this.type = type;
	}


	public byte[] getFileBytes() {
		return fileBytes;
	}


	public void setFileBytes(byte[] fileBytes) {
		this.fileBytes = fileBytes;
	}


	public MessageSMIME getMessageSMIME() {
		return messageSMIME;
	}


	public void setMessageSMIME(MessageSMIME messageSMIME) {
		this.messageSMIME = messageSMIME;
	}


}