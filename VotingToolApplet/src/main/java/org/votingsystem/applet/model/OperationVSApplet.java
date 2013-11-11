package org.votingsystem.applet.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVSBase;
import org.votingsystem.model.OperationVS;
import org.votingsystem.util.StringUtils;
import org.apache.log4j.Logger;
import org.votingsystem.applet.votingtool.VotingToolContext;
import org.votingsystem.model.ResponseVS;
/**
 *
 * @author jgzornoza
 */
public class OperationVSApplet implements OperationVS {
    
    private static Logger logger = Logger.getLogger(OperationVSApplet.class);
    
    public static enum Type {
        SOLICITUD_ACCESO(
                ContextVS.INSTANCE.getString("ANULADOR_VOTO")),
        ANULADOR_VOTO(
                ContextVS.INSTANCE.getString("SOLICITUD_ACCESO")),
        ASOCIAR_CENTRO_CONTROL(
                ContextVS.INSTANCE.getString("ASOCIAR_CENTRO_CONTROL")), 
        CAMBIO_ESTADO_CENTRO_CONTROL_SMIME(
                ContextVS.INSTANCE.getString("CAMBIO_ESTADO_CENTRO_CONTROL_SMIME")), 
        SOLICITUD_COPIA_SEGURIDAD(
                ContextVS.INSTANCE.getString("SOLICITUD_COPIA_SEGURIDAD")), 
        PUBLICACION_MANIFIESTO_PDF(
                ContextVS.INSTANCE.getString("PUBLICACION_MANIFIESTO_PDF")), 
        FIRMA_MANIFIESTO_PDF(
                ContextVS.INSTANCE.getString("FIRMA_MANIFIESTO_PDF")), 
        PUBLICACION_RECLAMACION_SMIME(
                ContextVS.INSTANCE.getString("PUBLICACION_RECLAMACION_SMIME")),
        FIRMA_RECLAMACION_SMIME(
                ContextVS.INSTANCE.getString("FIRMA_RECLAMACION_SMIME")), 
        PUBLICACION_VOTACION_SMIME(
                ContextVS.INSTANCE.getString("PUBLICACION_VOTACION_SMIME")), 
        ENVIO_VOTO_SMIME(
                ContextVS.INSTANCE.getString("ENVIO_VOTO_SMIME")),
        MENSAJE_APPLET(
                ContextVS.INSTANCE.getString("MENSAJE_APPLET")), 
        ANULAR_VOTO(
                ContextVS.INSTANCE.getString("ANULAR_VOTO")),
        ANULAR_SOLICITUD_ACCESO(
                ContextVS.INSTANCE.getString("ANULAR_SOLICITUD_ACCESO")),
        CANCELAR_EVENTO(
                ContextVS.INSTANCE.getString("CANCELAR_EVENTO")),
        GUARDAR_RECIBO_VOTO(
                ContextVS.INSTANCE.getString("GUARDAR_RECIBO_VOTO")),
        MENSAJE_CIERRE_APPLET(
                ContextVS.INSTANCE.getString("MENSAJE_CIERRE_APPLET")),
        NEW_REPRESENTATIVE(
                ContextVS.INSTANCE.getString("NEW_REPRESENTATIVE")),
        REPRESENTATIVE_VOTING_HISTORY_REQUEST(
                ContextVS.INSTANCE.getString("REPRESENTATIVE_VOTING_HISTORY_REQUEST")),
        REPRESENTATIVE_SELECTION(
                ContextVS.INSTANCE.getString("REPRESENTATIVE_SELECTION")),
        REPRESENTATIVE_ACCREDITATIONS_REQUEST(
                ContextVS.INSTANCE.getString("REPRESENTATIVE_ACCREDITATIONS_REQUEST")),
        MENSAJE_HERRAMIENTA_VALIDACION(""),
        REPRESENTATIVE_REVOKE(
                ContextVS.INSTANCE.getString("REPRESENTATIVE_REVOKE"));
        

    
        Type(String caption) {
            this.caption = caption;
        }
        
        Type( ) { 
            
        }
        
          
        String caption;
        
