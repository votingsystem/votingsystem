package org.votingsystem.model;

import org.apache.log4j.Logger;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import java.io.File;
import java.util.List;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ResponseVS<T> {
    
	private static Logger logger = Logger.getLogger(ResponseVS.class);
    
    public static final int SC_OK = 200;
    public static final int SC_OK_CANCEL_ACCESS_REQUEST = 270;
    public static final int SC_REQUEST_TIMEOUT  = 408;
    public static final int SC_ERROR_REQUEST = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_ERROR_VOTE_REPEATED = 470;
    public static final int SC_CANCELLATION_REPEATED = 471;
    public static final int SC_NULL_REQUEST = 472;
    public static final int SC_ERROR           = 500;
    public static final int SC_ERROR_TIMESTAMP = 570;
    public static final int SC_PROCESSING      = 700;
    public static final int SC_TERMINATED      = 710;
    public static final int SC_CANCELLED       = 0;
    public static final int SC_INITIALIZED     = 1;
    public static final int SC_PAUSED          = 10;
    
    private Integer statusCode;
    private StatusVS<?> status;
    private String message;
    private SMIMEMessageWrapper smimeMessage;
    private EventVS eventVS;
    private T data;
    private TypeVS type;
    private UserVS userVS;
    private byte[] messageBytes;
    private File file;
    private List<String> errorList;
        
    public ResponseVS () {  }

    public ResponseVS (int statusCode) {
        this.statusCode = statusCode;    
    }
    
    public ResponseVS (int statusCode, String msg) {
        this.statusCode = statusCode; 
        this.message = msg;
    }

    public ResponseVS (int statusCode, StatusVS<?> status, String msg) {
        this.statusCode = statusCode;
        this.status = status;
        this.message = msg;
    }

    public ResponseVS (int statusCode, StatusVS<?> status) {
        this.statusCode = statusCode;
        this.status = status;
    }

    public ResponseVS (int statusCode, T data) {
        this.statusCode = statusCode;
        this.data = data;
    }
        
    public ResponseVS (int statusCode, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
    }

    public String getMessage() {
        if(message == null && messageBytes != null) message = new String(messageBytes);
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public String toString () {
    	StringBuilder responseVS = new StringBuilder();
    	responseVS.append("statusCode: ").append(statusCode).append(" - Message: ").append(message);
    	if (type != null) {
    		responseVS.append(" - Type: ").append(type.toString());
    	}
    	return responseVS.toString();
    }

    public int getStatusCode() {
        return statusCode; 
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public TypeVS getType() {
        return type;
    }

    public void setType(TypeVS type) {
        this.type = type;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void appendMessage(String msg) {
        if(message != null) message = message + "\n" + msg;
        else message = msg;
    }

    public List<String> getErrorList() {
        return errorList;
    }

    public void setErrorList(List<String> errorList) {
        this.errorList = errorList;
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

    public EventVS getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

	public UserVS getUserVS() {
		return userVS;
	}

	public void setUserVS(UserVS userVS) {
		this.userVS = userVS;
	}

    public <E> StatusVS<E> getStatus() {
        return (StatusVS<E>)status;
    }

    public <E> void setStatus(StatusVS<E> status) {
        this.status = status;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}