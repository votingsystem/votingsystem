package org.votingsystem.android.model;

import org.votingsystem.android.model.AlmacenClaves;
import org.votingsystem.model.EventVS;

import java.io.Serializable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class ConsultaVoto implements Serializable  {
	
    private static final long serialVersionUID = 1L;
    private Long id;
    private EventVS eventVS;
    private AlmacenClaves tokenAcceso;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setEventVSBase(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public EventVS getEventVSBase() {
        return eventVS;
    }

    public void setTokenAcceso(AlmacenClaves tokenAcceso) {
        this.tokenAcceso = tokenAcceso;
    }

    public AlmacenClaves getTokenAcceso() {
        return tokenAcceso;
    }
	
}
