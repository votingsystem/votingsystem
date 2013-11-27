package org.votingsystem.android.model;

import org.votingsystem.model.EventVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class VoteCancellation {
	
	private static final long serialVersionUID = 1L;

	private Long id;
	private EventVS eventVS;
	private String origenHashTokenAcceso;

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setEventVS(EventVS eventVS) {
		this.eventVS = eventVS;
	}

	public EventVS getEventVS() {
		return eventVS;
	}

	public void setOrigenHashTokenAcceso(String origenHashTokenAcceso) {
		this.origenHashTokenAcceso = origenHashTokenAcceso;
	}

	public String getOrigenHashTokenAcceso() {
		return origenHashTokenAcceso;
	}
	
}
