package org.votingsystem.model;

import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="GET_SESSION_CERTIFICATION")
public class SessionCertification extends EntityBase implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue(strategy=IDENTITY)
	@Column(name="ID", unique=true, nullable=false)
	private Long id;
	@OneToOne @JoinColumn(name="MOBILE_CERTIFICATE_ID")
	private Certificate mobileCertificate;
	@OneToOne @JoinColumn(name="BROWSER_CERTIFICATE_ID")
	private Certificate browserCertificate;
	@ManyToOne @JoinColumn(name = "SIGNER_ID")
	private User user;
	@OneToOne @JoinColumn(name="SIGNED_DOCUMENT")
	private SignedDocument signedDocument;
	@Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
	private LocalDateTime dateCreated;
	@Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
	private LocalDateTime lastUpdated;

     public SessionCertification() { }

     public SessionCertification(User user, Certificate mobileCertificate, Certificate browserCertificate,
								 SignedDocument signedDocument) {
     	this.user = user;
     	this.mobileCertificate = mobileCertificate;
     	this.browserCertificate = browserCertificate;
     	this.signedDocument = signedDocument;
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

     public SignedDocument getSignedDocument() {
		return signedDocument;
	}

	 public void setSignedDocument(SignedDocument signedDocument) {
		this.signedDocument = signedDocument;
	}

	public Certificate getMobileCertificate() {
		return mobileCertificate;
	}

	public void setMobileCertificate(Certificate mobileCertificate) {
		this.mobileCertificate = mobileCertificate;
	}

	public Certificate getBrowserCertificate() {
		return browserCertificate;
	}

	public void setBrowserCertificate(Certificate browserCertificate) {
		this.browserCertificate = browserCertificate;
	}
}