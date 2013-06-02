package org.sistemavotacion.modelo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.PKIXParameters;
import java.util.Date;
import javax.mail.MessagingException;
import org.json.JSONObject;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailValidator;
import org.sistemavotacion.util.DateUtils;
import android.util.Log;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Respuesta {
    
    public static final int SC_OK = 200;
    public static final int SC_OK_ANULACION_SOLICITUD_ACCESO = 270;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ANULACION_REPETIDA = 471;
    public static final int SC_NULL_REQUEST = 472;
    
    public static final int SC_ERROR_EJECUCION = 500;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;

    public static final int SC_ERROR = 400;

    private int codigoEstado;
    private Consulta consulta; 
    private String mensaje;
    private Object objeto;
    private Tipo tipo;
    private Date fecha;
    private Long eventoId;
    private SMIMEMessageWrapper smimeMessage;
    private ActorConIP actorConIP;
    private byte[] messageBytes;
    
    
    public Respuesta (int codigoEstado, String mensaje, Object objeto) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
        this.objeto = objeto;
    }

    public Respuesta (int codigoEstado, String mensaje) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
    }
    
    public Respuesta (int codigoEstado, byte[] messageBytes) {
        this.codigoEstado = codigoEstado;
        this.messageBytes = messageBytes;
    }
    
    public Respuesta (int codigoEstado) {
        this.codigoEstado = codigoEstado;
    }
    
    public Respuesta (int codigoEstado, 
        SMIMEMessageWrapper recibo, PKIXParameters params) throws Exception {
        this.codigoEstado = codigoEstado;
        this.setSmimeMessage(recibo);
        SignedMailValidator.ValidationResult validationResult = recibo.verify(params);        
        if (validationResult.isValidSignature()) {
            JSONObject resultadoJSON = new JSONObject(recibo.getSignedContent());
            Log.i("Respuesta", resultadoJSON.toString());
            if (resultadoJSON.has("tipoRespuesta")) 
                tipo = Tipo.valueOf(resultadoJSON.getString("tipoRespuesta"));
            if (resultadoJSON.has("fecha"))  
                fecha = DateUtils.getDateFromString(resultadoJSON.getString("fecha"));
            if (resultadoJSON.has("mensaje"))
                mensaje = resultadoJSON.getString("mensaje");
         } else Log.e("", " Error en la validación de la respuesta");
    }
    
    public Respuesta (int codigoEstado, SMIMEMessageWrapper recibo) throws Exception {
        this.codigoEstado = codigoEstado;    
        this.setSmimeMessage(recibo);
        if (recibo.isValidSignature()) {
            JSONObject resultadoJSON = new JSONObject(
                    recibo.getSignedContent());
            Log.e("Respuesta", "ResultadoJSON: " + resultadoJSON.toString());
            if (resultadoJSON.has("tipoRespuesta")) 
                tipo = Tipo.valueOf(resultadoJSON.getString("tipoRespuesta"));
            if (resultadoJSON.has("fecha"))  
                fecha = DateUtils.getDateFromString(resultadoJSON.getString("fecha"));
            if (resultadoJSON.has("mensaje"))
                mensaje = resultadoJSON.getString("mensaje");
         } else Log.e("Respuesta","Error en la validación de la respuesta");
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
     * @return the consulta
     */
    public Consulta getConsulta() {
        return consulta;
    }

    /**
     * @param consulta the consulta to set
     */
    public void setConsulta(Consulta consulta) {
        this.consulta = consulta;
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


    /**
     * @return the archivoRecibo
     */
    public File getArchivo() throws IOException, MessagingException {
        File archivoRecibo = null;
        if (getSmimeMessage() != null) {
            archivoRecibo = new File("recibo");
            getSmimeMessage().writeTo(new FileOutputStream(archivoRecibo));
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

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public void setMessageBytes(byte[] messageBytes) {
		this.messageBytes = messageBytes;
	}

	public SMIMEMessageWrapper getSmimeMessage() {
		return smimeMessage;
	}

	public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) {
		this.smimeMessage = smimeMessage;
	}
}
