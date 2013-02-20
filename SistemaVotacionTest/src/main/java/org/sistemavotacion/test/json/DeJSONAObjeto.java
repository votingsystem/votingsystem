package org.sistemavotacion.test.json;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.test.modelo.SolicitudAcceso;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class DeJSONAObjeto {
    
    private static Logger logger = LoggerFactory.getLogger(DeJSONAObjeto.class);
    
    public static Evento obtenerEvento(String eventoStr) throws ParseException {
        return obtenerEvento((JSONObject)JSONSerializer.toJSON(eventoStr));
    }

    public static Evento obtenerEvento(JSONObject eventoJSON) throws ParseException {
        logger.debug("obtenerEvento - eventoJSON: " + eventoJSON);
        JSONArray jsonArray;
        JSONObject jsonObject;
        Evento evento = new Evento();
        if (eventoJSON.containsKey("contenido"))
            evento.setContenido(eventoJSON.getString("contenido"));
        if (eventoJSON.containsKey("asunto"))        
            evento.setAsunto(eventoJSON.getString("asunto"));
        if (eventoJSON.containsKey("id"))        
            evento.setEventoId(eventoJSON.getLong("id"));
        if (eventoJSON.containsKey("eventoId"))        
            evento.setEventoId(eventoJSON.getLong("eventoId"));
        if (eventoJSON.containsKey("usuario")) {
            Usuario usuario = new Usuario();
            usuario.setNombre(eventoJSON.getString("usuario"));
            evento.setUsuario(usuario);
        }
        if (eventoJSON.containsKey("numeroTotalFirmas"))        
            evento.setNumeroTotalFirmas(eventoJSON.getInt("numeroTotalFirmas"));
        if (eventoJSON.containsKey("numeroTotalVotos"))        
            evento.setNumeroTotalVotos(eventoJSON.getInt("numeroTotalVotos"));      
        if (eventoJSON.containsKey("fechaInicio"))
            evento.setFechaInicio(DateUtils.getDateFromString(eventoJSON.getString("fechaInicio")));
        if (eventoJSON.containsKey("fechaFin"))
            evento.setFechaFin(DateUtils.getDateFromString(eventoJSON.getString("fechaFin")));
        evento.setFirmado(Boolean.FALSE);
        if (eventoJSON.containsKey("controlAcceso") && 
                eventoJSON.getJSONObject("controlAcceso") != null) {
            jsonObject = eventoJSON.getJSONObject("controlAcceso");
            ActorConIP controlAcceso = new ActorConIP ();
            controlAcceso.setServerURL(jsonObject.getString("serverURL"));
            controlAcceso.setNombre(jsonObject.getString("nombre"));
            evento.setControlAcceso(controlAcceso);
        }
        if (eventoJSON.containsKey("etiquetas") && 
                eventoJSON.getJSONArray("etiquetas") != null) {
            List<String> etiquetas = new ArrayList<String>();
            jsonArray = eventoJSON.getJSONArray("etiquetas");
            for (int i = 0; i< jsonArray.size(); i++) {
                etiquetas.add(jsonArray.getString(i));
            }
            evento.setEtiquetas(etiquetas.toArray(new String[jsonArray.size()]));
        }
        if (eventoJSON.containsKey("campos")) {
            List<OpcionEvento> campos = new ArrayList<OpcionEvento>();
            jsonArray = eventoJSON.getJSONArray("campos");
             for (int i = 0; i< jsonArray.size(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                OpcionEvento campo = new OpcionEvento();
                if(eventoJSON.containsKey("id"))
                    campo.setId(jsonObject.getLong("id"));
                if(eventoJSON.containsKey("contenido"))
                    campo.setContenido(jsonObject.getString("contenido"));
                campos.add(campo);
             }
            evento.setOpciones(campos);
        }
        if (eventoJSON.containsKey("opciones")) {
            List<OpcionEvento> opciones = new ArrayList<OpcionEvento>();
            jsonArray = eventoJSON.getJSONArray("opciones");
             for (int i = 0; i< jsonArray.size(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                OpcionEvento opcion = new OpcionEvento();
                opcion.setId(jsonObject.getLong("id"));
                opcion.setContenido(jsonObject.getString("contenido"));
                opciones.add(opcion);
             }
            evento.setOpciones(opciones);
        }
        if (eventoJSON.containsKey("centroControl")) {
            jsonObject = eventoJSON.getJSONObject("centroControl");
            ActorConIP centroControl = new ActorConIP();
            centroControl.setId(jsonObject.getLong("id"));
            centroControl.setServerURL(jsonObject.getString("serverURL"));
            centroControl.setNombre(jsonObject.getString("nombre"));
            evento.setCentroControl(centroControl);
        }
        return evento;

    }
    
    public static ActorConIP obtenerActorConIP(String actorConIPStr) throws ParseException {
        logger.debug("obtenerActorConIP - actor: " + actorConIPStr);
        JSONObject actorConIPJSON = (JSONObject) JSONSerializer.toJSON(actorConIPStr);
        JSONObject jsonObject = null;
        ActorConIP actorConIP = null;
        JSONArray jsonArray;
        if (actorConIPJSON.containsKey("tipoServidor")){
            ActorConIP.Tipo tipoServidor = ActorConIP.Tipo.valueOf(actorConIPJSON.getString("tipoServidor"));
            actorConIP = new ActorConIP();
            actorConIP.setTipo(tipoServidor);
             switch (tipoServidor) {
                 case CENTRO_CONTROL: break;
                 case CONTROL_ACCESO:
                     if (actorConIPJSON.getJSONArray("centrosDeControl") != null) {
                         Set<ActorConIP> centrosDeControl = new HashSet<ActorConIP>();
                         jsonArray = actorConIPJSON.getJSONArray("centrosDeControl");
                         for (int i = 0; i< jsonArray.size(); i++) {
                             jsonObject = jsonArray.getJSONObject(i);
                             ActorConIP centroControl = new ActorConIP();
                             centroControl.setNombre(jsonObject.getString("nombre"));
                             centroControl.setServerURL(jsonObject.getString("serverURL"));
                             centroControl.setId(jsonObject.getLong("id"));
                             if (jsonObject.getString("estado") != null) {
                                 centroControl.setEstado(ActorConIP.Estado.valueOf(jsonObject.getString("estado")));
                             }
                             centrosDeControl.add(centroControl);
                         }
                         actorConIP.setCentrosDeControl(centrosDeControl);
                     }
                     break;
             }   
        }
        if (actorConIPJSON.containsKey("environmentMode")) {
            actorConIP.setEnvironmentMode(ActorConIP.EnvironmentMode.valueOf(
                    actorConIPJSON.getString("environmentMode")));
        }
        if (actorConIPJSON.containsKey("cadenaCertificacionURL"))
             actorConIP.setCertificadoURL(actorConIPJSON.getString("cadenaCertificacionURL"));
        if (actorConIPJSON.containsKey("serverURL"))
             actorConIP.setServerURL(actorConIPJSON.getString("serverURL"));
        if (actorConIPJSON.containsKey("nombre"))
             actorConIP.setNombre(actorConIPJSON.getString("nombre"));
        return actorConIP;
    }

	public static SolicitudAcceso obtenerSolicitudAcceso(String strSolicitud) 
			throws IOException, Exception {
        logger.debug("obtenerSolicitudAcceso - strSolicitud: " + strSolicitud);
        SolicitudAcceso solicitud = new SolicitudAcceso();
        JSONObject solicitudJSON = (JSONObject) JSONSerializer.toJSON(strSolicitud);
        if(solicitudJSON.containsKey("hashSolicitudAccesoBase64")) ;
            solicitud.setHashSolicitudAccesoBase64(solicitudJSON.getString("hashSolicitudAccesoBase64"));
        if(solicitudJSON.containsKey("origenHashSolicitudAcceso")) ;
            solicitud.setOrigenHashSolicitudAcceso(
                    solicitudJSON.getString("origenHashSolicitudAcceso"));
        if(solicitudJSON.containsKey("hashCertificadoVotoBase64")) ;
            solicitud.setHashCertificadoVotoBase64(
                    solicitudJSON.getString("hashCertificadoVotoBase64"));
        if(solicitudJSON.containsKey("origenHashCertificadoVoto")) ;
            solicitud.setOrigenHashCertificadoVoto(
                    solicitudJSON.getString("origenHashCertificadoVoto"));            
        return solicitud;
	}
    
}
