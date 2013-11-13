package org.votingsystem.android.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessControl extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private EventVS eventVS;
    private Set<ControlCenter> centrosDeControl;


    public AccessControl() {}

    public AccessControl(ActorVS actorVS) {
        setCertificado(actorVS.getCertificado());
        setCertChain(actorVS.getCertChain());
        setCertificadoPEM(actorVS.getCertificadoPEM());
        setCertificadoURL(actorVS.getCertificadoURL());
        setDateCreated(actorVS.getDateCreated());
        setId(actorVS.getId());
        setEstado(actorVS.getEstado());
        setServerURL(actorVS.getServerURL());
        setLastUpdated(actorVS.getLastUpdated());
        setNombre(actorVS.getNombre());
    }

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
     * @return the centrosDeControl
     */
    public Set<ControlCenter> getCentrosDeControl() {
        return centrosDeControl;
    }

    /**
     * @param centrosDeControl the centrosDeControl to set
     */
    public void setCentrosDeControl(Set<ControlCenter> centrosDeControl) {
        this.centrosDeControl = centrosDeControl;
    }

    public static AccessControl parse(String actorConIPStr) throws Exception {
        JSONObject actorConIPJSON = new JSONObject(actorConIPStr);
        JSONObject jsonObject = null;
        JSONArray jsonArray;
        AccessControl actorConIP = new AccessControl();
        if (actorConIPJSON.getJSONArray("centrosDeControl") != null) {
            Set<ControlCenter> centrosDeControl = new HashSet<ControlCenter>();
            jsonArray = actorConIPJSON.getJSONArray("centrosDeControl");
            for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                ControlCenter controlCenter = new ControlCenter();
                controlCenter.setNombre(jsonObject.getString("nombre"));
                controlCenter.setServerURL(jsonObject.getString("serverURL"));
                controlCenter.setId(jsonObject.getLong("id"));
                controlCenter.setDateCreated(DateUtils.getDateFromString(jsonObject.getString("fechaCreacion")));
                if (jsonObject.getString("estado") != null) {
                    controlCenter.setEstado(ActorVS.Estado.valueOf(jsonObject.getString("estado")));
                }
                centrosDeControl.add(controlCenter);
            }
            ((AccessControl)actorConIP).setCentrosDeControl(centrosDeControl);
        }
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
