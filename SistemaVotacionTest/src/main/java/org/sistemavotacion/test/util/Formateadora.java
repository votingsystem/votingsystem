package org.sistemavotacion.test.util;

import java.text.ParseException;
import java.util.List;
import java.util.Set;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.*;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Formateadora {
    
    private static Logger logger = LoggerFactory.getLogger(Formateadora.class);
	
    public static String obtenerEvento (Evento evento) {
        logger.debug("obtenerEvento - evento: " + evento.getEventoId()); 
        if (evento == null) return null;
        StringBuilder result = new StringBuilder("<html>");
        result.append("<h2> - Control de Acceso - </h2><b>Nombre: </b>").append(
                evento.getControlAcceso().getNombre()).append("<br/>").append(
                "<b>ServerURL: </b>").append(evento.getControlAcceso().getServerURL())
                .append("<br/><br/><h2> - Datos del evento - </h2>").append("<b>EventoId: </b>")
                .append(evento.getEventoId()).append("<br/><b>Asunto: </b>")
                .append(evento.getAsunto()).append("<br/><b>Contenido: </b>").
                append(evento.getContenido()).append("</html>");
        return result.toString();
    }
    
    
    public static String obtenerInfoServidor (ActorConIP actorConIP) {
        logger.debug("obtenerInfoServidor - actorConIP: " + actorConIP.getNombre()); 
        if (actorConIP == null) return null;
        StringBuilder result = new StringBuilder("<html>");
        if(actorConIP.getTipo() != null)
            result.append("<b>Tipo de servidor: </b>").append(actorConIP.getTipo().toString());
        result.append("<br/><b>Nombre: </b>").append(actorConIP.getNombre());
        if(actorConIP.getEstado() != null) {
            result.append("<br/><b>Estado: </b>" 
                    + actorConIP.getEstado().toString());
        }
        if(actorConIP.getEnvironmentMode() != null) {
            result.append("<br/><b>Entorno de ejecución: </b>" 
                    + actorConIP.getEnvironmentMode().toString());
        }
        result.append("<br/><b><a href=\"" + 
            actorConIP.getServerURL() +"\">URL del servidor</a></b>");
        if(actorConIP.getCertificadoURL() != null)
            result.append("<br/><b><a href=\"" + 
                   actorConIP.getCertificadoURL() +"\">URL de la Cadena de Certificación</a></b>");
        if(actorConIP.getTipo() == ActorConIP.Tipo.CONTROL_ACCESO) {
            result.append("<br/><b>Centros de Control: </b><ul>");
            if(actorConIP.getCentrosDeControl() != null) {
                Set<ActorConIP> centrosDeControl = actorConIP.getCentrosDeControl();
                for(ActorConIP centroControl: centrosDeControl) {
                    result.append("<li><b>Nombre: </b> " + centroControl.getNombre() 
                            + "<br/><b><a href=\"" + 
                    centroControl.getServerURL() +"\">URL del servidor</a></b></li>");
                }
            }
            result.append("</ul>");
        }
        return result.toString();
    }
    
    public static String obtenerInfoEvento (Evento evento) {
        logger.debug("obtenerInfoEvento - evento: " + evento.getAsunto()); 
        if (evento == null) return null;
        StringBuilder result = new StringBuilder("<html>");
        result.append("<b>Asunto: </b>").append(evento.getAsunto());
        result.append("<br/><b>Contenido: </b>").append(evento.getContenido());
        result.append("<br/><b>Fecha inicio: </b>" + DateUtils.getStringFromDate(evento.getFechaInicio()));
        result.append("   <b> - Fecha fin: </b>" + DateUtils.getStringFromDate(evento.getFechaFin()));
        if(evento.getOpciones() != null && evento.getOpciones().size() > 0) {
            result.append("<br/><b>Opciones: </b><ul>");
            List<OpcionEvento> opciones = evento.getOpciones();
            for(OpcionEvento opcion: opciones) {
                result.append("<li><b>Nombre: </b> " + opcion.getContenido() + "</li>");
            }
            result.append("</ul>");
        }
        if(evento.getEtiquetas() != null && evento.getEtiquetas().length > 0) {
            String[] etiquetas = evento.getEtiquetas();
            if(!(etiquetas.length == 1 && "".equals(etiquetas[0].trim()))) {
                result.append("<br/><b>Etriquetas: </b><ul>");
                for(String etiqueta: etiquetas) {
                    result.append("<li>" + etiqueta + "</li>");
                }
                result.append("</ul>");
            }
        }
        if(evento.getEventoId() != null) {
            String urlConvocatoriaEnControlAcceso = ContextoPruebas.INSTANCE.
                    getVotingEventURL(evento.getEventoId());
            String urlConvocatoriaEnCentroControl = ContextoPruebas.getVotingEventURL(
                    evento.getCentroControl().getServerURL(), evento.getEventoId());
            logger.debug("urlConvocatoriaEnControlAcceso: " + urlConvocatoriaEnControlAcceso);
            result.append("<br/><b><a href=\"" + urlConvocatoriaEnControlAcceso +
                    "\">URL de los datos de la convocatoria en el Control de Acceso</a></b>");
            result.append("<br/><b><a href=\"" + urlConvocatoriaEnCentroControl +
                    "\">URL de los datos de la convocatoria en el Centro de Control</a></b>");
        }
        return result.toString();
    }
    
    public String obtenerMensaje (JSONObject mensajeJSON) {
        logger.debug("obtenerMensaje - mensajeJSON: " + mensajeJSON.toString()); 
        return null;
    }



    public String obtenerHistorico (JSONObject jsonObject){
        logger.debug("obtenerHistorico - jsonObject: " + jsonObject.toString());
        return null;}

    public static String procesar (String cadena) throws ParseException {
        String result = null;
        JSONObject jsonObject = (JSONObject)JSONSerializer.toJSON(cadena);
        if (jsonObject.containsKey("asunto")) {
            Evento evento = Evento.parse(jsonObject);
            result = obtenerEvento(evento);
        }
        return result;
    }
}