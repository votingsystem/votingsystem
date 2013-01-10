package org.sistemavotacion.controlacceso.modelo;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
@Entity
@Table(name="Voto")
public class Voto implements Serializable {
	
	public enum Estado{OK, ANULADO, OBSERVACION, ERROR}

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @OneToOne
    private MensajeSMIME mensajeSMIME;
    @OneToOne
    private Certificado certificado;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="opcionDeEventoId")
    private OpcionDeEvento opcionDeEvento;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoVotacionId")
    private EventoVotacion eventoVotacion;   
    @Enumerated(EnumType.STRING)
    @Column(name="estado", nullable=false)
    private Estado estado;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;
    
    @Transient
    private X509Certificate certificadoUsuario;

    @Transient
    private X509Certificate certificadoCentroControl;
    
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

	public void setOpcionDeEvento(OpcionDeEvento opcionDeEvento) {
		this.opcionDeEvento = opcionDeEvento;
	}

	public OpcionDeEvento getOpcionDeEvento() {
		return opcionDeEvento;
	}

	public MensajeSMIME getMensajeSMIME() {
		return mensajeSMIME;
	}

	public void setMensajeSMIME(MensajeSMIME mensajeSMIME) {
		this.mensajeSMIME = mensajeSMIME;
	}

	public Estado getEstado() {
		return estado;
	}

	public void setEstado(Estado estado) {
		this.estado = estado;
	}

	public Certificado getCertificado() {
		return certificado;
	}

	public void setCertificado(Certificado certificado) {
		this.certificado = certificado;
	}

}
