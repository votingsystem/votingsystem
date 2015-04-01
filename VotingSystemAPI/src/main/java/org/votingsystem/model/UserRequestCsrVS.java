package org.votingsystem.model;

import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="UserRequestCsrVS")
public class UserRequestCsrVS extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public enum State {OK, PENDING, REJECTED, CANCELED}
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="serialNumber") private Long serialNumber;
    @Column(name="content", nullable=false) private byte[] content;
    @OneToOne(mappedBy="userRequestCsrVS") private CertificateVS certificateVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="device") private DeviceVS deviceVS;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @OneToOne private MessageSMIME cancelMessage;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23, insertable=true) private Date lastUpdated;

    public UserRequestCsrVS () {}

    public UserRequestCsrVS (State state, byte[] content, UserVS userVS, DeviceVS deviceVS) {
        this.state = state;
        this.content = content;
        this.userVS = userVS;
        this.deviceVS = deviceVS;
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

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public CertificateVS getCertificateVS() {
		return certificateVS;
	}

	public void setCertificateVS(CertificateVS certificateVS) {
		this.certificateVS = certificateVS;
	}

	public State getState() {
		return state;
	}

	public UserRequestCsrVS setState(State state) {
		this.state = state;
        return this;
	}
	
	public DeviceVS getDeviceVS() {
		return deviceVS;
	}

	public void setDeviceVS(DeviceVS deviceVS) {
		this.deviceVS = deviceVS;
	}

	public MessageSMIME getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(MessageSMIME cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public UserVS getUserVS() {
		return userVS;
	}

	public void setUserVS(UserVS userVS) {
		this.userVS = userVS;
	}

}