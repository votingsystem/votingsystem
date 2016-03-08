package org.votingsystem.model;

import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="ImageVS")
public class ImageVS extends EntityVS implements Serializable {

	 private static final long serialVersionUID = 1L;
	 
	 public enum Type {REPRESENTATIVE, REPRESENTATIVE_CANCELED}
	 
	 @Id @GeneratedValue(strategy=IDENTITY)
	 @Column(name="id", unique=true, nullable=false)
	 private Long id;
	 @Enumerated(EnumType.STRING)
	 @Column(name="type", nullable=false)
	 private Type type;
	 @ManyToOne(fetch=FetchType.LAZY)
	 @JoinColumn(name="userVS")
	 private UserVS userVS;
     @Column(name="fileBytes")
     private byte[] fileBytes;
     @OneToOne
     private MessageCMS messageCMS;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="dateCreated", length=23)
     private Date dateCreated;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="lastUpdated", length=23)
     private Date lastUpdated;
     
     public ImageVS() { }

    public ImageVS(UserVS userVS, MessageCMS messageCMS, Type type, byte[] fileBytes) {
        this.userVS = userVS;
        this.messageCMS = messageCMS;
        this.type = type;
        this.fileBytes = fileBytes;
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

     public MessageCMS getMessageCMS() {
		return messageCMS;
	}

	 public void setMessageCMS(MessageCMS messageCMS) {
		this.messageCMS = messageCMS;
	}

}