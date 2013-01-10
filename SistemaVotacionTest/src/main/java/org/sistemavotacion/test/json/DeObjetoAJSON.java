package org.sistemavotacion.test.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.*;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.modelo.SolicitudAcceso;
import org.sistemavotacion.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class DeObjetoAJSON {

    private static Logger logger = LoggerFactory.getLogger(DeObjetoAJSON.class);

    public static String obtenerEventoJSON(Evento evento){
        logger.debug("obtenerEventoJSON");
        Map map = new HashMap();
        map.put("asunto", evento.getAsunto());
        map.put("contenido", evento.getContenido());
        map.put("fechaInicio", DateUtils.getStringFromDate(evento.getFechaInicio()));
        map.put("fechaFin", DateUtils.getStringFromDate(evento.getFechaFin()));
        if (evento.getTipo() != null) map.put("tipoEvento", evento.getTipo().toString()); 
        if (evento.getEventoId() != null) map.put("eventoId", evento.getEventoId()); 
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON( map );
        if (evento.getEtiquetas() != null) {
            String[] etiquetas = evento.getEtiquetas();
            JSONArray jsonArray = new JSONArray();
            for (String etiqueta : etiquetas) {
                jsonArray.element(etiqueta);
            }
            jsonObject.put("etiquetas", jsonArray);
        }
        if (evento.getCentroControl() != null) {
            Map centroControlMap = new HashMap(); 
            centroControlMap.put("id", evento.getCentroControl().getId());
            centroControlMap.put("nombre", evento.getCentroControl().getNombre());
            centroControlMap.put("serverURL", evento.getCentroControl().getServerURL());
            JSONObject centroControlJSON = (JSONObject) JSONSerializer.toJSON( centroControlMap );
            jsonObject.put("centroControl", centroControlJSON);
        }        
        if (evento.getOpciones() != null 
        		&& evento.getTipo() == Tipo.VOTACION) {
            List<OpcionEvento> opciones = evento.getOpciones();
            JSONArray jsonArray = new JSONArray();
            for (OpcionEvento opcion : opciones) {
                jsonArray.element(opcion.getContenido());
            }
            jsonObject.put("opciones", jsonArray);
        }
        if (evento.getOpciones() != null 
        		&& evento.getTipo() != Tipo.VOTACION) {
        	List<OpcionEvento> opciones = evento.getOpciones();
            JSONArray jsonArray = new JSONArray();
            for (OpcionEvento opcion : opciones) {
                Map campoMap = new HashMap();
                campoMap.put("contenido", opcion.getContenido());
                campoMap.put("valor", opcion.getValor());
                campoMap.put("id", opcion.getId());
                JSONObject camposJSON = (JSONObject) JSONSerializer.toJSON(campoMap);
                jsonArray.element(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        if (evento.getCardinalidadDeOpciones() != null) map.put("cardinalidad", 
        		evento.getCardinalidadDeOpciones().toString()); 
        if (evento.getOpcionSeleccionada() != null) {
            Map opcionSeleccionadaMap = new HashMap(); 
            opcionSeleccionadaMap.put("id", evento.getOpcionSeleccionada().getId());
            opcionSeleccionadaMap.put("contenido", evento.getOpcionSeleccionada().getContenido());
            JSONObject opcionSeleccionadaJSON = (JSONObject) JSONSerializer.toJSON( opcionSeleccionadaMap );
            jsonObject.put("opcionSeleccionada", opcionSeleccionadaJSON);
        } 
        logger.debug("obtenerEventoJSON - jsonObject.toString(): " + jsonObject.toString());
        return jsonObject.toString();    
    }

   public String obtenerVotoParaEventoJSON(Evento evento) {
        logger.debug("obtenerVotoParaEventoJSON");
        Map map = new HashMap();
        map.put("eventoURL", ContextoPruebas.getURLEventoParaVotar(
                ContextoPruebas.getControlAcceso().getServerURL(), 
                evento.getEventoId()));
        map.put("opcionSeleccionadaId", evento.getOpcionSeleccionada().getId());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }
   
   public static String obtenerAnuladorDeVotoJSON(Evento evento) {
        logger.debug("obtenerAnuladorDeVotoJSON - origenHashCertificadoVoto:" +
               evento.getOrigenHashCertificadoVoto() + 
                " - hashCertificadoVotoBase64: " + evento.getHashCertificadoVotoBase64() + 
                " - origenHashSolicitudAcceso: " + evento.getOrigenHashSolicitudAcceso() +
                " - hashSolicitudAccesoBase64: " + evento.getHashSolicitudAccesoBase64());
        Map map = new HashMap();
        map.put("origenHashCertificadoVoto", evento.getOrigenHashCertificadoVoto());
        map.put("hashCertificadoVotoBase64", evento.getHashCertificadoVotoBase64());
        map.put("origenHashSolicitudAcceso", evento.getOrigenHashSolicitudAcceso());
        map.put("hashSolicitudAccesoBase64", evento.getHashSolicitudAccesoBase64());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }
   
    public static String obtenerAnuladorDeVotoJSON(SolicitudAcceso solicitud) {
        logger.debug("obtenerAnuladorDeVotoJSON");
        Map map = new HashMap();
        map.put("origenHashCertificadoVoto", solicitud.getOrigenHashCertificadoVoto());
        map.put("hashCertificadoVotoBase64", solicitud.getHashCertificadoVotoBase64());
        map.put("origenHashSolicitudAcceso", solicitud.getOrigenHashSolicitudAcceso());
        map.put("hashSolicitudAccesoBase64", solicitud.getHashSolicitudAccesoBase64());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }
    
   public static String obtenerFirmaParaEventoJSON(Evento evento) {
        logger.debug("obtenerFirmaParaEventoJSON");
        Map map = new HashMap();
        Map controlAccesoMap = new HashMap();
        controlAccesoMap.put("serverURL", evento.getControlAcceso().getServerURL());
        controlAccesoMap.put("nombre", evento.getControlAcceso().getNombre());
        map.put("controlAcceso", controlAccesoMap);
        map.put("eventoId", evento.getEventoId());
        map.put("asunto", evento.getAsunto());
        map.put("contenido", evento.getContenido());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON( map );
        if (evento.getOpciones() != null) {
            List<OpcionEvento> opciones = evento.getOpciones();
            JSONArray jsonArray = new JSONArray();
            for (OpcionEvento opcion : opciones) {
                Map campoMap = new HashMap();
                campoMap.put("id", opcion.getId());
                campoMap.put("contenido", opcion.getContenido());
                campoMap.put("valor", opcion.getValor());
                JSONObject camposJSON = (JSONObject) JSONSerializer.toJSON(campoMap);
                jsonArray.element(camposJSON);
            }
            jsonObject.put("campos", jsonArray);
        }
        return jsonObject.toString();
    }
   
    public static String obtenerSolicitudAccesoJSON(String serverURL, Evento evento) {
        logger.debug("obtenerSolicitudAccesoJSON");
        Map map = new HashMap();
        map.put("eventoURL", ContextoPruebas.getURLEventoParaVotar(
                serverURL, evento.getEventoId()));
        map.put("hashSolicitudAccesoBase64", evento.getHashSolicitudAccesoBase64());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON( map );        
         return jsonObject.toString();
    }
    
    public static String obtenerDocumentoAsociacionJSON(String serverURL) {
        return "{serverURL:\""+ serverURL.trim() +"\"}";
    }
    

    
}
