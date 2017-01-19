package org.votingsystem.model.currency;

import org.votingsystem.model.EntityBase;
import org.votingsystem.model.User;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="CURRENCY_REQUEST_BATCH")
public class CurrencyRequestBatch extends EntityBase implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum State { OK, ERROR;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false) private Long id;

    @Column(name="STATE", nullable=false) @Enumerated(EnumType.STRING)
    private State state;

    @Column(name="REASON")
    private String reason;

    @Column(name="CONTENT")
    private byte[] content;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="USER_ID")
    private User user;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="TAG", nullable=false)
    private Tag tag;

    @Column(name="TIME_LIMITED", nullable=false)
    private Boolean timeLimited;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;

    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    public CurrencyRequestBatch() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    @Override
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}