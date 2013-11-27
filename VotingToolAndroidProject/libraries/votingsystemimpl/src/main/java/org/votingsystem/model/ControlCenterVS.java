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
    private Set<AccessControlVS> controlesDeAcceso;

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public EventVS getEventVS() {
        return eventVS;
    }
        
    @Override
    public Type getType() {
        return Type.CONTROL_CENTER;
    }

    /**
     * @return the controlesDeAcceso
     */
    public Set<AccessControlVS> getControlesDeAcceso() {
        return controlesDeAcceso;
    }

    /**
     * @param controlesDeAcceso the controlesDeAcceso to set
     */
    public void setControlesDeAcceso(Set<AccessControlVS> controlesDeAcceso) {
        this.controlesDeAcceso = controlesDeAcceso;
    }


    public static ControlCenterVS parse(String actorVSStr, Type type)
            throws Exception {
        JSONObject actorVSJSON = new JSONObject(actorVSStr);
        JSONObject jsonObject = null;
        JSONArray jsonArray;
        ControlCenterVS actorVS = new ControlCenterVS();
        if (actorVSJSON.has("urlBlog"))
            actorVS.setUrlBlog(actorVSJSON.getString("urlBlog"));
        if (actorVSJSON.has("serverURL"))
            actorVS.setServerURL(actorVSJSON.getString("serverURL"));
        if (actorVSJSON.has("name"))
            actorVS.setName(actorVSJSON.getString("name"));
        if (actorVSJSON.has("certChainPEM")) {
            Collection<X509Certificate> certChain =
                    CertUtil.fromPEMToX509CertCollection(actorVSJSON.
                            getString("certChainPEM").getBytes());
            actorVS.setCertChain(certChain);
            X509Certificate serverCert = certChain.iterator().next();
            Log.d(TAG + ".getActorConIP(..) ", " - actorVS Cert: "
                    + serverCert.getSubjectDN().toString());
            actorVS.setCertificate(serverCert);
        }
        if (actorVSJSON.has("timeStampCertPEM")) {
            actorVS.setTimeStampCertPEM(actorVSJSON.getString(
                    "timeStampCertPEM"));
        }
        return actorVS;
    }

}
