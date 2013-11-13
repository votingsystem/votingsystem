package org.votingsystem.model;

import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailValidator;
import org.votingsystem.util.DateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.PKIXParameters;
import java.util.Date;

import javax.mail.MessagingException;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ResponseVS {
    
    public static final int SC_OK = 200;
    public static final int SC_OK_ANULACION_ACCESS_REQUEST = 270;
    public static final int SC_ERROR_REQUEST = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_ERROR_VOTE_REPEATED = 470;
    public static final int SC_CANCELLATION_REPEATED = 471;
    public static final int SC_NULL_REQUEST = 472;

    public static final int SC_ERROR           = 500;
    public static final int SC_ERROR_EXCEPCION = 500;
    public static final int SC_ERROR_TIMESTAMP = 570;    
    public static final int SC_PROCESSING      = 700;
    public static final int SC_CANCELLED       = 0;


    private int statusCode;
    private EventQueryResponseVS eventQueryResponse;
    private String message;
    private Object data;
    private TypeVS typeVS;
    private Date fecha;
    private Long eventoId;
    private SMIMEMessageWrapper smimeMessage;
    private ActorVS actorVS;
    private byte[] messageBytes;
    
    
    public ResponseVS(int statusCode,
                      String message, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.message = message;
        this.messageBytes = messageBytes;
    }

    public ResponseVS(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
    
    public ResponseVS(int statusCode, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
    }
    
    public ResponseVS(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public ResponseVS(int statusCode,
                      SMIMEMessageWrapper recibo, PKIXParameters params) throws Exception {
        this.statusCode = statusCode;
        this.setSmimeMessage(recibo);
        SignedMailValidator.ValidationResult validationResult = recibo.verify(params);        
        if (validationResult.isValidSignature()) {
            JSONObject resultadoJSON = new JSONObject(recibo.getSignedContent());
            Log.i("ResponseVS", resultadoJSON.toString());
            if (resultadoJSON.has("tipoRespuesta")) 
                typeVS = TypeVS.valueOf(resultadoJSON.getString("tipoRespuesta"));
            if (resultadoJSON.has("fecha"))  
                fecha = DateUtils.getDateFromString(resultadoJSON.getString("fecha"));
            if (resultadoJSON.has("message"))
                message = resultadoJSON.getString("message");
         } else Log.e("", " Error en la validación de la respuesta");
    }
    
    public ResponseVS(int statusCode, SMIMEMessageWrapper recibo) throws Exception {
        this.statusCode = statusCode;
        this.setSmimeMessage(recibo);
        if (recibo.isValidSignature()) {
            JSONObject resultadoJSON = new JSONObject(
                    recibo.getSignedContent());
            Log.e("ResponseVS", "ResultadoJSON: " + resultadoJSON.toString());
            if (resultadoJSON.has("tipoRespuesta")) 
                typeVS = TypeVS.valueOf(resultadoJSON.getString("tipoRespuesta"));
            if (resultadoJSON.has("fecha"))  
                fecha = DateUtils.getDateFromString(resultadoJSON.getString("fecha"));
            if (resultadoJSON.has("message"))
                message = resultadoJSON.getString("message");
         } else Log.e("ResponseVS","Error en la validación de la respuesta");
    }
    
    
    public ResponseVS() {  }
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
    
    
    public String toString () {
    	StringBuilder respuesta = new StringBuilder();
    	respuesta.append("Estado: ").append(statusCode).append(" - Mensaje: ").append(message);
    	if (typeVS != null) {
    		respuesta.append(" - TypeVS: ").append(typeVS.toString());
    	}
    	return respuesta.toString();
    }

    /**
     * @return the eventQueryResponse
     */
    public EventQueryResponseVS getEventQueryResponse() {
        return eventQueryResponse;
    }

    /**
     * @param eventQueryResponse the eventQueryResponse to set
     */
    public void setEventQueryResponse(EventQueryResponseVS eventQueryResponse) {
        this.eventQueryResponse = eventQueryResponse;
    }

    /**
     * @return the codigoEstadoHTTP
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @param codigoEstadoHTTP the codigoEstadoHTTP to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the mensajeDeError
     */
    public TypeVS getTypeVS() {
        return typeVS;
    }

    /**
     * @param mensajeDeError the mensajeDeError to set
     */
    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
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
     * @return the actorVS
     */
    public ActorVS getActorVS() {
        return actorVS;
    }

    /**
     * @param actorVS the actorVS to set
     */
    public void setActorVS(ActorVS actorVS) {
        this.actorVS = actorVS;
    }

    /**
     * @return the objeto
     */
    public Object getData() {
        return data;
    }

    /**
     * @param objeto the objeto to set
     */
    public void setData(Object data) {
        this.data = data;
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

    public void appendErrorMessage(String msg) {
        statusCode = SC_ERROR;
        if(message != null) message = message + " - " + msg;
        else message = msg;
    }
    
}
