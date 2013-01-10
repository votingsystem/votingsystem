package org.sistemavotacion.modelo;

import java.io.Serializable;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class ConsultaVoto implements Serializable  {
	
    private static final long serialVersionUID = 1L;
    private Long id;
    private Evento evento;
    private AlmacenClaves tokenAcceso;
    private MensajeMime mensajeMime;

    public void setMensajeMime(MensajeMime mensajeMime) {
        this.mensajeMime = mensajeMime;
    }

    public MensajeMime getMensajeMime() {
        return mensajeMime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setTokenAcceso(AlmacenClaves tokenAcceso) {
        this.tokenAcceso = tokenAcceso;
    }

    public AlmacenClaves getTokenAcceso() {
        return tokenAcceso;
    }
	
}
