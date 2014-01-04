package org.votingsystem.model;

import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailValidator;

import java.security.cert.PKIXParameters;
import java.util.Date;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ResponseVS<T> {

    public static final int SC_OK                       = 200;
    public static final int SC_OK_WITHOUT_BODY          = 204;
    public static final int SC_OK_CANCEL_ACCESS_REQUEST = 270;
    public static final int SC_REQUEST_TIMEOUT          = 408;
    public static final int SC_ERROR_REQUEST            = 400;
    public static final int SC_NOT_FOUND                = 404;
    public static final int SC_ERROR_REQUEST_REPEATED   = 409;
    public static final int SC_EXCEPTION                = 490;
    public static final int SC_NULL_REQUEST             = 472;
    public static final int SC_ERROR                    = 500;
    public static final int SC_CONNECTION_TIMEOUT       = 522;
    public static final int SC_ERROR_TIMESTAMP          = 570;
    public static final int SC_PROCESSING               = 700;
    public static final int SC_TERMINATED               = 710;
    public static final int SC_CANCELLED                = 0;
    public static final int SC_INITIALIZED              = 1;
    public static final int SC_PAUSED                   = 10;


    private int statusCode;
    private String status;
    private EventVSResponse eventQueryResponse;
    private String message;
    private T data;
    private TypeVS typeVS;
    private Long eventId;
    private SMIMEMessageWrapper smimeMessage;
    private ContentTypeVS contentType = ContentTypeVS.TEXT;
    private ActorVS actorVS;
    private byte[] messageBytes;

    public ResponseVS() {  }
    
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
    
    public ResponseVS(int statusCode, SMIMEMessageWrapper recibo, PKIXParameters params) throws Exception {
        this.statusCode = statusCode;
        this.setSmimeMessage(recibo);
        SignedMailValidator.ValidationResult validationResult = recibo.verify(params);        
        if (validationResult.isValidSignature()) {
            JSONObject resultadoJSON = new JSONObject(recibo.getSignedContent());
            Log.d("", " Receipt OK");
         } else Log.e("", " Receipt with errors");
    }

    public String getMessage() {
        if(message == null && messageBytes != null) return new String(messageBytes);
        else return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    
    public String toString () {
    	StringBuilder respuesta = new StringBuilder();
    	respuesta.append("State: ").append(statusCode).append(" - Mensaje: ").append(message);
    	if (typeVS != null) {
    		respuesta.append(" - TypeVS: ").append(typeVS.toString());
    	}
    	return respuesta.toString();
    }

    public EventVSResponse getEventQueryResponse() {
        return eventQueryResponse;
    }

    public void setEventQueryResponse(EventVSResponse eventQueryResponse) {
        this.eventQueryResponse = eventQueryResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public Long getEventVSId() {
        return eventId;
    }

    public void setEventVSId(Long eventId) {
        this.eventId = eventId;
    }

    public ActorVS getActorVS() {
        return actorVS;
    }

    public void setActorVS(ActorVS actorVS) {
        this.actorVS = actorVS;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ContentTypeVS getContentType() {
        return contentType;
    }

    public void setContentType(ContentTypeVS contentType) {
        this.contentType = contentType;
    }

}
