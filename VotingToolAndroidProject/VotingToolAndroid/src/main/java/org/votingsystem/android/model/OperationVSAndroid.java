package org.votingsystem.android.model;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.model.OperationVS;
import org.votingsystem.util.StringUtils;

import java.text.ParseException;


/**
 *
 * @author jgzornoza
 */
public class OperationVSAndroid implements OperationVS {

	public static final String TAG = "Operacion";

    public static final String OPERATION_KEY   = "operationKey";
    
    public static enum Tipo {ASOCIAR_CENTRO_CONTROL,
        CAMBIO_ESTADO_CENTRO_CONTROL_SMIME, 
        SOLICITUD_COPIA_SEGURIDAD, 
        PUBLICACION_MANIFIESTO_PDF, 
        FIRMA_MANIFIESTO_PDF, 
        CREAR_RECLAMACION,
        CREAR_VOTACION,
        CREAR_MANIFIESTO,
        PUBLICACION_RECLAMACION_SMIME,
        FIRMA_RECLAMACION_SMIME, 
        PUBLICACION_VOTACION_SMIME, 
        CANCELAR_EVENTO, FIRMAR_MANIFIESTO,
        ENVIO_VOTO_SMIME, SOLICITUD_ACCESO;
    
        Tipo(String caption) {
            this.caption = caption;
        }
        
        Tipo( ) {  }
          
        String caption;
        
        public String getCaption() {
            return this.caption;
        }
        
    }
    
    public static final int SC_PING = 0;
    public static final int SC_OK = 200;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ERROR = 500;
    public static final int SC_ERROR_ENVIO_VOTO = 570;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;
    
    private Tipo tipo;
    private Integer codigoEstado;
    private String mensaje;
    private String urlDocumento;
    private String urlTimeStampServer;
    private String urlEnvioDocumento;
    private String nombreDestinatarioFirma;
    private String emailSolicitante;
    private String asuntoMensajeFirmado;
    private Boolean respuestaConRecibo = false;
    private JSONObject contenidoFirma; 
    private EventVSAndroid eventVSAndroid;
    private String sessionId;
    private String[] args;

    
    public OperationVSAndroid() {}
    
    public OperationVSAndroid(int codigoEstado) {
        this.codigoEstado = codigoEstado;
    }
    
    public OperationVSAndroid(Tipo tipo) {
        this.tipo = tipo;
    }
    
    public OperationVSAndroid(String tipo) {
        this.tipo = Tipo.valueOf(tipo);
    }
    
