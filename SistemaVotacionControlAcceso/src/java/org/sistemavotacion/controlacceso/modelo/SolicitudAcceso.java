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
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="SolicitudAcceso")
public class SolicitudAcceso implements Serializable {
	
    public enum Estado {OK, ANULADO, OBSERVACION, ERROR}
	
    private static final long serialVersionUID = 1L; 
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="estado", nullable=false)
    private Estado estado;       
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoVotacionId")
    private EventoVotacion eventoVotacion;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuarioId")
    private Usuario usuario;    
    @OneToOne
    private MensajeSMIME mensajeSMIME; 
    @OneToOne(mappedBy="solicitudAcceso")
    private AnuladorVoto anuladorVoto; 
    @Column(name="hashSolicitudAccesoBase64")
    private String hashSolicitudAccesoBase64;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;

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

	public void setEstado(Estado estado) {
		this.estado = estado;
	}

	public Estado getEstado() {
		return estado;
	}

	public String getHashSolicitudAccesoBase64() {
		return hashSolicitudAccesoBase64;
	}

	public void setHashSolicitudAccesoBase64(
			String hashSolicitudAccesoBase64) {
		this.hashSolicitudAccesoBase64 = hashSolicitudAccesoBase64;
	}

	public MensajeSMIME getMensajeSMIME() {
		return mensajeSMIME;
	}

	public void setMensajeSMIME(MensajeSMIME mensajeSMIME) {
		this.mensajeSMIME = mensajeSMIME;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public AnuladorVoto getAnuladorVoto() {
		return anuladorVoto;
	}

	public void setAnuladorVoto(AnuladorVoto anuladorVoto) {
		this.anuladorVoto = anuladorVoto;
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

}
