package org.votingsystem.model;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="BackupRequestVS")
public class BackupRequestVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;


    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @OneToOne
    @JoinColumn(name="documento")
    private org.votingsystem.model.PDFDocumentVS PDFDocumentVS;
    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable=false)
    private TypeVS type;
    @OneToOne
    @JoinColumn(name="messageSMIME")
    private MessageSMIME messageSMIME;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="representative")
    private UserVS representative;
    @Column(name="email")
    private String email;
    @Column(name="filePath")
    private String filePath;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23)
    private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23)
    private Date lastUpdated;
     
    public BackupRequestVS() { }

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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public PDFDocumentVS getPDFDocumentVS() {
		return PDFDocumentVS;
	}

	public void setPDFDocumentVS(PDFDocumentVS PDFDocumentVS) {
		this.PDFDocumentVS = PDFDocumentVS;
	}

	public MessageSMIME getMessageSMIME() {
		return messageSMIME;
	}

	public void setMessageSMIME(MessageSMIME messageSMIME) {
		this.messageSMIME = messageSMIME;
	}

	public UserVS getRepresentative() {
		return representative;
	}

	public void setRepresentative(UserVS representative) {
		this.representative = representative;
	}

	public TypeVS getType() {
		return type;
	}

	public void setType(TypeVS type) {
		this.type = type;
	}

}