package org.votingsystem.model.voting;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
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
    @OneToOne private CertificateVS certificateVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="device") private DeviceVS deviceVS;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @OneToOne private CMSMessage cancelationCMS;
    @OneToOne private CMSMessage activationCMS;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23, insertable=true) private Date lastUpdated;

    public UserRequestCsrVS () {}

    public UserRequestCsrVS (State state, byte[] content, DeviceVS deviceVS) {
        this.state = state;
        this.content = content;
        this.userVS = deviceVS.getUserVS();
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

	public UserRequestCsrVS setCertificateVS(CertificateVS certificateVS) {
		this.certificateVS = certificateVS;
        return this;
	}

	public State getState() {
		return state;
	}

	public UserRequestCsrVS setState(State state) {
		this.state = state;
        return this;
	}

    public CMSMessage getCancelationCMS() {
        return cancelationCMS;
    }

    public void setCancelationCMS(CMSMessage cancelationCMS) {
        this.cancelationCMS = cancelationCMS;
    }

    public CMSMessage getActivationCMS() {
        return activationCMS;
    }

    public UserRequestCsrVS setActivationCMS(CMSMessage activationCMS) {
        this.activationCMS = activationCMS;
        return this;
    }

	public DeviceVS getDeviceVS() {
		return deviceVS;
	}

	public void setDeviceVS(DeviceVS deviceVS) {
		this.deviceVS = deviceVS;
	}

	public UserVS getUserVS() {
		return userVS;
	}

	public void setUserVS(UserVS userVS) {
		this.userVS = userVS;
	}

}