package org.sistemavotacion.modelo;

import java.io.Serializable;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class ControlAcceso extends ActorConIP implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private Evento evento;
    private String urlClientePublicacionJNLP;
    private Set<CentroControl> centrosDeControl;

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
     * @return the urlClientePublicacionJNLP
     */
    public String getURLClientePublicacionJNLP() {
        return urlClientePublicacionJNLP;
    }

    /**
     * @param urlClientePublicacionJNLP the urlClientePublicacionJNLP to set
     */
    public void setUrlClientePublicacionJNLP(String urlClientePublicacionJNLP) {
        this.urlClientePublicacionJNLP = urlClientePublicacionJNLP;
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