        public String getCaption() {
            return this.caption;
        }
        
        
        public String getNombreArchivoEnDisco() {
            String resultado = null;
            switch(this) {
                case ASOCIAR_CENTRO_CONTROL:
                    resultado = 
                        ContextVS.INSTANCE.getString("ASOCIAR_CENTRO_CONTROL");
                    break;
                case CAMBIO_ESTADO_CENTRO_CONTROL_SMIME:
                    resultado = ContextVS.INSTANCE.getString(
                            "CAMBIO_ESTADO_CENTRO_CONTROL_SMIME_FILE");
                    break;
                case SOLICITUD_COPIA_SEGURIDAD:
                    resultado = ContextVS.INSTANCE.getString(
                            "SOLICITUD_COPIA_SEGURIDAD_FILE");
                    break;
                case PUBLICACION_MANIFIESTO_PDF:
                    resultado = ContextVS.INSTANCE.getString(
                            "PUBLICACION_MANIFIESTO_PDF_FILE");
                    break;
                case FIRMA_MANIFIESTO_PDF:
                    resultado = ContextVS.INSTANCE.getString(
                            "FIRMA_MANIFIESTO_PDF_FILE");
                    break;
                case PUBLICACION_RECLAMACION_SMIME:
                    resultado = ContextVS.INSTANCE.getString(
                            "PUBLICACION_RECLAMACION_SMIME_FILE");
                    break;
                case FIRMA_RECLAMACION_SMIME:
                    resultado = ContextVS.INSTANCE.getString(
                            "FIRMA_RECLAMACION_SMIME_FILE");
                    break;
                case PUBLICACION_VOTACION_SMIME:
                    resultado = ContextVS.INSTANCE.getString(
                            "PUBLICACION_VOTACION_SMIME_FILE");
                    break;
                case ENVIO_VOTO_SMIME:
                    resultado = ContextVS.INSTANCE.getString(
                            "ENVIO_VOTO_SMIME_FILE");
                    break;
                default:
                    this.toString();
            }
            return resultado;
        }
    }

    private Type type;
    private Integer statusCode;
    private String callerCallback;
    private String message;
    private String urlDocumento;
    private String urlTimeStampServer;
    private String urlServer;
    private String urlEnvioDocumento;
    private String nombreDestinatarioFirma;
    private String emailSolicitante;
    private String asuntoMensajeFirmado;
    private Boolean respuestaConRecibo = false;
    private JSONObject contenidoFirma; 
    private String contentType;
    private EventVSBase evento;
    private String[] args;

    
    public OperationVSApplet() {}
    
