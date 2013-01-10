package org.sistemavotacion.controlacceso.modelo;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
@Entity
@Table(name="SolicitudCSRUsuario")
public class SolicitudCSRUsuario implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public enum Estado {OK, PENDIENTE_APROVACION, RECHAZADA, ANULADA}
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;

    @Column(name="numeroSerie")
    private Long numeroSerie;
   
    @Column(name="contenido", nullable=false)
    @Lob
    private byte[] contenido;
    
    @OneToOne(mappedBy="solicitudCSRUsuario")
    private Certificado certificado;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="dispositivoId")
    private Dispositivo dispositivo;
    
    @Enumerated(EnumType.STRING)
    @Column(name="estado", nullable=false)
    private Estado estado; 
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuarioId")
    private Usuario usuario;
    
    @OneToOne
    private MensajeSMIME cancelacion;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23, insertable=true)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23, insertable=true)
    private Date lastUpdated;

    /**
     * @return the contenido
     */
    public byte[] getContenido() {
        return contenido;
    }

    /**
     * @param contenido the contenido to set
     */
    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    /**
     * @return the id
     */
    public Long getNumeroSerie() {
        return numeroSerie;
    }

    /**
     * @param id the id to set
     */
    public void setNumeroSerie(Long numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public Certificado getCertificado() {
		return certificado;
	}

	public void setCertificado(Certificado certificado) {
		this.certificado = certificado;
	}

	public Estado getEstado() {
		return estado;
	}

	public void setEstado(Estado estado) {
		this.estado = estado;
	}
	
	public Dispositivo getDispositivo() {
		return dispositivo;
	}

	public void setDispositivo(Dispositivo dispositivo) {
		this.dispositivo = dispositivo;
	}

	public MensajeSMIME getCancelacion() {
		return cancelacion;
	}

	public void setCancelacion(MensajeSMIME cancelacion) {
		this.cancelacion = cancelacion;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

}
