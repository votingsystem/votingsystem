package org.votingsystem.model;

import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.util.OperationType;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="BACKUP_REQUEST")
public class BackupRequest extends EntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="TYPE", nullable=false)
    private OperationType type;
    @OneToOne
    @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;
    @Column(name="EMAIL")
    private String email;
    @Column(name="FILE_PATH")
    private String filePath;
	@Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
	private LocalDateTime dateCreated;
	@Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
	private LocalDateTime lastUpdated;
     
    public BackupRequest() { }

    public BackupRequest(String filePath, OperationType type, String email) {
        this.filePath = filePath;
        this.type = type;
        this.email = email;
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

	public SignedDocument getSignedDocument() {
		return signedDocument;
	}

	public void setSignedDocument(SignedDocument signedDocument) {
		this.signedDocument = signedDocument;
	}

	public OperationType getType() {
		return type;
	}

	public void setType(OperationType type) {
		this.type = type;
	}

}