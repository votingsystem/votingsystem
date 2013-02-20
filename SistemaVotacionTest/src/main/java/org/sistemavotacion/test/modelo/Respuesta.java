package org.sistemavotacion.test.modelo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.PKIXParameters;
import java.util.Date;
import javax.mail.MessagingException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailValidator;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class Respuesta {
    
    private static Logger logger = LoggerFactory.getLogger(Respuesta.class);
    
    public static final int SC_OK = 200;
    public static final int SC_ERROR = 400;
    public static final int SC_NOT_FOUND = 404;

    private int codigoEstado;
    private String mensaje;
    private Object objeto;
    private Tipo tipo;
    private Date fecha;
    private Long eventoId;
    private SMIMEMessageWrapper smimeMessage;
    private ActorConIP actorConIP;
    
    
    public Respuesta (int codigoEstado, String mensaje, Object objeto) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
        this.objeto = objeto;
    }

    public Respuesta (int codigoEstado, String mensaje) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
    }
    
    public Respuesta (int codigoEstado, 
        SMIMEMessageWrapper recibo, PKIXParameters params) throws Exception {
        this.codigoEstado = codigoEstado;
        this.smimeMessage = recibo;
        SignedMailValidator.ValidationResult validationResult = recibo.verify(params);        
        if (validationResult.isValidSignature()) {
            JSONObject resultadoJSON = (JSONObject)JSONSerializer.toJSON(
                    recibo.getSignedContent());
            logger.debug("Respuesta - resultadoJSON: " + resultadoJSON.toString());
            if (resultadoJSON.containsKey("tipoRespuesta")) 
                tipo = Tipo.valueOf(resultadoJSON.getString("tipoRespuesta"));
            if (resultadoJSON.containsKey("fecha"))  
                fecha = DateUtils.getDateFromString(resultadoJSON.getString("fecha"));
            if (resultadoJSON.containsKey("mensaje"))
                mensaje = resultadoJSON.getString("mensaje");
         } else logger.error(" Error en la validación de la respuesta");
    }
    
    public Respuesta (int codigoEstado, SMIMEMessageWrapper recibo) throws Exception {
        this.codigoEstado = codigoEstado;    
        this.smimeMessage = recibo;
        if (recibo.isValidSignature()) {
            JSONObject resultadoJSON = (JSONObject)JSONSerializer.toJSON(
                    recibo.getSignedContent());
            logger.debug("Respuesta - resultadoJSON: " + resultadoJSON.toString());
            if (resultadoJSON.containsKey("tipoRespuesta")) 
                tipo = Tipo.valueOf(resultadoJSON.getString("tipoRespuesta"));
            if (resultadoJSON.containsKey("fecha"))  
                fecha = DateUtils.getDateFromString(resultadoJSON.getString("fecha"));
            if (resultadoJSON.containsKey("mensaje"))
                mensaje = resultadoJSON.getString("mensaje");
         } else logger.error(" Error en la validación de la respuesta");
    }
    
    
    public Respuesta () {  }
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
    
    
    public String toString () {
    	StringBuilder respuesta = new StringBuilder();
    	respuesta.append("Estado: ").append(codigoEstado).append(" - Mensaje: ").append(mensaje);
    	if (tipo != null) {
    		respuesta.append(" - Tipo: ").append(tipo.toString());
    	}
    	return respuesta.toString();
    }

    /**
     * @return the codigoEstadoHTTP
     */
    public int getCodigoEstado() {
        return codigoEstado;
    }

    /**
     * @param codigoEstadoHTTP the codigoEstadoHTTP to set
     */
    public void setCodigoEstado(int codigoEstado) {
        this.codigoEstado = codigoEstado;
    }

    /**
     * @return the mensajeDeError
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * @param mensajeDeError the mensajeDeError to set
     */
    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public void setFecha(Date fecha) {
            this.fecha = fecha;
    }

    public Date getFecha() {
            return fecha;
    }

    public boolean isValidSignature () {
        boolean resultado = false;
        try {
            if (smimeMessage.isValidSignature()) {
                JSONObject resultadoJSON = (JSONObject)JSONSerializer.toJSON(
                        smimeMessage.getSignedContent());
                logger.debug("Respuesta - resultadoJSON: " + resultadoJSON.toString());
                if (resultadoJSON.containsKey("tipoRespuesta")) 
                    tipo = Tipo.valueOf(resultadoJSON.getString("tipoRespuesta"));
                if (resultadoJSON.containsKey("fecha"))  
                    fecha = DateUtils.getDateFromString(resultadoJSON.getString("fecha"));
                if (resultadoJSON.containsKey("mensaje"))
                    mensaje = resultadoJSON.getString("mensaje");
                resultado = true;
            } else logger.error(" Error en la validación de la respuesta");
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return resultado;
        }
        return resultado;
    }

    /**
     * @return the archivoRecibo
     */
    public File getArchivo() throws IOException, MessagingException {
        File archivoRecibo = null;
        if (smimeMessage != null) {
            archivoRecibo = new File("recibo");
            smimeMessage.writeTo(new FileOutputStream(archivoRecibo));
        }
        return archivoRecibo;
    }
    
    /**
     * @return the eventoId
     */
    public Long getEventoId() {
        return eventoId;
    }

    /**
     * @param eventoId the eventoId to set
     */
    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    /**
     * @return the actorConIP
     */
    public ActorConIP getActorConIP() {
        return actorConIP;
    }

    /**
     * @param actorConIP the actorConIP to set
     */
    public void setActorConIP(ActorConIP actorConIP) {
        this.actorConIP = actorConIP;
    }

    /**
     * @return the objeto
     */
    public Object getObjeto() {
        return objeto;
    }

    /**
     * @param objeto the objeto to set
     */
    public void setObjeto(Object objeto) {
        this.objeto = objeto;
    }
}
