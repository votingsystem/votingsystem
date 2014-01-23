package org.votingsystem.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.signature.util.CertUtil;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ControlCenterVS extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private EventVS eventVS;

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public EventVS getEventVS() {
        return eventVS;
    }
        
    @Override public Type getType() {
        return Type.CONTROL_CENTER;
    }

    public String getVoteServiceURL () {
        return getServerURL() + "/voteVS";
    }

}