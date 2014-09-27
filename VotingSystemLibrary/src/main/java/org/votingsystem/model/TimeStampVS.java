package org.votingsystem.model;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;

import javax.persistence.*;
import java.io.IOException;
import java.util.Date;

@Entity
@Table(name="TimeStampVS")
public class TimeStampVS {
	
	private static final long serialVersionUID = 1L;
	
	public enum State {OK, CANCELLED, ERROR}
	
    @Id
    @Column(name="serialNumber", unique=true, nullable=false)
    private Long serialNumber;
    @Column(name="timeStampRequestBytes")
    @Lob private byte[] timeStampRequestBytes;
    @Column(name="tokenBytes")
    @Lob private byte[] tokenBytes;
    @Enumerated(EnumType.STRING)
    @Column(name="state")
    private State state;
    @Column(name="reason") 
    private String reason;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23)
    private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23)
    private Date lastUpdated;
   
    public TimeStampVS() { }

	public Long getSerialNumber() {
		return serialNumber;
	}
	
	public void setSerialNumber(Long serialNumber) {
		this.serialNumber = serialNumber;
	}
	
	public byte[] getTimeStampRequestBytes() {
		return timeStampRequestBytes;
	}
	
	public void setTimeStampRequestBytes(byte[] timeStampRequestBytes) {
		this.timeStampRequestBytes = timeStampRequestBytes;
	}
	
	public byte[] getTokenBytes() {
		return tokenBytes;
	}
	
	public void setTokenBytes(byte[] tokenBytes) {
		this.tokenBytes = tokenBytes;
	}
	
	public State getState() {
		return state;
	}
	
	public void setState(State state) {
		this.state = state;
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

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

    public TimeStampToken getTimeStampToken() throws
            TSPException, IOException, CMSException {
        if(tokenBytes == null) return null;
        return new TimeStampToken(
                new CMSSignedData(tokenBytes));
    }

    public TimeStampRequest getTimeStampRequest() throws IOException {
        if(timeStampRequestBytes == null) return null;
        return new TimeStampRequest(timeStampRequestBytes);
    }

}
