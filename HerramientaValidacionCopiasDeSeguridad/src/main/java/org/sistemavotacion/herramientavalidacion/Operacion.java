package org.sistemavotacion.herramientavalidacion;

import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Operacion {
    
    private static Logger logger = LoggerFactory.getLogger(Operacion.class);
    
    public static enum Tipo {MENSAJE_HERRAMIENTA_VALIDACION(
            AppletHerramienta.getResourceBundle().getString("MENSAJE_APPLET")),
        MENSAJE_CIERRE_HERRAMIENTA_VALIDACION(AppletHerramienta.getResourceBundle().
                getString("MENSAJE_CIERRE_APPLET")),
        MOSTRAR_HERRAMIENTA_VALIDACION("");
    
        Tipo(String caption) {
            this.caption = caption;
        }
        
        Tipo( ) {  }
        
          
        String caption;
        
        public String getCaption() {
            return this.caption;
        }

    }
    
    public static final int SC_OK = 200;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ERROR_EJECUCION = 500;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;
    
    private Tipo tipo;
    private Integer codigoEstado;
    private String mensaje;
    private String urlDocumento;
    private String urlEnvioDocumento;
    private String nombreDestinatarioFirma;
    private String emailSolicitante;
    private String asuntoMensajeFirmado;
    private Boolean respuestaConRecibo = false;
    private JSONObject contenidoFirma; 
    private Evento evento;
    private String[] args;

    
    public Operacion() {}
    
    public Operacion(int codigoEstado) {
        this.codigoEstado = codigoEstado;
    }
    
    public Operacion(Tipo tipo) {
        this.tipo = tipo;
    }
    
    public Operacion(int codigoEstado, String mensaje) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
    }
    
    public Operacion(int codigoEstado, Tipo tipo, String mensaje) {
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
     * @return the evento
     */
    public Evento getEvento() {
        return evento;
    }

    /**
     * @param evento the evento to set
     */
    public void setEvento(Evento evento) {
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

    public static Operacion parse (String operacionStr) {
        logger.debug("- parse: " + operacionStr);
        if(operacionStr == null) return null;
        Operacion operacion = new Operacion();
        JSON datosJSON = JSONSerializer.toJSON(operacionStr);
        JSONObject operacionJSON = null;
        if(datosJSON instanceof JSONArray) {
            operacionJSON = ((JSONArray)datosJSON).getJSONObject(0);
        } else operacionJSON = (JSONObject)datosJSON;
        if (operacionJSON.containsKey("operacion")) {
            operacion.setTipo(Tipo.valueOf(operacionJSON.getString("operacion")));
        }
        if (operacionJSON.containsKey("args")) {
            JSONArray arrayArgs = operacionJSON.getJSONArray("args");
            String[] args = new String[arrayArgs.size()];
            for(int i = 0; i < arrayArgs.size(); i++) {
                args[i] = arrayArgs.getString(i);
            }
            operacion.setArgs(args);
        }
        if (operacionJSON.containsKey("codigoEstado")) {
            operacion.setCodigoEstado(operacionJSON.getInt("codigoEstado"));
        }
        if (operacionJSON.containsKey("mensaje")) {
            operacion.setMensaje(operacionJSON.getString("mensaje"));
        }   
        if (operacionJSON.containsKey("urlEnvioDocumento")) {
            operacion.setUrlEnvioDocumento(operacionJSON.getString("urlEnvioDocumento"));
        }  
        if (operacionJSON.containsKey("urlDocumento")) {
            operacion.setUrlDocumento(operacionJSON.getString("urlDocumento"));
        }  
        if (operacionJSON.containsKey("evento")) {
            Evento evento = Evento.parse(operacionJSON.getJSONObject("evento"));
            operacion.setEvento(evento);
        }  
        if (operacionJSON.containsKey("contenidoFirma")) 
             operacion.setContenidoFirma(operacionJSON.getJSONObject("contenidoFirma"));
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

    public JSONObject obtenerJSON () {
        Map map = new HashMap();
        if(codigoEstado != null) map.put("codigoEstado", codigoEstado);
        if(mensaje != null) map.put("mensaje", mensaje);
        if(tipo != null) map.put("operacion", tipo.toString());
        if(urlDocumento != null) map.put("urlDocumento", urlDocumento);
        if(urlEnvioDocumento != null) map.put("urlEnvioDocumento", urlEnvioDocumento);
        if(asuntoMensajeFirmado != null) map.put("asuntoMensajeFirmado", urlEnvioDocumento);
        if(nombreDestinatarioFirma != null) map.put("nombreDestinatarioFirma", nombreDestinatarioFirma);
        if(respuestaConRecibo != null) map.put("respuestaConRecibo", respuestaConRecibo);
        if(args != null) map.put("args", args);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        if(evento != null) jsonObject.put("evento", evento.obtenerJSON());
        return jsonObject;
    }
    
    public String obtenerJSONStr () {
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

}


