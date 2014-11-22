package org.votingsystem.model;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="ImageVS")
public class ImageVS implements java.io.Serializable {

	 private static final long serialVersionUID = 1L;
	 
	 public enum Type {REPRESENTATIVE, REPRESENTATIVE_CANCELLED}
	 
	 @Id @GeneratedValue(strategy=IDENTITY)
	 @Column(name="id", unique=true, nullable=false)
	 private Long id;
	 @Enumerated(EnumType.STRING)
	 @Column(name="type", nullable=false)
	 private Type type;
	 @ManyToOne(fetch=FetchType.LAZY)
	 @JoinColumn(name="userVS")
	 private UserVS userVS;
     @Lob @Column(name="fileBytes")
     private byte[] fileBytes;
     @OneToOne
     private MessageSMIME messageSMIME;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="dateCreated", length=23)
     private Date dateCreated;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="lastUpdated", length=23)
     private Date lastUpdated;
     
     public ImageVS() { }


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

	 public UserVS getUserVS() {
		return userVS;
	}


	 public void setUserVS(UserVS userVS) {
		this.userVS = userVS;
	}


	 public Type getType() {
		return type;
	}


	 public ImageVS setType(Type type) {
		this.type = type;
        return this;
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