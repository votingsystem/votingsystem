package org.sistemavotacion.modelo;

import java.io.Serializable;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ControlAcceso extends ActorConIP implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private Evento evento;
    private Set<CentroControl> centrosDeControl;


    public ControlAcceso() {}

    public ControlAcceso(ActorConIP actorConIP) {
        setCertificado(actorConIP.getCertificado());
        setCertChain(actorConIP.getCertChain());
        setCertificadoPEM(actorConIP.getCertificadoPEM());
        setCertificadoURL(actorConIP.getCertificadoURL());
        setDateCreated(actorConIP.getDateCreated());
        setId(actorConIP.getId());
        setEstado(actorConIP.getEstado());
        setServerURL(actorConIP.getServerURL());
        setLastUpdated(actorConIP.getLastUpdated());
        setNombre(actorConIP.getNombre());
    }

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
     * @return the centrosDeControl
     */
    public Set<CentroControl> getCentrosDeControl() {
        return centrosDeControl;
    }

    /**
     * @param centrosDeControl the centrosDeControl to set
     */
    public void setCentrosDeControl(Set<CentroControl> centrosDeControl) {
        this.centrosDeControl = centrosDeControl;
    }
    
}
