package org.sistemavotacion.modelo;

import static org.sistemavotacion.Contexto.*;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.util.VotacionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class Operacion {
    
    private static Logger logger = LoggerFactory.getLogger(Operacion.class);
    
    public static enum Tipo {ASOCIAR_CENTRO_CONTROL_SMIME(
                getString("ASOCIAR_CENTRO_CONTROL_SMIME")), 
        CAMBIO_ESTADO_CENTRO_CONTROL_SMIME(
                getString("CAMBIO_ESTADO_CENTRO_CONTROL_SMIME")), 
        SOLICITUD_COPIA_SEGURIDAD(
                getString("SOLICITUD_COPIA_SEGURIDAD")), 
        PUBLICACION_MANIFIESTO_PDF(
                getString("PUBLICACION_MANIFIESTO_PDF")), 
        FIRMA_MANIFIESTO_PDF(
                getString("FIRMA_MANIFIESTO_PDF")), 
        PUBLICACION_RECLAMACION_SMIME(
                getString("PUBLICACION_RECLAMACION_SMIME")),
        FIRMA_RECLAMACION_SMIME(
                getString("FIRMA_RECLAMACION_SMIME")), 
        PUBLICACION_VOTACION_SMIME(
                getString("PUBLICACION_VOTACION_SMIME")), 
        ENVIO_VOTO_SMIME(
                getString("ENVIO_VOTO_SMIME")),
        MENSAJE_MONITOR_DESCARGA_APPLET(
                getString("MENSAJE_MONITOR_DESCARGA_APPLET")), 
        MENSAJE_APPLET(
                getString("MENSAJE_APPLET")), 
        ANULAR_VOTO(
                getString("ANULAR_VOTO")),
        ANULAR_SOLICITUD_ACCESO(
                getString("ANULAR_SOLICITUD_ACCESO")),
        CANCELAR_EVENTO(
                getString("CANCELAR_EVENTO")),
        GUARDAR_RECIBO_VOTO(
                getString("GUARDAR_RECIBO_VOTO")),
        MENSAJE_CIERRE_APPLET(
                getString("MENSAJE_CIERRE_APPLET"));
    
        Tipo(String caption) {
            this.caption = caption;
        }
        
        Tipo( ) {  }
        
          
        String caption;
        
        public String getCaption() {
            return this.caption;
        }
        
        
        public String getNombreArchivoEnDisco() {
            String resultado = null;
            switch(this) {
                case ASOCIAR_CENTRO_CONTROL_SMIME:
                    resultado = 
                        getString("ASOCIAR_CENTRO_CONTROL_SMIME_FILE");
                    break;
                case CAMBIO_ESTADO_CENTRO_CONTROL_SMIME:
                    resultado = 
                        getString("CAMBIO_ESTADO_CENTRO_CONTROL_SMIME_FILE");
                    break;
                case SOLICITUD_COPIA_SEGURIDAD:
                    resultado = 
                        getString("SOLICITUD_COPIA_SEGURIDAD_FILE");
                    break;
                case PUBLICACION_MANIFIESTO_PDF:
                    resultado = 
                        getString("PUBLICACION_MANIFIESTO_PDF_FILE");
                    break;
                case FIRMA_MANIFIESTO_PDF:
                    resultado = 
                        getString("FIRMA_MANIFIESTO_PDF_FILE");
                    break;
                case PUBLICACION_RECLAMACION_SMIME:
                    resultado = 
                        getString("PUBLICACION_RECLAMACION_SMIME_FILE");
                    break;
                case FIRMA_RECLAMACION_SMIME:
                    resultado = 
                        getString("FIRMA_RECLAMACION_SMIME_FILE");
                    break;
                case PUBLICACION_VOTACION_SMIME:
                    resultado = 
                        getString("PUBLICACION_VOTACION_SMIME_FILE");
                    break;
                case ENVIO_VOTO_SMIME:
                    resultado = 
                        getString("ENVIO_VOTO_SMIME_FILE");
                    break;
                default:
                    this.toString();
            }
            return resultado;
        }
    }
    
    public static final int SC_OK = 200;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ERROR_EJECUCION = 500;
    public static final int SC_ERROR_ENVIO_VOTO = 570;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;
    
    private Tipo tipo;
    private Integer codigoEstado;
    private String mensaje;
    private String urlDocumento;
    private String urlTimeStampServer;
    private String urlServer;
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
        if (operacionJSON.containsKey("urlTimeStampServer")) {
            operacion.setUrlTimeStampServer(operacionJSON.getString("urlTimeStampServer"));
        }  
        if (operacionJSON.containsKey("urlServer")) {
            operacion.setUrlServer(operacionJSON.getString("urlServer"));
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
         if(urlTimeStampServer != null) map.put("urlTimeStampServer", urlTimeStampServer);
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

    public String getErrorValidacion() {
        if(tipo == null) return 
                        getString("errorOperacionNoEncontrada"); 
        if(codigoEstado == null) return 
                        getString("errorCodigoEstadoNoEncontrado"); 
        switch (tipo) {
            case ANULAR_SOLICITUD_ACCESO:
            case ANULAR_VOTO:
                if(args[0] == null) return 
                        getString("errorHashCertificadoVotoBase64NoEncontrado");
                if(VotacionHelper.getReciboVoto(args[0]) == null)
                    return 
                        getString("errorReciboNoEncontrado") + " " + args[0];
                contenidoFirma = VotacionHelper.obtenerAnuladorDeVotoJSON(args[0]);
                break;
            case SOLICITUD_COPIA_SEGURIDAD:
                if(evento == null || evento.getEventoId() == null) return 
                        getString("errorDatosEvento");
                if(emailSolicitante == null) return 
                        getString("errorEmail");
                break;
            case GUARDAR_RECIBO_VOTO:
                return null;
            case CANCELAR_EVENTO:
                if(contenidoFirma == null) return 
                        getString("errorDatosCancelacionEvento");
                return null;
            case ENVIO_VOTO_SMIME:
                try {
                   evento = VotacionHelper.prepararVoto(evento);
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    return 
                        getString("errorPreparandoVoto") + " - " + ex.getMessage();
                }
            default:
                if(nombreDestinatarioFirma == null) return 
                        getString("errorDestinatarioNoEncontrado");
        }
        return null;
    }


}


