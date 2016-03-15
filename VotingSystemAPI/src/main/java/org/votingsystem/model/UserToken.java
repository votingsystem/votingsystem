package org.votingsystem.model;

import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="UserToken")
public class UserToken extends EntityVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum State {OK, CANCELLED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="state") @Enumerated(EnumType.STRING) private State state = State.OK;
    //user token encrypted with the server certificate
    @Column(name="token") private byte[] token;
    @OneToOne private CertificateVS certificateVS;
    @OneToOne private User user;
    @OneToOne private CMSMessage cmsMessage;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public UserToken() {}

    public UserToken(User user, byte[] token, CertificateVS certificateVS, CMSMessage cmsMessage) {
        this.user = user;
        this.token = token;
        this.certificateVS = certificateVS;
        this.cmsMessage = cmsMessage;
    }

    public byte[] getToken() {
        return token;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

    public CertificateVS getCertificateVS() {
        return certificateVS;
    }

    public void setCertificateVS(CertificateVS certificateVS) {
        this.certificateVS = certificateVS;
    }

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }

    public void setCmsMessage(CMSMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public State getState() {
        return state;
    }

    public UserToken setState(State state) {
        this.state = state;
        return this;
    }

}
