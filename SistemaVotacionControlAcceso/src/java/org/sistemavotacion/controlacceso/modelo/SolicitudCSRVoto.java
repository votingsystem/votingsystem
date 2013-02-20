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
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="SolicitudCSRVoto")
public class SolicitudCSRVoto implements Serializable {

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
    
    @OneToOne(mappedBy="solicitudCSRVoto")
    private Certificado certificado;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoVotacionId")
    private EventoVotacion eventoVotacion;
    
    @Enumerated(EnumType.STRING)
    @Column(name="estado", nullable=false)
    private Estado estado; 
    
    @OneToOne(mappedBy="solicitudCSRVoto")
    private AnuladorVoto anuladorVoto; 

    @Column(name="hashCertificadoVotoBase64")
    private String hashCertificadoVotoBase64;
    
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

	public EventoVotacion getEventoVotacion() {
		return eventoVotacion;
	}

	public void setEventoVotacion(EventoVotacion eventoVotacion) {
		this.eventoVotacion = eventoVotacion;
	}

	public AnuladorVoto getAnuladorVoto() {
		return anuladorVoto;
	}

	public void setAnuladorVoto(AnuladorVoto anuladorVoto) {
		this.anuladorVoto = anuladorVoto;
	}

	public String getHashCertificadoVotoBase64() {
		return hashCertificadoVotoBase64;
	}

	public void setHashCertificadoVotoBase64(String hashCertificadoVotoBase64) {
		this.hashCertificadoVotoBase64 = hashCertificadoVotoBase64;
	}

}
