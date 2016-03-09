package org.votingsystem.model;

import org.votingsystem.util.EntityVS;
import org.votingsystem.util.TypeVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="BackupRequestVS")
public class BackupRequestVS extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="type", nullable=false)
    private TypeVS type;
    @OneToOne
    @JoinColumn(name="cmsMessage")
    private CMSMessage cmsMessage;
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

    public BackupRequestVS(String filePath,  TypeVS type,  String email) {
        this.filePath = filePath;
        this.type = type;
        this.email = email;
    }

    public BackupRequestVS(String filePath, TypeVS type, UserVS representative, CMSMessage cmsMessage, String email) {
        this.filePath = filePath;
        this.type = type;
        this.representative = representative;
        this.cmsMessage = cmsMessage;
        this.email = email;
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

	public CMSMessage getCmsMessage() {
		return cmsMessage;
	}

	public void setCmsMessage(CMSMessage cmsMessage) {
		this.cmsMessage = cmsMessage;
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