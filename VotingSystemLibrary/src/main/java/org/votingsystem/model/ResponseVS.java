package org.votingsystem.model;

import java.io.File;
import java.util.List;
import org.apache.log4j.Logger;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
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

    //Simulation status codes
    public static final int SC_SIMULATION_INITIATED = 275;
    public static final int SC_SIMULATION_RUNNING = 475;
    
    private Integer statusCode;
    private String message;
    private SMIMEMessageWrapper smimeMessage;
    private EventVS eventVS;
    private T data;
    private TypeVS type;
    private UserVS userVS;

    private byte[] messageBytes;
    private File archivo;
    private List<String> errorList;
        
    public ResponseVS () {  }
    
        
    public ResponseVS (Integer statusCode) {
        this.statusCode = statusCode;    
    }
    
    public ResponseVS (Integer statusCode, String msg) {
        this.statusCode = statusCode; 
        this.message = msg;
    }
    
    public ResponseVS (int statusCode, T data) {
        this.statusCode = statusCode;
        this.data = data;
    }


    public ResponseVS (int statusCode, String message, Object objeto) {
        this.statusCode = statusCode;
        this.message = message;
    }
        
    public ResponseVS (int statusCode, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
    }
           
    public ResponseVS (int statusCode, String message, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.message = message;
        this.messageBytes = messageBytes;
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
    
    
    public String toString () {
    	StringBuilder responseVS = new StringBuilder();
    	responseVS.append("Estado: ").append(statusCode).append(" - Message: ").append(message);
    	if (type != null) {
    		responseVS.append(" - Type: ").append(type.toString());
    	}
    	return responseVS.toString();
    }

    /**
     * @return the statusCodeHTTP
     */
    public int getStatusCode() {
        return statusCode; 
    }

    /**
     * @param statusCodeHTTP the statusCodeHTTP to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the messageDeError
     */
    public TypeVS getType() {
        return type;
    }

    /**
     * @param messageDeError the messageDeError to set
     */
    public void setType(TypeVS type) {
        this.type = type;
    }

    /**
     * @return the archivo
     */
    public File getArchivo() {
        return archivo;
    }

    /**
     * @param archivo the archivo to set
     */
    public void setArchivo(File archivo) {
        this.archivo = archivo;
    }


    /**
     * @return the data
     */
    public T getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(T data) {
        this.data = data;
    }

    
    public void appendMessage(String msg) {
        if(message != null) message = message + "\n" + msg;
        else message = msg;
    }
    
    public void appendErrorMessage(String msg) {
        statusCode = SC_ERROR;
        if(message != null) message = message + " - " + msg;
        else message = msg;
    }

    /**
     * @return the errorList
     */
    public List<String> getErrorList() {
        return errorList;
    }

    /**
     * @param errorList the errorList to set
     */
    public void setErrorList(List<String> errorList) {
        this.errorList = errorList;
    }

    /**
     * @return the messageBytes
     */
    public byte[] getMessageBytes() {
        return messageBytes;
    }

    /**
     * @param messageBytes the messageBytes to set
     */
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

}