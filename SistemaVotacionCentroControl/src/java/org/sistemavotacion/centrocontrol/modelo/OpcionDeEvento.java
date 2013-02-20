package org.sistemavotacion.centrocontrol.modelo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="OpcionDeEvento")
public class OpcionDeEvento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    
    @Column(name="opcionDeEventoId",nullable=false)
    private String opcionDeEventoId;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoVotacionId", nullable=false)
    private EventoVotacion eventoVotacion;
    @Column(name="contenido", length=10000, nullable=false)
    private String contenido;
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="opcionDeEvento")
    private Set<Voto> votos = new HashSet<Voto>(0);
    
    public OpcionDeEvento () {}

    public OpcionDeEvento (OpcionDeEvento opcion) {
        this.contenido = opcion.getContenido();
        this.id = opcion.getId();
    }

    /**
     * @return the contenido
     */
    public String getContenido() {
        return contenido;
    }

    /**
     * @param contenido the contenido to set
     */
    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public EventoVotacion getEventoVotacion() {
        return eventoVotacion;
    }

    public void setEventoVotacion(EventoVotacion eventoVotacion) {
        this.eventoVotacion = eventoVotacion;
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

	public void setOpcionDeEventoId(String opcionDeEventoId) {
		this.opcionDeEventoId = opcionDeEventoId;
	}

	public String getOpcionDeEventoId() {
		return opcionDeEventoId;
	}

	public void setVotos(Set<Voto> votos) {
		this.votos = votos;
	}

	public Set<Voto> getVotos() {
		return votos;
	}


}
