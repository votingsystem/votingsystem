package org.sistemavotacion.centrocontrol.modelo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.GeneratedValue;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
@Entity
@Table(name="EtiquetaEvento")
public class EtiquetaEvento  implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	@Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventoId", nullable=false)
    private EventoVotacion eventoVotacion;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="etiquetaId", nullable=false)
    private Etiqueta etiqueta;

    public EtiquetaEvento() { }

    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public EventoVotacion getEventoVotacion() {
        return this.eventoVotacion;
    }
    
    public void setEventoVotacion(EventoVotacion eventoVotacion) {
        this.eventoVotacion = eventoVotacion;
    }

    public Etiqueta getEtiqueta() {
        return this.etiqueta;
    }
    
    public void setEtiqueta(Etiqueta etiqueta) {
        this.etiqueta = etiqueta;
    }

}


