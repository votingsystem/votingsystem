package org.sistemavotacion.util;

import static org.sistemavotacion.Contexto.*;

import org.sistemavotacion.smime.*;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Tipo;
import org.sistemavotacion.smime.DNIeSignedMailGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class VotacionHelper {

    private static Logger logger = LoggerFactory.getLogger(VotacionHelper.class);
    
    public static final String ASUNTO_MENSAJE_SOLICITUD_ACCESO = "[SOLICITUD ACCESO]-";     
    public static final String NOMBRE_DESTINATARIO = "Sistema de Votación"; 
    
    private static Map<String, ReciboVoto> mapaRecibos;
  
    public static Evento prepararVoto (Evento evento) throws NoSuchAlgorithmException {    
        evento.setOrigenHashSolicitudAcceso(UUID.randomUUID().toString());
        evento.setHashSolicitudAccesoBase64(CMSUtils.getHashBase64(
            evento.getOrigenHashSolicitudAcceso(), VOTING_DATA_DIGEST));
        evento.setOrigenHashCertificadoVoto(UUID.randomUUID().toString());
        evento.setHashCertificadoVotoBase64(CMSUtils.getHashBase64(
            evento.getOrigenHashCertificadoVoto(), VOTING_DATA_DIGEST));         
        return evento;
    }
   
    public static File obtenerSolicitudAcceso (Evento voto, 
            String password, File resultado) throws NoSuchAlgorithmException, Exception {
        String asuntoMensaje = 
                ASUNTO_MENSAJE_SOLICITUD_ACCESO + voto.getEventoId();
        File solicitudAcceso = DNIeSignedMailGenerator.genFile("",
                NOMBRE_DESTINATARIO, obtenerSolicitudAccesoJSONStr(voto),
                password.toCharArray(), asuntoMensaje, resultado);
        return solicitudAcceso;
    }
        
    public static String obtenerSolicitudAccesoJSONStr(Evento voto) {
        logger.debug("obtenerSolicitudAccesoJSONStr");
        JSONObject jsonObject = obtenerSolicitudAccesoJSON(voto);        
        return jsonObject.toString();
    }
    
    public static String obtenerVotoJSONStr(Evento voto) {
        logger.debug("obtenerVotoJSONStr");
        if(voto == null) return null;
        return obtenerVotoJSON(voto).toString();
    }
    
    public static JSONObject obtenerVotoJSON(Evento voto) {
        logger.debug("obtenerVotoJSON");
        if(voto == null) return null;
        Map map = new HashMap();
        map.put("operation", Tipo.VOTO.toString());
        map.put("eventoURL", voto.getUrl());
        map.put("opcionSeleccionadaId", voto.getOpcionSeleccionada().getId());
        map.put("opcionSeleccionadaContenido", voto.getOpcionSeleccionada().getContenido());
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject;
    }
    
    public static JSONObject obtenerAnuladorDeVotoSesion(
            String hashCertificadoVotoBase64) {
        logger.debug("obtenerAnuladorDeVotoSesion");
        if(mapaRecibos == null) return null;
        ReciboVoto recibo = mapaRecibos.get(hashCertificadoVotoBase64);
        if(recibo == null) return null;
        Evento voto = recibo.getVoto();
        return obtenerAnuladorDeVotoJSON(voto);
    } 
    
    public static JSONObject obtenerAnuladorDeVotoJSON(Evento voto) {
        logger.debug("obtenerAnuladorDeVotoJSON");
        if(voto == null) return null;
        Map map = new HashMap();
        map.put("operation", Tipo.ANULADOR_VOTO.toString());
        map.put("origenHashCertificadoVoto", voto.getOrigenHashCertificadoVoto());
        map.put("hashCertificadoVotoBase64", voto.getHashCertificadoVotoBase64());
        map.put("origenHashSolicitudAcceso", voto.getOrigenHashSolicitudAcceso());
        map.put("hashSolicitudAccesoBase64", voto.getHashSolicitudAccesoBase64());
        map.put("UUID", UUID.randomUUID().toString());
        return (JSONObject) JSONSerializer.toJSON(map);
    }
        
    public static JSONObject obtenerSolicitudAccesoJSON(Evento voto) {
        logger.debug("obtenerSolicitudAccesoJSON");
        Map map = new HashMap();
        map.put("operation", Tipo.SOLICITUD_ACCESO.toString());
        map.put("eventId", voto.getEventoId());
        map.put("eventURL", voto.getUrl());
        map.put("hashSolicitudAccesoBase64", voto.getHashSolicitudAccesoBase64());
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);        
        return jsonObject;
    }

    
    public static void addRecibo(String hashCertificadoVotoBase64, ReciboVoto recibo) {
        if(mapaRecibos == null) mapaRecibos = new HashMap<String, ReciboVoto>();
        mapaRecibos.put(hashCertificadoVotoBase64, recibo);
    }
    
    public static ReciboVoto getReciboVoto(String hashCertificadoVotoBase64) {
        if(mapaRecibos == null || hashCertificadoVotoBase64 == null) return null;
        return mapaRecibos.get(hashCertificadoVotoBase64);
    }
       
}