package org.sistemavotacion.herramientavalidacion.util;

import java.text.ParseException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.herramientavalidacion.AppletHerramienta;
import org.sistemavotacion.modelo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Formateadora {
    
   private static Logger logger = LoggerFactory.getLogger(Formateadora.class);

   private static String controlAccesoLabel = AppletHerramienta.getResourceBundle().
                getString("controlAccesoLabel");
   private static String nombreLabel = AppletHerramienta.getResourceBundle().
                getString("nombreLabel");  
   private static String asuntoLabel = AppletHerramienta.getResourceBundle().
                getString("asuntoLabel"); 
   private static String contenidoLabel = AppletHerramienta.getResourceBundle().
                getString("contenidoLabel");
   private static String fechaInicioLabel = AppletHerramienta.getResourceBundle().
                getString("fechaInicioLabel");   
   private static String fechaFinLabel = AppletHerramienta.getResourceBundle().
                getString("fechaFinLabel");  
   private static String urlLabel = AppletHerramienta.getResourceBundle().
              getString("urlLabel");  
    private static String hashSolicitudAccesoBase64Label = AppletHerramienta.getResourceBundle().
              getString("hashSolicitudAccesoBase64Label");  
    private static String opcionSeleccionadaContenidoLabel = AppletHerramienta.getResourceBundle().
              getString("opcionSeleccionadaContenidoLabel");
    

    public static String procesar (String cadena) throws ParseException {
        if(cadena == null) {
            logger.debug(" - procesar null string");
            return null;
        }
        Evento evento = null;
        String result = null;
        try {
            JSONObject jsonObject = (JSONObject)JSONSerializer.toJSON(cadena);
            evento = Evento.parse(jsonObject);
            result = obtenerEvento(evento);
        } catch(Exception ex) {
            logger.error("cadena: " + cadena + " - " + ex.getMessage(), ex);
        }
        return result;
    }
    
    	
    public static String obtenerEvento (Evento evento) {
        logger.debug("obtenerEvento - evento: " + evento.getEventoId()); 
        if (evento == null) return null;
        StringBuilder result = new StringBuilder("<html>");
        if(evento.getControlAcceso() != null) {
            result.append("<b>" + controlAccesoLabel + "</b>:").append(
                evento.getControlAcceso().getNombre()).append("<br/>");
        }
        if(evento.getAsunto() != null) result.append("<b>" + asuntoLabel + "</b>: ").
                append(evento.getAsunto() + "<br/>");
        if(evento.getContenido() != null) result.append("<b>" + contenidoLabel + "</b>: ").
                append(evento.getContenido() + "<br/>");
        if(evento.getFechaInicioStr() != null) result.append("<b>" + fechaInicioLabel + "</b>: ").
                append(evento.getFechaInicioStr() + "<br/>");
        if(evento.getFechaFinStr() != null) result.append("<b>" + fechaFinLabel + "</b>: ").
                append(evento.getFechaFinStr() + "<br/>");  
        if(evento.getUrl() != null) result.append("<b>" + urlLabel + "</b>: ").
                append(evento.getUrl() + "<br/>"); 
        if(evento.getHashSolicitudAccesoBase64() != null) result.append("<b>" + 
                hashSolicitudAccesoBase64Label + "</b>: ").append(
                evento.getHashSolicitudAccesoBase64() + "<br/>");
        if(evento.getOpcionSeleccionada() != null) {
             result.append("<b>" + 
                opcionSeleccionadaContenidoLabel + "</b>: ").append(
                evento.getOpcionSeleccionada().getContenido() + "<br/>");
        }
        return result.toString();
    }
    
}