package org.sistemavotacion.modelo;

import java.io.Serializable;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CentroControl extends ActorConIP implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private Evento evento;
    private Set<ControlAcceso> controlesDeAcceso;

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Evento getEvento() {
        return evento;
    }
        
    @Override
    public Tipo getTipo() {
        return Tipo.CENTRO_CONTROL;
    }

    /**
     * @return the controlesDeAcceso
     */
    public Set<ControlAcceso> getControlesDeAcceso() {
        return controlesDeAcceso;
    }

    /**
     * @param controlesDeAcceso the controlesDeAcceso to set
     */
    public void setControlesDeAcceso(Set<ControlAcceso> controlesDeAcceso) {
        this.controlesDeAcceso = controlesDeAcceso;
    }

}
