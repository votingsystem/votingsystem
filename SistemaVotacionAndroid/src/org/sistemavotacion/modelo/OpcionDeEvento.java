package org.sistemavotacion.modelo;

import java.io.Serializable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class OpcionDeEvento implements Serializable {


	private static final long serialVersionUID = 1L;

	private Long id;
    private Evento evento;
    private String contenido;
    private int numeroVotos;

    public OpcionDeEvento () {}

    public OpcionDeEvento (OpcionDeEvento opcion) {
        this.contenido = opcion.getContenido();
        this.evento = opcion.getEvento();
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

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
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

	public void setNumeroVotos(int numeroVotos) {
		this.numeroVotos = numeroVotos;
	}

	public int getNumeroVotos() {
		return numeroVotos;
	}

}