    public OperationVSAndroid(int codigoEstado, String mensaje) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
    }
    
    public OperationVSAndroid(int codigoEstado, Tipo tipo, String mensaje) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
        this.tipo = tipo;
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
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * @param urlTimeStampServer the urlTimeStampServer to set
     */
    public void setUrlTimeStampServer(String urlTimeStampServer) {
        this.urlTimeStampServer = urlTimeStampServer;
    }
     
    /**
     * @return the tipo
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * @param tipo the tipo to set
     */
    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }
    
    /**
     * @param tipo the tipo to set
     */
    public void setTipo(String tipo) {
        this.tipo = Tipo.valueOf(tipo);
    }

    /**
     * @return the codigoEstado
     */
    public Integer getCodigoEstado() {
        return codigoEstado;
    }

    /**
     * @param codigoEstado the codigoEstado to set
     */
    public void setCodigoEstado(Integer codigoEstado) {
        this.codigoEstado = codigoEstado;
    }

    /**
     * @return the mensaje
     */
    public String getMensaje() {
        return mensaje;
    }
    
    /**
     * @param mensaje the mensaje to set
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
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
     * @return the eventVSAndroid
     */
    public EventVSAndroid getEventVSAndroid() {
        return eventVSAndroid;
    }

    /**
     * @param eventVSAndroid the eventVSAndroid to set
     */
    public void setEventVSAndroid(EventVSAndroid eventVSAndroid) {
        this.eventVSAndroid = eventVSAndroid;
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

    public static OperationVSAndroid parse (String operacionStr) throws JSONException, ParseException {
		Log.d(TAG + ".parse(...) ", "operacionStr: " + operacionStr);
        if(operacionStr == null) return null;
        OperationVSAndroid operacion = new OperationVSAndroid();
        JSONObject operacionJSON = new JSONObject(operacionStr);
        if (operacionJSON.has("operacion")) {
            operacion.setTipo(Tipo.valueOf(operacionJSON.getString("operacion")));
        }
        if (operacionJSON.has("args")) {
            JSONArray arrayArgs = operacionJSON.getJSONArray("args");
            String[] args = new String[arrayArgs.length()];
            for(int i = 0; i < arrayArgs.length(); i++) {
                args[i] = arrayArgs.getString(i);
            }
            operacion.setArgs(args);
        }
        if (operacionJSON.has("codigoEstado")) {
            operacion.setCodigoEstado(operacionJSON.getInt("codigoEstado"));
        }
        if (operacionJSON.has("mensaje")) {
            operacion.setMensaje(operacionJSON.getString("mensaje"));
        }   
        if (operacionJSON.has("urlEnvioDocumento")) {
            operacion.setUrlEnvioDocumento(operacionJSON.getString("urlEnvioDocumento"));
        }  
        if (operacionJSON.has("urlDocumento")) {
            operacion.setUrlDocumento(operacionJSON.getString("urlDocumento"));
        }  
        if (operacionJSON.has("urlTimeStampServer")) {
            operacion.setUrlTimeStampServer(operacionJSON.getString("urlTimeStampServer"));
        }  
        if (operacionJSON.has("eventVSAndroid")) {
            EventVSAndroid eventVSAndroid = EventVSAndroid.parse(operacionJSON.getJSONObject("eventVSAndroid"));
            operacion.setEventVSAndroid(eventVSAndroid);
        }  
        if (operacionJSON.has("contenidoFirma")) 
             operacion.setContenidoFirma(operacionJSON.getJSONObject("contenidoFirma"));
        if (operacionJSON.has("nombreDestinatarioFirma")) {
            operacion.setNombreDestinatarioFirma(operacionJSON.getString("nombreDestinatarioFirma"));
        }
        if (operacionJSON.has("asuntoMensajeFirmado")) {
            operacion.setAsuntoMensajeFirmado(operacionJSON.getString("asuntoMensajeFirmado"));
        }
        if (operacionJSON.has("respuestaConRecibo")) {
            operacion.setRespuestaConRecibo(operacionJSON.getBoolean("respuestaConRecibo"));
        }
        if (operacionJSON.has("emailSolicitante")) {
            operacion.setEmailSolicitante(operacionJSON.getString("emailSolicitante"));
        }
        if (operacionJSON.has("sessionId")) {
        	operacion.setSessionId(operacionJSON.getString("sessionId"));
        }
        return operacion;
    }

    public JSONObject obtenerJSON () throws JSONException {
    	JSONObject jsonObject = new JSONObject();
        if(codigoEstado != null) jsonObject.put("codigoEstado", codigoEstado);
        if(mensaje != null) jsonObject.put("mensaje", mensaje);
        if(tipo != null) jsonObject.put("operacion", tipo.toString());
        if(urlDocumento != null) jsonObject.put("urlDocumento", urlDocumento);
        if(urlEnvioDocumento != null) jsonObject.put("urlEnvioDocumento", urlEnvioDocumento);
        if(asuntoMensajeFirmado != null) jsonObject.put("asuntoMensajeFirmado", urlEnvioDocumento);
        if(nombreDestinatarioFirma != null) jsonObject.put("nombreDestinatarioFirma", nombreDestinatarioFirma);
        if(respuestaConRecibo != null) jsonObject.put("respuestaConRecibo", respuestaConRecibo);
        if(urlTimeStampServer != null) jsonObject.put("urlTimeStampServer", urlTimeStampServer);
        if(args != null) jsonObject.put("args", args);
        if(sessionId != null) jsonObject.put("sessionId", sessionId);
        
        if(eventVSAndroid != null) jsonObject.put("eventVSAndroid", eventVSAndroid.toJSON());
        return jsonObject;
    }
    
    public String obtenerJSONStr () throws JSONException {
        JSONObject operacionJSON = obtenerJSON();
        if(operacionJSON == null) return null;
        else return operacionJSON.toString();
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
    
    public String getErrorValidacion(Context context) {
        if(tipo == null) return context.getString(R.string.errorOperacionNoEncontrada);
        if(codigoEstado == null) return context.getString(R.string.errorCodigoEstadoNoEncontrado);
        switch (tipo) {
            case SOLICITUD_COPIA_SEGURIDAD:
                if(eventVSAndroid == null || eventVSAndroid.getEventoId() == null) return
                        context.getString(R.string.errorDatosEvento);
                if(emailSolicitante == null) return
                        context.getString(R.string.errorEmail);
                break;
            case CANCELAR_EVENTO:
                if(contenidoFirma == null) return
                        context.getString(R.string.errorDatosCancelacionEvento);
                return null;
            default:
                if(nombreDestinatarioFirma == null) return
                        context.getString(R.string.errorDestinatarioNoEncontrado);
        }
        return null;
    }


}


