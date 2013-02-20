package org.sistemavotacion.centrocontrol.modelo;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* Clase para tratar los mensajes firmados recibidos en el servidor
*/
@Entity
@Table(name="MensajeSMIME")
public class MensajeSMIME implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="tipo", nullable=false)
    private Tipo tipo;
    @Column(name="contenido")
    @Lob
    private byte[] contenido;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuarioId")
    private Usuario usuario;
    
    @OneToOne(mappedBy="mensajeMime")
    private EventoVotacion eventoVotacion;
    
    @OneToOne(mappedBy="mensajeSMIME")
    private Voto voto;
    
    //@OneToOne(mappedBy="mensajeSMIME")
    //private AnuladorVoto anuladorVoto;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="smimePadreId")
    private MensajeSMIME smimePadre;
    
    @Column(name="valido", nullable=false, columnDefinition="BOOLEAN default false")
    private Boolean valido;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23, insertable=true)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23, insertable=true)
    private Date lastUpdated;
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="smimePadre")
    private Set<MensajeSMIME> smimeHijosSet = new HashSet<MensajeSMIME>(0);  
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
     * @return the usuario
     */
    public Usuario getUsuario() {
        return usuario;
    }

    /**
     * @param usuario the usuario to set
     */
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
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

	public void setEventoVotacion(EventoVotacion eventoVotacion) {
		this.eventoVotacion = eventoVotacion;
	}

	public EventoVotacion getEventoVotacion() {
		return eventoVotacion;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public void setValido(Boolean valido) {
		this.valido = valido;
	}

	public Boolean getValido() {
		return valido;
	}

	public Voto getVoto() {
		return voto;
	}

	public void setVoto(Voto voto) {
		this.voto = voto;
	}

	public Set<MensajeSMIME> getSmimeHijosSet() {
		return smimeHijosSet;
	}

	public void setSmimeHijosSet(Set<MensajeSMIME> smimeHijosSet) {
		this.smimeHijosSet = smimeHijosSet;
	}

	public MensajeSMIME getSmimePadre() {
		return smimePadre;
	}

	public void setSmimePadre(MensajeSMIME smimePadre) {
		this.smimePadre = smimePadre;
	}

}
