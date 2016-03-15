package org.votingsystem.model.voting;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="UserRequestCsr")
public class UserRequestCsr extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum State {OK, PENDING, REJECTED, CANCELED}
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="serialNumber") private Long serialNumber;
    @Column(name="content", nullable=false) private byte[] content;
    @OneToOne private CertificateVS certificateVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="device") private Device device;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userId") private User user;
    @OneToOne private CMSMessage cancelationCMS;
    @OneToOne private CMSMessage activationCMS;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23, insertable=true) private Date lastUpdated;

    public UserRequestCsr() {}

    public UserRequestCsr(State state, byte[] content, Device device) {
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

	public UserRequestCsr setCertificateVS(CertificateVS certificateVS) {
		this.certificateVS = certificateVS;
        return this;
	}

	public State getState() {
		return state;
	}

	public UserRequestCsr setState(State state) {
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

    public UserRequestCsr setActivationCMS(CMSMessage activationCMS) {
        this.activationCMS = activationCMS;
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