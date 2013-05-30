package org.sistemavotacion.controlacceso.modelo;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
@Entity
@Table(name="EventoFirma")
@DiscriminatorValue("EventoFirma")
@Indexed
public class EventoFirma extends Evento implements Serializable {

    private static final long serialVersionUID = 1L;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="evento")
    private Set<Firma> firmas;

    public void setFirmas(Set<Firma> firmas) {
        this.firmas = firmas;
    }

    public Set<Firma> getFirmas() {
        return firmas;
    }

}
