package org.votingsystem.android.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.signature.util.CertUtil;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ControlCenter extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private EventVS eventVS;
    private Set<AccessControl> controlesDeAcceso;

    public void setEventVSBase(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public EventVS getEventVSBase() {
        return eventVS;
    }
        
    @Override
    public Tipo getTipo() {
        return Tipo.CONTROL_CENTER;
    }

    /**
     * @return the controlesDeAcceso
     */
    public Set<AccessControl> getControlesDeAcceso() {
        return controlesDeAcceso;
    }

    /**
     * @param controlesDeAcceso the controlesDeAcceso to set
     */
    public void setControlesDeAcceso(Set<AccessControl> controlesDeAcceso) {
        this.controlesDeAcceso = controlesDeAcceso;
    }


    public static ControlCenter parse(String actorConIPStr, ActorVS.Tipo tipo)
            throws Exception {
        JSONObject actorConIPJSON = new JSONObject(actorConIPStr);
        JSONObject jsonObject = null;
        JSONArray jsonArray;
        ControlCenter actorConIP = new ControlCenter();
        if (actorConIPJSON.has("urlBlog"))
            actorConIP.setUrlBlog(actorConIPJSON.getString("urlBlog"));
        if (actorConIPJSON.has("serverURL"))
            actorConIP.setServerURL(actorConIPJSON.getString("serverURL"));
        if (actorConIPJSON.has("nombre"))
            actorConIP.setNombre(actorConIPJSON.getString("nombre"));
        if (actorConIPJSON.has("cadenaCertificacionPEM")) {
            Collection<X509Certificate> certChain =
                    CertUtil.fromPEMToX509CertCollection(actorConIPJSON.
                            getString("cadenaCertificacionPEM").getBytes());
            actorConIP.setCertChain(certChain);
            X509Certificate serverCert = certChain.iterator().next();
            Log.d(TAG + ".obtenerActorConIP(..) ", " - actorConIP Cert: "
                    + serverCert.getSubjectDN().toString());
            actorConIP.setCertificado(serverCert);
        }
        if (actorConIPJSON.has("timeStampCertPEM")) {
            actorConIP.setTimeStampCertPEM(actorConIPJSON.getString(
                    "timeStampCertPEM"));
        }
        return actorConIP;
    }

}