    public OperationVSApplet(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public OperationVSApplet(Type type) {
        this.type = type;
    }
    
    public OperationVSApplet(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
    
    public OperationVSApplet(int statusCode, Type type, String message) {
        this.statusCode = statusCode;
        this.message = message;
        this.type = type;
    }

    public String[] getArgs() {
        return this.args;
    }
    
    public void setArgs(String[] args) {
        this.args = args;
    }
    
    
    /**
     * @return the urlTimeStampServer
     */
    public String getUrlTimeStampServer() {
        return urlTimeStampServer;
    }

    /**
     * @param urlTimeStampServer the urlTimeStampServer to set
     */
    public void setUrlTimeStampServer(String urlTimeStampServer) {
        this.urlTimeStampServer = urlTimeStampServer;
    }
    
    
    /**
     * @return the urlServerCert
     */
    public String getUrlServer() {
        return urlServer;
    }

    /**
     * @param urlServerCert the urlServerCert to set
     */
    public void setUrlServer(String urlServer) {
        this.urlServer = urlServer;
    }
     
    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return the statusCode
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    
    /**
     * @return the urlDocumento
     */
    public String getUrlDocumento() {
        return urlDocumento;
    }

    /**
     * @param urlDocumento the urlDocumento to set
     */
    public void setUrlDocumento(String urlDocumento) {
        this.urlDocumento = urlDocumento;
    }

    /**
     * @return the urlEnvioDocumento
     */
    public String getUrlEnvioDocumento() {
        return urlEnvioDocumento;
    }

    /**
     * @param urlEnvioDocumento the urlEnvioDocumento to set
     */
    public void setUrlEnvioDocumento(String urlEnvioDocumento) {
        this.urlEnvioDocumento = urlEnvioDocumento;
    }
    
    
    /**
     * @return the evento
     */
    public EventVSBase getEvento() {
        return evento;
    }

    /**
     * @param evento the evento to set
     */
    public void setEvento(EventVSBase evento) {
        this.evento = evento;
    }
    
    
    /**
     * @return the contenidoFirma
     */
    public JSONObject getContenidoFirma() {
        return contenidoFirma;
    }

    /**
     * @param contenidoFirma the contenidoFirma to set
     */
    public void setContenidoFirma(JSONObject contenidoFirma) {
        this.contenidoFirma = contenidoFirma;
    }
    
    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }
    /**
     * @return the nombreDestinatarioFirma
     */
    public String getNombreDestinatarioFirma() {
        return nombreDestinatarioFirma;
    }
    
    public String getNombreDestinatarioFirmaNormalizado() {
        if(nombreDestinatarioFirma == null) return null;
        return StringUtils.getCadenaNormalizada(nombreDestinatarioFirma);
    }

    /**
     * @param nombreDestinatarioFirma the nombreDestinatarioFirma to set
     */
    public void setNombreDestinatarioFirma(String nombreDestinatarioFirma) {
        this.nombreDestinatarioFirma = nombreDestinatarioFirma;
    }

    public static OperationVSApplet parse (String operacionStr) {
        logger.debug("- parse: " + operacionStr);
        if(operacionStr == null) return null;
        OperationVSApplet operacion = new OperationVSApplet();
        JSON datosJSON = JSONSerializer.toJSON(operacionStr.trim());
        JSONObject operacionJSON = null;
        if(datosJSON instanceof JSONArray) {
            operacionJSON = ((JSONArray)datosJSON).getJSONObject(0);
        } else operacionJSON = (JSONObject)datosJSON;
        if (operacionJSON.containsKey("operacion")) {
            operacion.setType(Type.valueOf(operacionJSON.getString("operacion")));
        }
        if (operacionJSON.containsKey("args")) {
            JSONArray arrayArgs = operacionJSON.getJSONArray("args");
            String[] args = new String[arrayArgs.size()];
            for(int i = 0; i < arrayArgs.size(); i++) {
                args[i] = arrayArgs.getString(i);
            }
            operacion.setArgs(args);
        }
        if (operacionJSON.containsKey("statusCode")) {
            operacion.setStatusCode(operacionJSON.getInt("statusCode"));
        }
        if (operacionJSON.containsKey("message")) {
            operacion.setMessage(operacionJSON.getString("message"));
        }   
        if (operacionJSON.containsKey("urlEnvioDocumento")) {
            operacion.setUrlEnvioDocumento(operacionJSON.getString("urlEnvioDocumento"));
        }  
        if (operacionJSON.containsKey("urlDocumento")) {
            operacion.setUrlDocumento(operacionJSON.getString("urlDocumento"));
        }  
        if (operacionJSON.containsKey("urlTimeStampServer")) {
            operacion.setUrlTimeStampServer(operacionJSON.getString("urlTimeStampServer"));
        }  
        if (operacionJSON.containsKey("urlServer")) {
            operacion.setUrlServer(operacionJSON.getString("urlServer"));
        }
        if (operacionJSON.containsKey("callerCallback")) {
            operacion.setCallerCallback(operacionJSON.getString("callerCallback"));
        }
        if (operacionJSON.containsKey("evento")) {
            EventVSBase evento = EventVSBase.populate(operacionJSON.getJSONObject("evento"));
            operacion.setEvento(evento);
        }  
        if (operacionJSON.containsKey("contenidoFirma")) {
            JSONObject contenidoJSONObject = operacionJSON.getJSONObject("contenidoFirma");
            //to avoid process repeated messages on servers
            contenidoJSONObject.put("UUID", UUID.randomUUID().toString());
            operacion.setContenidoFirma(contenidoJSONObject);
        }
             
        if(operacionJSON.containsKey("contentType")) {
            operacion.setContentType(operacionJSON.getString("contentType"));
        }
        if (operacionJSON.containsKey("nombreDestinatarioFirma")) {
            operacion.setNombreDestinatarioFirma(operacionJSON.getString("nombreDestinatarioFirma"));
        }
        if (operacionJSON.containsKey("asuntoMensajeFirmado")) {
            operacion.setAsuntoMensajeFirmado(operacionJSON.getString("asuntoMensajeFirmado"));
        }
        if (operacionJSON.containsKey("respuestaConRecibo")) {
            operacion.setRespuestaConRecibo(operacionJSON.getBoolean("respuestaConRecibo"));
        }
        if (operacionJSON.containsKey("emailSolicitante")) {
            operacion.setEmailSolicitante(operacionJSON.getString("emailSolicitante"));
        }
        return operacion;
    }

    public JSONObject toJSON () {
        logger.debug("toJSON");
        Map map = new HashMap();
        if(statusCode != null) map.put("statusCode", statusCode);
        if(message != null) {
            map.put("message", message);
        } else {
            if(statusCode != null && ResponseVS.SC_OK != statusCode) {
                map.put("message", getErrorValidacion());
            }
        }
        if(type != null) map.put("operacion", type.toString());
        if(urlDocumento != null) map.put("urlDocumento", urlDocumento);
        if(urlEnvioDocumento != null) map.put("urlEnvioDocumento", urlEnvioDocumento);
        if(asuntoMensajeFirmado != null) map.put("asuntoMensajeFirmado", asuntoMensajeFirmado);
        if(nombreDestinatarioFirma != null) map.put("nombreDestinatarioFirma", nombreDestinatarioFirma);
        if(respuestaConRecibo != null) map.put("respuestaConRecibo", respuestaConRecibo);
         if(urlTimeStampServer != null) map.put("urlTimeStampServer", urlTimeStampServer);
        if(args != null) map.put("args", args);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        if(evento != null) jsonObject.put("evento", evento.getDataMap());
        return jsonObject;
    }

    /**
     * @return the asuntoMensajeFirmado
     */
    public String getAsuntoMensajeFirmado() {
        return asuntoMensajeFirmado;
    }

    /**
     * @param asuntoMensajeFirmado the asuntoMensajeFirmado to set
     */
    public void setAsuntoMensajeFirmado(String asuntoMensajeFirmado) {
        this.asuntoMensajeFirmado = asuntoMensajeFirmado;
    }

    /**
     * @return the respuestaConRecibo
     */
    public boolean isRespuestaConRecibo() {
        return respuestaConRecibo;
    }

    /**
     * @param respuestaConRecibo the respuestaConRecibo to set
     */
    public void setRespuestaConRecibo(boolean respuestaConRecibo) {
        this.respuestaConRecibo = respuestaConRecibo;
    }

    /**
     * @return the emailSolicitante
     */
    public String getEmailSolicitante() {
        return emailSolicitante;
    }

    /**
     * @param emailSolicitante the emailSolicitante to set
     */
    public void setEmailSolicitante(String emailSolicitante) {
        this.emailSolicitante = emailSolicitante;
    }

    public String getErrorValidacion() {
        if(type == null) return ContextVS.INSTANCE.getString(
                "errorOperacionNoEncontrada"); 
        if(statusCode == null) return ContextVS.INSTANCE.getString(
                "errorCodigoEstadoNoEncontrado"); 
        switch (type) {
            case ANULAR_SOLICITUD_ACCESO:
            case ANULAR_VOTO:
                if(args[0] == null) return ContextVS.INSTANCE.getString(
                        "errorHashCertificadoVotoBase64NoEncontrado");
                contenidoFirma = VotingToolContext.INSTANCE.
                        getVoteCancelationInSession(args[0]);
                if(contenidoFirma == null)
                    return ContextVS.INSTANCE.getString(
                            "errorReciboNoEncontrado") + " " + args[0];
                break;
            case SOLICITUD_COPIA_SEGURIDAD:
                if(evento == null || evento.getEventoId() == null) return 
                        ContextVS.INSTANCE.getString("errorDatosEvento");
                if(emailSolicitante == null) return 
                        ContextVS.INSTANCE.getString("errorEmail");
                break;
            case GUARDAR_RECIBO_VOTO:
                return null;
            case CANCELAR_EVENTO:
                if(contenidoFirma == null) return 
                        ContextVS.INSTANCE.getString("errorDatosCancelacionEvento");
                return null;
            case ENVIO_VOTO_SMIME:
                try {
                   evento.genVote();
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    return ContextVS.INSTANCE.getString(
                            "errorPreparandoVoto") + " - " + ex.getMessage();
                }
            default:
                if(nombreDestinatarioFirma == null) return ContextVS.INSTANCE.
                        getString("errorDestinatarioNoEncontrado");
        }
        return null;
    }

    /**
     * @return the callerCallback
     */
    public String getCallerCallback() {
        return callerCallback;
    }

    /**
     * @param callerCallback the callerCallback to set
     */
    public void setCallerCallback(String callerCallback) {
        this.callerCallback = callerCallback;
    }


}

