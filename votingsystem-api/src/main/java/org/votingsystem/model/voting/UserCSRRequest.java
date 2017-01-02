package org.votingsystem.model.voting;

import org.votingsystem.model.*;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="USER_CSR_REQUEST")
public class UserCSRRequest extends EntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum State {OK, PENDING, REJECTED, CANCELED}
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;
    @Column(name="SERIAL_NUMBER")
    private Long serialNumber;
    @Column(name="CONTENT", nullable=false)
    private byte[] content;
    @OneToOne
    @JoinColumn(name="CERTIFICATE_ID")
    private Certificate certificate;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="DEVICE_ID")
    private Device device;
    @Column(name="STATE", nullable=false)
    @Enumerated(EnumType.STRING) private State state;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    private User user;
    @OneToOne
    @JoinColumn(name="CANCELATION_DOCUMENT_ID")
    private SignedDocument cancelationDocument;
    @OneToOne
    @JoinColumn(name="ACTIVATION_DOCUMENT_ID")
    private SignedDocument activationDocument;
    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    public UserCSRRequest() {}

    public UserCSRRequest(State state, byte[] content, Device device) {
        this.state = state;
        this.content = content;
        this.user = device.getUser();
        this.device = device;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
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

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public Certificate getCertificate() {
		return certificate;
	}

	public UserCSRRequest setCertificate(Certificate certificate) {
		this.certificate = certificate;
        return this;
	}

	public State getState() {
		return state;
	}

	public UserCSRRequest setState(State state) {
		this.state = state;
        return this;
	}

    public SignedDocument getCancelationDocument() {
        return cancelationDocument;
    }

    public void setCancelationDocument(SignedDocument cancelationCMS) {
        this.cancelationDocument = cancelationCMS;
    }

    public SignedDocument getActivationDocument() {
        return activationDocument;
    }

    public UserCSRRequest setActivationDocument(SignedDocument activationCMS) {
        this.activationDocument = activationCMS;
        return this;
    }

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}