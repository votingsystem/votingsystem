package org.sistemavotacion.modelo;


public class DatosBusqueda {
	
	private Tipo tipo;
	private Evento.Estado estadoEvento;
	private String textQuery;
	
	public DatosBusqueda(Tipo tipo, Evento.Estado estadoEvento, 
			String textQuery) {
		this.tipo = tipo;
		this.estadoEvento = estadoEvento;
		this.textQuery = textQuery;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public void setTipo(Tipo tipo) {
		this.tipo = tipo;
	}

	public Evento.Estado getEstadoEvento() {
		return estadoEvento;
	}

	public void setEstadoEvento(Evento.Estado estadoEvento) {
		this.estadoEvento = estadoEvento;
	}

	public String getTextQuery() {
		return textQuery;
	}

	public void setTextQuery(String textQuery) {
		this.textQuery = textQuery;
	}
	
}
