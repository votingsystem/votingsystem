package org.sistemavotacion.controlacceso.modelo;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="AlmacenClaves")
public class AlmacenClaves implements Serializable {
	
	private static final long serialVersionUID = 1L;
	 
	@Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
	
    @Column(name="activo", nullable=false)
    private Boolean activo;
    
    @Column(name="esRaiz", nullable=false)
    private Boolean esRaiz;
    
    @OneToOne
    private Evento evento;  
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="validoDesde", length=23)
    private Date validoDesde;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="validoHasta", length=23)
    private Date validoHasta;
    
    @Column(name="keyAlias", nullable=false, length=50)
    private String keyAlias;
    
    @Column(name="bytes")
    @Lob
    private byte[] bytes;
    
    
   
   public AlmacenClaves() { }
  
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

	public void setActivo(Boolean activo) {
		this.activo = activo;
	}
	
	public Boolean getActivo() {
		return activo;
	}
	
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public Date getDateCreated() {
		return dateCreated;
	}
	
	public void setEsRaiz(Boolean esRaiz) {
		this.esRaiz = esRaiz;
	}
	
	public Boolean getEsRaiz() {
		return esRaiz;
	}
	
	private void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	private Date getLastUpdated() {
		return lastUpdated;
	}
	
	public void setValidoDesde(Date validoDesde) {
		this.validoDesde = validoDesde;
	}
	
	public Date getValidoDesde() {
		return validoDesde;
	}
	
	public void setValidoHasta(Date validoHasta) {
		this.validoHasta = validoHasta;
	}
	
	public Date getValidoHasta() {
		return validoHasta;
	}
	
	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}
	
	public String getKeyAlias() {
		return keyAlias;
	}

	public Evento getEvento() {
		return evento;
	}

	public void setEvento(Evento evento) {
		this.evento = evento;
	}

}
