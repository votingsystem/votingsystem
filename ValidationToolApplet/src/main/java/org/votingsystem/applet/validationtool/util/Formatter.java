package org.votingsystem.applet.validationtool.util;

import java.security.cert.X509Certificate;
import org.votingsystem.model.EventVSBase;
import java.text.ParseException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.DateUtils;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Formatter {
    
   private static Logger logger = Logger.getLogger(Formatter.class);

   private static String controlAccesoLabel = ContextVS.INSTANCE.
           getString("controlAccesoLabel");
   private static String nombreLabel = ContextVS.INSTANCE.getString("nombreLabel");  
   private static String asuntoLabel = ContextVS.INSTANCE.getString("asuntoLabel"); 
   private static String contenidoLabel = ContextVS.INSTANCE.getString("contenidoLabel");
   private static String fechaInicioLabel = ContextVS.INSTANCE.getString("fechaInicioLabel");   
   private static String fechaFinLabel = ContextVS.INSTANCE.getString("fechaFinLabel");  
   private static String urlLabel = ContextVS.INSTANCE.getString("urlLabel");  
    private static String hashSolicitudAccesoBase64Label = ContextVS.INSTANCE.
              getString("hashSolicitudAccesoBase64Label");  
    private static String opcionSeleccionadaContenidoLabel = ContextVS.INSTANCE.
              getString("opcionSeleccionadaContenidoLabel");
    

    public static String getInfoCert(X509Certificate certificado) {
        return ContextVS.INSTANCE.getString("certInfoFormattedMsg", 
            certificado.getSubjectDN().toString(), 
            certificado.getIssuerDN().toString(),
            certificado.getSerialNumber().toString(), 
            DateUtils.getSpanishFormattedStringFromDate(
                certificado.getNotBefore()), 
            DateUtils.getSpanishFormattedStringFromDate(
                certificado.getNotAfter()));
    }
    public static String procesar (String cadena) throws ParseException {
        if(cadena == null) {
            logger.debug(" - procesar null string");
            return null;
        }
        EventVSBase evento = null;
        String result = null;
        try {
            JSONObject jsonObject = (JSONObject)JSONSerializer.toJSON(cadena);
            evento = EventVSBase.populate(jsonObject);
            result = obtenerEvento(evento);
        } catch(Exception ex) {
            logger.error("cadena: " + cadena + " - " + ex.getMessage(), ex);
        }
        return result;
    }
    
    	
    public static String obtenerEvento (EventVSBase evento) {
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
                evento.getOpcionSeleccionada().getContent() + "<br/>");
        }
        return result.toString();
    }
    
}