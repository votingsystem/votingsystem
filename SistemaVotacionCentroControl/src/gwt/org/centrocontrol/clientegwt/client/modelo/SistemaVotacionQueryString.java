package org.centrocontrol.clientegwt.client.modelo;

import org.centrocontrol.clientegwt.client.HistoryToken;

public class SistemaVotacionQueryString {

	private HistoryToken historyToken;
	private Integer eventoId;
	private EventoSistemaVotacionJso.Estado estadoEvento;
	
	public SistemaVotacionQueryString() {}

	public EventoSistemaVotacionJso.Estado getEstadoEvento() {
		return estadoEvento;
	}

	public void setEstadoEvento(EventoSistemaVotacionJso.Estado estadoEvento) {
		this.estadoEvento = estadoEvento;
	}

	public Integer getEventoId() {
		return eventoId;
	}

	public void setEventoId(Integer eventoId) {
		this.eventoId = eventoId;
	}

	public HistoryToken getHistoryToken() {
		return historyToken;
	}

	public void setHistoryToken(HistoryToken historyToken) {
		this.historyToken = historyToken;
	}
	
	public String toString()  {
		return "[historyToken: " + historyToken + "- eventoId:" + eventoId 
				+ " - estadoEvento: " + estadoEvento + "]";
	}
	
}
