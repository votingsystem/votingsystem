package org.votingsystem.model;

import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="IMAGE")
public class Image extends EntityBase implements Serializable {

	 private static final long serialVersionUID = 1L;
	 
	 public enum Type {REPRESENTATIVE, REPRESENTATIVE_CANCELED}
	 
	@Id @GeneratedValue(strategy=IDENTITY)
	@Column(name="ID", unique=true, nullable=false)
	private Long id;
	@Enumerated(EnumType.STRING)
	@Column(name="TYPE", nullable=false)
	private Type type;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="USER_ID")
	private User user;
	@Column(name="CONTENT")
	private byte[] content;
	@OneToOne
	private SignedDocument signedDocument;
	@Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
	private LocalDateTime dateCreated;
	@Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
	private LocalDateTime lastUpdated;
     
     public Image() { }

    public Image(User user, SignedDocument signedDocument, Type type, byte[] content) {
        this.user = user;
        this.signedDocument = signedDocument;
        this.type = type;
        this.content = content;
    }

     public Long getId() {
		return id;
     }


	 public void setId(Long id) {
		this.id = id;
	}
	
	 public LocalDateTime getDateCreated() {
		return dateCreated;
	}


	 public void setDateCreated(LocalDateTime dateCreated) {
		this.dateCreated = dateCreated;
	}


	 public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}


	 public void setLastUpdated(LocalDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	 public User getUser() {
		return user;
	}


	 public void setUser(User user) {
		this.user = user;
	}


	 public Type getType() {
		return type;
	}


	 public Image setType(Type type) {
		this.type = type;
        return this;
    }


	 public byte[] getContent() {
		return content;
	}

	 public void setContent(byte[] content) {
		this.content = content;
	}

     public SignedDocument getSignedDocument() {
		return signedDocument;
	}

	 public void setSignedDocument(SignedDocument signedDocument) {
		this.signedDocument = signedDocument;
	}

}