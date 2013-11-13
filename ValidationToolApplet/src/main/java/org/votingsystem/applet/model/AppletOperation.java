package org.votingsystem.applet.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.votingsystem.model.EventVSBase;
import org.votingsystem.model.OperationVS;
import org.votingsystem.util.StringUtils;
import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
/**
 *
 * @author jgzornoza
 */
public class AppletOperation implements OperationVS {
    
    private static Logger logger = Logger.getLogger(AppletOperation.class);
    
    public static enum Type {
        ACCESS_REQUEST(
                ContextVS.INSTANCE.getString("CANCEL_VOTE")),
        CANCEL_VOTE(
                ContextVS.INSTANCE.getString("ACCESS_REQUEST")),
        CONTROL_CENTER_ASSOCIATION(
                ContextVS.INSTANCE.getString("CONTROL_CENTER_ASSOCIATION")), 
        CONTROL_CENTER_STATE_CHANGE_SMIME(
                ContextVS.INSTANCE.getString("CONTROL_CENTER_STATE_CHANGE_SMIME")), 
        BACKUP_REQUEST(
                ContextVS.INSTANCE.getString("BACKUP_REQUEST")), 
        MANIFEST_PUBLISHING(
                ContextVS.INSTANCE.getString("MANIFEST_PUBLISHING")), 
        MANIFEST_SIGN(
                ContextVS.INSTANCE.getString("MANIFEST_SIGN")), 
        CLAIM_PUBLISHING(
                ContextVS.INSTANCE.getString("CLAIM_PUBLISHING")),
        SMIME_CLAIM_SIGNATURE(
                ContextVS.INSTANCE.getString("SMIME_CLAIM_SIGNATURE")),
        VOTING_PUBLISHING(
                ContextVS.INSTANCE.getString("VOTING_PUBLISHING")), 
        SEND_SMIME_VOTE(
                ContextVS.INSTANCE.getString("SEND_SMIME_VOTE")),
        APPLET_MESSAGE(
                ContextVS.INSTANCE.getString("APPLET_MESSAGE")), 
        VOTE_CANCELLATION(
                ContextVS.INSTANCE.getString("VOTE_CANCELLATION")),
        ACCESS_REQUEST_CANCELLATION(
                ContextVS.INSTANCE.getString("ACCESS_REQUEST_CANCELLATION")),
        EVENT_CANCELLATION(
                ContextVS.INSTANCE.getString("EVENT_CANCELLATION")),
        SAVE_VOTE_RECEIPT(
                ContextVS.INSTANCE.getString("SAVE_VOTE_RECEIPT")),
        APPLET_PAUSED_MESSAGE(
                ContextVS.INSTANCE.getString("APPLET_PAUSED_MESSAGE")),
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
                case CONTROL_CENTER_ASSOCIATION:
                    resultado = 
                        ContextVS.INSTANCE.getString("CONTROL_CENTER_ASSOCIATION");
                    break;
                case CONTROL_CENTER_STATE_CHANGE_SMIME:
                    resultado = ContextVS.INSTANCE.getString(
                            "CONTROL_CENTER_STATE_CHANGE_SMIME_FILE");
                    break;
                case BACKUP_REQUEST:
                    resultado = ContextVS.INSTANCE.getString(
                            "BACKUP_REQUEST_FILE");
                    break;
                case MANIFEST_PUBLISHING:
                    resultado = ContextVS.INSTANCE.getString(
                            "MANIFEST_PUBLISHING_FILE");
                    break;
                case MANIFEST_SIGN:
                    resultado = ContextVS.INSTANCE.getString(
                            "MANIFEST_SIGN_FILE");
                    break;
                case CLAIM_PUBLISHING:
                    resultado = ContextVS.INSTANCE.getString(
                            "CLAIM_PUBLISHING_FILE");
                    break;
                case SMIME_CLAIM_SIGNATURE:
                    resultado = ContextVS.INSTANCE.getString(
                            "SMIME_CLAIM_SIGNATURE_FILE");
                    break;
                case VOTING_PUBLISHING:
                    resultado = ContextVS.INSTANCE.getString(
                            "VOTING_PUBLISHING_FILE");
                    break;
                case SEND_SMIME_VOTE:
                    resultado = ContextVS.INSTANCE.getString(
                            "SEND_SMIME_VOTE_FILE");
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

    
    public AppletOperation() {}
    
    public AppletOperation(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public AppletOperation(Type type) {
        this.type = type;
    }
    
    public AppletOperation(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
    
    public AppletOperation(int statusCode, Type type, String message) {
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

    public static AppletOperation parse (String operacionStr) {
        logger.debug("- parse: " + operacionStr);
        if(operacionStr == null) return null;
        AppletOperation operacion = new AppletOperation();
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

