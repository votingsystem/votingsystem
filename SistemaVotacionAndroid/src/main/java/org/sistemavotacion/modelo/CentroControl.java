package org.sistemavotacion.modelo;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sistemavotacion.seguridad.CertUtil;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class CentroControl extends ActorConIP implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private Evento evento;
    private Set<ControlAcceso> controlesDeAcceso;

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Evento getEvento() {
        return evento;
    }
        
    @Override
    public Tipo getTipo() {
        return Tipo.CENTRO_CONTROL;
    }

    /**
     * @return the controlesDeAcceso
     */
    public Set<ControlAcceso> getControlesDeAcceso() {
        return controlesDeAcceso;
    }

    /**
     * @param controlesDeAcceso the controlesDeAcceso to set
     */
    public void setControlesDeAcceso(Set<ControlAcceso> controlesDeAcceso) {
        this.controlesDeAcceso = controlesDeAcceso;
    }


    public static CentroControl parse(String actorConIPStr, ActorConIP.Tipo tipo)
            throws Exception {
        JSONObject actorConIPJSON = new JSONObject(actorConIPStr);
        JSONObject jsonObject = null;
        JSONArray jsonArray;
        CentroControl actorConIP = new CentroControl();
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
