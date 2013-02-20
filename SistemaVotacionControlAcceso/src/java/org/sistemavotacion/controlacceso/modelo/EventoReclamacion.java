package org.sistemavotacion.controlacceso.modelo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Column;

import org.hibernate.search.annotations.Indexed;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="EventoReclamacion")
@DiscriminatorValue("EventoReclamacion")
@Indexed
public class EventoReclamacion extends Evento implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name="cardinalidadRepresentaciones")
    private Cardinalidad cardinalidadRepresentaciones = Cardinalidad.UNA;   
    
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="evento")
    private Set<Firma> firmas = new HashSet<Firma>(0);
    
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="evento")
    private Set<CampoDeEvento> camposEvento = new HashSet<CampoDeEvento>(0);

    public void setFirmas(Set<Firma> firmas) {
        this.firmas = firmas;
    }

    public Set<Firma> getFirmas() {
        return firmas;
    }

    
    /**
     * @return the cardinalidadRepresentaciones
     */
    public Cardinalidad getCardinalidadRepresentaciones() {
        return cardinalidadRepresentaciones;
    }

    /**
     * @param cardinalidadRepresentaciones the cardinalidadRepresentaciones to set
     */
    public void setCardinalidadRepresentaciones(Cardinalidad cardinalidadRepresentaciones) {
        this.cardinalidadRepresentaciones = cardinalidadRepresentaciones;
    }
    
    
    /**
     * @return the camposEvento
     */
    public Set<CampoDeEvento> getCamposEvento() {
        return camposEvento;
    }

    /**
     * @param camposEvento the camposEvento to set
     */
    public void setCamposEvento(Set<CampoDeEvento> camposEvento) {
        this.camposEvento = camposEvento;
    }

}
