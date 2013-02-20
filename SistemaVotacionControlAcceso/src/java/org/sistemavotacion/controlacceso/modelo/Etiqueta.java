package org.sistemavotacion.controlacceso.modelo;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.GeneratedValue;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="Etiqueta")
public class Etiqueta  implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
	 
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Column(name="nombre", nullable=false, length=50) 
    private String nombre;
    @Column(name="frecuencia")
    private Long frecuencia;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;
    @ManyToMany(mappedBy = "etiquetaSet")
    private Set<Evento> eventoSet;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;

    public Etiqueta() { }
   
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNombre() {
        return this.nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setFrecuencia(Long frecuencia) {
        this.frecuencia = frecuencia;
    }

    public Long getFrecuencia() {
        return frecuencia;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

	public Set<Evento> getEventoSet() {
		return eventoSet;
	}

	public void setEventoSet(Set<Evento> eventoSet) {
		this.eventoSet = eventoSet;
	}

}