package org.controlacceso.clientegwt.client.modelo;

import org.controlacceso.clientegwt.client.HistoryToken;

public class SistemaVotacionQueryString {

	private HistoryToken historyToken;
	private Integer eventoId;
	private Integer representativeId;
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

	public Integer getRepresentativeId() {
		return representativeId;
	}

	public void setRepresentativeId(Integer representativeId) {
		this.representativeId = representativeId;
	}
	
}
