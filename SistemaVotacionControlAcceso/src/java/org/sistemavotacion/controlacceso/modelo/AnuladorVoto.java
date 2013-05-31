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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.sistemavotacion.controlacceso.modelo.Certificado.Estado;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="AnuladorVoto")
public class AnuladorVoto implements Serializable {
	
	public enum Estado {SIN_NOTIFICAR, NOTIFICADO, ANULADO, ERROR}
	
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @OneToOne
    private MensajeSMIME mensajeSMIME;
    @OneToOne
    private SolicitudAcceso solicitudAcceso;
    @OneToOne
    private SolicitudCSRVoto solicitudCSRVoto;
    @OneToOne
    private Voto voto;    
    @Enumerated(EnumType.STRING)
    @Column(name="estado", nullable=false)
    private Estado estado;    
    @Column(name="hashCertificadoVotoBase64")
    private String hashCertificadoVotoBase64;    
    @Column(name="origenHashCertificadoVotoBase64")
    private String origenHashCertificadoVotoBase64;     
    @Column(name="hashSolicitudAccesoBase64")
    private String hashSolicitudAccesoBase64;
    @Column(name="origenHashSolicitudAccesoBase64")
    private String origenHashSolicitudAccesoBase64;          
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoVotacionId", nullable=false)
    private EventoVotacion eventoVotacion;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;

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

	public void setMensajeSMIME(MensajeSMIME mensajeSMIME) {
		this.mensajeSMIME = mensajeSMIME;
	}

	public MensajeSMIME getMensajeSMIME() {
		return mensajeSMIME;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setEventoVotacion(EventoVotacion eventoVotacion) {
		this.eventoVotacion = eventoVotacion;
	}

	public EventoVotacion getEventoVotacion() {
		return eventoVotacion;
	}

	public SolicitudAcceso getSolicitudAcceso() {
		return solicitudAcceso;
	}

	public void setSolicitudAcceso(SolicitudAcceso solicitudAcceso) {
		this.solicitudAcceso = solicitudAcceso;
	}

	public String getHashCertificadoVotoBase64() {
		return hashCertificadoVotoBase64;
	}

	public void setHashCertificadoVotoBase64(String hashCertificadoVotoBase64) {
		this.hashCertificadoVotoBase64 = hashCertificadoVotoBase64;
	}

	public String getOrigenHashCertificadoVotoBase64() {
		return origenHashCertificadoVotoBase64;
	}

	public void setOrigenHashCertificadoVotoBase64(
			String origenHashCertificadoVotoBase64) {
		this.origenHashCertificadoVotoBase64 = origenHashCertificadoVotoBase64;
	}

	public String getHashSolicitudAccesoBase64() {
		return hashSolicitudAccesoBase64;
	}

	public void setHashSolicitudAccesoBase64(String hashSolicitudAccesoBase64) {
		this.hashSolicitudAccesoBase64 = hashSolicitudAccesoBase64;
	}

	public String getOrigenHashSolicitudAccesoBase64() {
		return origenHashSolicitudAccesoBase64;
	}

	public void setOrigenHashSolicitudAccesoBase64(
			String origenHashSolicitudAccesoBase64) {
		this.origenHashSolicitudAccesoBase64 = origenHashSolicitudAccesoBase64;
	}

	public SolicitudCSRVoto getSolicitudCSRVoto() {
		return solicitudCSRVoto;
	}

	public void setSolicitudCSRVoto(SolicitudCSRVoto solicitudCSR) {
		this.solicitudCSRVoto = solicitudCSR;
	}

	public Voto getVoto() {
		return voto;
	}

	public void setVoto(Voto voto) {
		this.voto = voto;
	}

	public Estado getEstado() {
		return estado;
	}

	public void setEstado(Estado estado) {
		this.estado = estado;
	}


}
