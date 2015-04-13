package org.votingsystem.model;

import org.votingsystem.model.voting.EventVS;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity @Table(name="KeyStoreVS")
public class KeyStoreVS extends EntityVS implements Serializable {
	
	private static final long serialVersionUID = 1L;
	 
	@Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
	
    @Column(name="valid", nullable=false) private Boolean valid;
    
    @Column(name="isRoot", nullable=false) private Boolean isRoot;
    
    @OneToOne private EventVS eventVS;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validFrom", length=23) private Date validFrom;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;

    @Column(name="keyAlias", nullable=false, length=50) private String keyAlias;

    @Column(name="bytes") private byte[] bytes;

    @Transient private String password;
    @Transient private String rootKeyAlias;
    @Transient private CertificateVS certificateVS;

    public KeyStoreVS() { }

    public KeyStoreVS(boolean isRoot, boolean valid, String keyAlias, EventVS eventVS, Date validFrom, Date validTo,
                      byte[] bytes) {
        this.isRoot = isRoot;
        this.valid = valid;
        this.keyAlias = keyAlias;
        this.eventVS = eventVS;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.bytes = bytes;
    }

    public Long getId() {
       return this.id;
   }

    public void setId(Long id) {
       this.id = id;
   }

    public byte[] getBytes() {
       return this.bytes;
   }

    public void setBytes(byte[] bytes) {
       this.bytes = bytes;
   }

	public void setValid(Boolean valid) {
		this.valid = valid;
	}

	public Boolean getValid() {
		return valid;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setIsRoot(Boolean isRoot) {
		this.isRoot = isRoot;
	}

	public Boolean getIsRoot() {
		return isRoot;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	public Date getValidFrom() {
		return validFrom;
	}

	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}

	public Date getValidTo() {
		return validTo;
	}

	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}

	public String getKeyAlias() {
		return keyAlias;
	}

	public EventVS getEventVS() {
		return eventVS;
	}

	public void setEventVS(EventVS eventVS) {
		this.eventVS = eventVS;
	}

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRootKeyAlias() {
        return rootKeyAlias;
    }

    public void setRootKeyAlias(String rootKeyAlias) {
        this.rootKeyAlias = rootKeyAlias;
    }

    public CertificateVS getCertificateVS() {
        return certificateVS;
    }

    public void setCertificateVS(CertificateVS certificateVS) {
        this.certificateVS = certificateVS;
    }
}
