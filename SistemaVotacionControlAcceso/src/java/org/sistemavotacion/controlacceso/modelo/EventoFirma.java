package org.sistemavotacion.controlacceso.modelo;

import java.io.Serializable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity
@Table(name="EventoFirma")
@DiscriminatorValue("EventoFirma")
@Indexed
public class EventoFirma extends Evento implements Serializable {

    private static final long serialVersionUID = 1L;


}
