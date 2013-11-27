package org.votingsystem.android.model;

import org.votingsystem.model.EventVS;

import java.io.Serializable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class VoteQuery implements Serializable  {
	
    private static final long serialVersionUID = 1L;
    private Long id;
    private EventVS eventVS;
    private KeyStoreVS tokenAcceso;

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

    public void setTokenAcceso(KeyStoreVS tokenAcceso) {
        this.tokenAcceso = tokenAcceso;
    }

    public KeyStoreVS getTokenAcceso() {
        return tokenAcceso;
    }
	
}
