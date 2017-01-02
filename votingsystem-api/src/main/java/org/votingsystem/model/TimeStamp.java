package org.votingsystem.model;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.dto.metadata.MetaInfDto;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.model.converter.MetaInfConverter;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@Entity
@Table(name="TimeStamp")
public class TimeStamp extends EntityBase implements Serializable {

    private static Logger log = Logger.getLogger(TimeStamp.class.getName());
	
	private static final long serialVersionUID = 1L;

	public enum State {OK, CANCELED, ERROR}
	
    @Id
    @Column(name="SERIAL_NUMBER", unique=true, nullable=false)
    private Long serialNumber;
    @Column(name="TOKEN_BYTES") private byte[] tokenBytes;
    @Enumerated(EnumType.STRING)
    @Column(name="STATE")
    private State state;
	@Column(name="META_INF", columnDefinition="TEXT")
	@Convert(converter = MetaInfConverter.class)
	private MetaInfDto metaInf;
    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
	@Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;
   
    public TimeStamp() { }

    public TimeStamp(Long serialNumber, byte[] tokenBytes, State state) {
        this.serialNumber = serialNumber;
        this.tokenBytes = tokenBytes;
        this.state = state;
    }

	public Long getSerialNumber() {
		return serialNumber;
	}
	
	public void setSerialNumber(Long serialNumber) {
		this.serialNumber = serialNumber;
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

    public TimeStampToken getTimeStampToken() throws
            TSPException, IOException, CMSException {
        if(tokenBytes == null) return null;
        return new TimeStampToken(
                new CMSSignedData(tokenBytes));
    }

	public MetaInfDto getMetaInf() {
		return metaInf;
	}

	public TimeStamp setMetaInf(MetaInfDto metaInf) {
		this.metaInf = metaInf;
		return this;
	}

}
