package org.sistemavotacion.modelo;

import java.io.Serializable;
import java.util.Date;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class AnuladorVoto {
	
	private static final long serialVersionUID = 1L;

	private Long id;
	private Evento evento;
	private MensajeMime mensajeMime;
	private String origenHashTokenAcceso;

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

	public void setOrigenHashTokenAcceso(String origenHashTokenAcceso) {
		this.origenHashTokenAcceso = origenHashTokenAcceso;
	}

	public String getOrigenHashTokenAcceso() {
		return origenHashTokenAcceso;
	}
	
}
