package org.votingsystem.model;

import org.bouncycastle.tsp.TimeStampToken;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="PDFDocumentVS")
public class PDFDocumentVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public enum State {VALIDATED_MANIFEST, MANIFEST_SIGNATURE_VALIDATED, ERROR, BACKUP_REQUEST,
     BACKUP_REQUEST_ERROR, VALIDATED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVS") private EventVS eventVS;
    @Lob @Column(name="pdf") private byte[] pdf;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="signDate", length=23) private Date signDate;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Transient private TimeStampToken timeStampToken;
     
    public PDFDocumentVS() { }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getSignDate() {
        return signDate;
    }

    public void setSignDate(Date signDate) {
        this.signDate = signDate;
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

    public byte[] getPdf() {
    return pdf;
    }


    public void setPdf(byte[] pdf) {
        this.pdf = pdf;
    }

    public EventVS getEventVS() {
        return eventVS;
    }


    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }


    public State getState() {
        return state;
    }


    public void setState(State state) {
        this.state = state;
    }


    public UserVS getUserVS() {
        return userVS;
    }


    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }


    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }


    public void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }


}