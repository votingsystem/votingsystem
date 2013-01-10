package org.sistemavotacion.controlacceso.modelo;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.IOException;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;

@Entity
@Table(name="SelloTiempo")
public class SelloTiempo {
	
	private static final long serialVersionUID = 1L;
	
	public enum Estado {OK, ANULADO, ERRORES}
	
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="serialNumber", unique=true, nullable=false)
    private Long serialNumber;
    @Column(name="timeStampRequestBytes")
    @Lob private byte[] timeStampRequestBytes;
    @Column(name="tokenBytes")
    @Lob private byte[] tokenBytes;
    @Enumerated(EnumType.STRING)
    @Column(name="estado")
    private Estado estado;
    @Column(name="reason") 
    private String reason;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;
   
   public SelloTiempo() { }

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
	
	public Estado getEstado() {
		return estado;
	}
	
	public void setEstado(Estado estado) {
		this.estado = estado;
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

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
	
}
