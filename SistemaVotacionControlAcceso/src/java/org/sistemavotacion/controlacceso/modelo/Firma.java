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

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
@Entity
@Table(name="Firma")
public class Firma implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="tipo", nullable=false)
    private Tipo tipo;
    @OneToOne
    private MensajeSMIME mensajeSMIME;  
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuarioId")
    private Usuario usuario;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoId")
    private Evento evento;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;

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

    public void setUsuario(Usuario usuario) {
            this.usuario = usuario;
    }

    public Usuario getUsuario() {
            return usuario;
    }

    public void setId(Long id) {
            this.id = id;
    }

    public Long getId() {
            return id;
    }

    public void setMensajeSMIME(MensajeSMIME mensajeSMIME) {
    	this.mensajeSMIME = mensajeSMIME;
    }

    public MensajeSMIME getMensajeSMIME() {
        return mensajeSMIME;
    }

    /**
     * @return the evento
     */
    public Evento getEvento() {
        return evento;
    }

    /**
     * @param eventoFirma the evento to set
     */
    public void setEvento(Evento evento) {
        this.evento = evento;
    }

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

}
