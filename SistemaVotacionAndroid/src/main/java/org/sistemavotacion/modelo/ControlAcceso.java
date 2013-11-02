package org.sistemavotacion.modelo;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.util.DateUtils;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ControlAcceso extends ActorConIP implements Serializable {

    public static final long serialVersionUID = 1L;
    
    private Evento evento;
    private Set<CentroControl> centrosDeControl;


    public ControlAcceso() {}

    public ControlAcceso(ActorConIP actorConIP) {
        setCertificado(actorConIP.getCertificado());
        setCertChain(actorConIP.getCertChain());
        setCertificadoPEM(actorConIP.getCertificadoPEM());
        setCertificadoURL(actorConIP.getCertificadoURL());
        setDateCreated(actorConIP.getDateCreated());
        setId(actorConIP.getId());
        setEstado(actorConIP.getEstado());
        setServerURL(actorConIP.getServerURL());
        setLastUpdated(actorConIP.getLastUpdated());
        setNombre(actorConIP.getNombre());
    }

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
     * @return the centrosDeControl
     */
    public Set<CentroControl> getCentrosDeControl() {
        return centrosDeControl;
    }

    /**
     * @param centrosDeControl the centrosDeControl to set
     */
    public void setCentrosDeControl(Set<CentroControl> centrosDeControl) {
        this.centrosDeControl = centrosDeControl;
    }

    public static ControlAcceso parse(String actorConIPStr) throws Exception {
        JSONObject actorConIPJSON = new JSONObject(actorConIPStr);
        JSONObject jsonObject = null;
        JSONArray jsonArray;
        ControlAcceso actorConIP = new ControlAcceso();
        if (actorConIPJSON.getJSONArray("centrosDeControl") != null) {
            Set<CentroControl> centrosDeControl = new HashSet<CentroControl>();
            jsonArray = actorConIPJSON.getJSONArray("centrosDeControl");
            for (int i = 0; i< jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                CentroControl centroControl = new CentroControl();
                centroControl.setNombre(jsonObject.getString("nombre"));
                centroControl.setServerURL(jsonObject.getString("serverURL"));
                centroControl.setId(jsonObject.getLong("id"));
                centroControl.setDateCreated(DateUtils.getDateFromString(jsonObject.getString("fechaCreacion")));
                if (jsonObject.getString("estado") != null) {
                    centroControl.setEstado(ActorConIP.Estado.valueOf(jsonObject.getString("estado")));
                }
                centrosDeControl.add(centroControl);
            }
            ((ControlAcceso)actorConIP).setCentrosDeControl(centrosDeControl);
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
