package org.sistemavotacion.modelo;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class EventoEtiqueta  implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Evento evento;
    private Etiqueta etiqueta;

    public EventoEtiqueta() { }

    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public Evento getEvento() {
        return this.evento;
    }
    
    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Etiqueta getEtiqueta() {
        return this.etiqueta;
    }
    
    public void setEtiqueta(Etiqueta etiqueta) {
        this.etiqueta = etiqueta;
    }

}


