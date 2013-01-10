package org.sistemavotacion.centrocontrol.modelo;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.ManyToOne;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
@Entity
@Table(name="AnuladorVoto")
public class AnuladorVoto implements Serializable {
	
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @OneToOne
    private MensajeSMIME mensajeSMIME;
    @OneToOne
    private Certificado certificado;
    @OneToOne
    private Voto voto;
    @Column(name="hashCertificadoVotoBase64")
    private String hashCertificadoVotoBase64;    
    @Column(name="origenHashCertificadoVotoBase64")
    private String origenHashCertificadoVotoBase64;             
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

	public Certificado getCertificado() {
		return certificado;
	}

	public void setCertificado(Certificado certificado) {
		this.certificado = certificado;
	}

	public Voto getVoto() {
		return voto;
	}

	public void setVoto(Voto voto) {
		this.voto = voto;
	}

	public MensajeSMIME getMensajeSMIME() {
		return mensajeSMIME;
	}

	public void setMensajeSMIME(MensajeSMIME mensajeSMIME) {
		this.mensajeSMIME = mensajeSMIME;
	}

}
