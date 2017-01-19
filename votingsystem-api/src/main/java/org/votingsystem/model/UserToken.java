package org.votingsystem.model;

import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="USER_TOKEN")
public class UserToken extends EntityBase implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum State {OK, CANCELLED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;
    @Column(name="STATE") @Enumerated(EnumType.STRING) private State state = State.OK;
    //user token encrypted with the server certificate
    @Column(name="TOKEN")
    private String token;
    @Column(name="AES_PARAMS_DTO")
    private String aesParamsDto;
    @Column(name="HTTP_SESSION_ID")
    private String httpSessionId;
    @OneToOne
    @JoinColumn(name="CERTIFICATE_ID")
    private Certificate certificate;
    @OneToOne
    @JoinColumn(name="USER_ID")
    private User user;
    @OneToOne
    @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;
    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    public UserToken() {}

    public UserToken(User user, String token, Certificate certificate, SignedDocument signedDocument) {
        this.user = user;
        this.token = token;
        this.certificate = certificate;
        this.signedDocument = signedDocument;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public void setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
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

    public State getState() {
        return state;
    }

    public UserToken setState(State state) {
        this.state = state;
        return this;
    }

    public String getAesParamsDto() {
        return aesParamsDto;
    }

    public UserToken setAesParamsDto(String aesParamsDto) {
        this.aesParamsDto = aesParamsDto;
        return this;

    }

    public String getHttpSessionId() {
        return httpSessionId;
    }

    public void setHttpSessionId(String httpSessionId) {
        this.httpSessionId = httpSessionId;
    }

}
