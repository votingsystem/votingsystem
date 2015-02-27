package org.votingsystem.model;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;

import javax.persistence.*;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="ResponseVS")
public class ResponseVS<T> implements Serializable {

    public static final long serialVersionUID = 1L;
    
	private static Logger log = Logger.getLogger(ResponseVS.class);


    public enum Status {
        OK, ERROR
    }
    
    public static final int SC_OK                       = 200;
    public static final int SC_OK_WITHOUT_BODY          = 204;
    public static final int SC_OK_CANCEL_ACCESS_REQUEST = 270;
    public static final int SC_MESSAGE_FROM_VS          = 277;
    public static final int SC_REQUEST_TIMEOUT          = 408;
    public static final int SC_ERROR_REQUEST            = 400;
    public static final int SC_NOT_FOUND                = 404;
    public static final int SC_PRECONDITION_FAILED      = 412;
    public static final int SC_ERROR_REQUEST_REPEATED   = 409;
    public static final int SC_EXCEPTION                = 490;
    public static final int SC_NULL_REQUEST             = 472;
    public static final int SC_COOIN_EXPENDED           = 473;
    public static final int SC_ERROR                    = 500;
    public static final int SC_CONNECTION_TIMEOUT       = 522;
    public static final int SC_ERROR_TIMESTAMP          = 570;
    public static final int SC_PROCESSING               = 700;
    public static final int SC_TERMINATED               = 710;
    public static final int SC_WS_CONNECTION_INIT_OK    = 800;
    public static final int SC_WS_MESSAGE_SEND_OK       = 801;
    public static final int SC_WS_MESSAGE_ENCRYPTED     = 810;
    public static final int SC_WS_CONNECTION_INIT_ERROR = 840;
    public static final int SC_WS_CONNECTION_NOT_FOUND  = 841;
    public static final int SC_CANCELLED                = 0;
    public static final int SC_INITIALIZED              = 1;
    public static final int SC_PAUSED                   = 10;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="statusCode") private Integer statusCode;
    @Column(name="reason", columnDefinition="TEXT") private String reason;
    @Column(name="metaInf", columnDefinition="TEXT") private String metaInf;
    @Column(name="url") private String url;
    @Column(name="message", columnDefinition="TEXT") private String message;
    @Column(name="typeVS") @Enumerated(EnumType.STRING) private TypeVS type;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @Column(name="messageBytes") @Lob private byte[] messageBytes;
    @OneToOne private MessageSMIME messageSMIME;
    @Transient private StatusVS<?> status;
    @Transient private JSON messageJSON;
    @Transient private SMIMEMessage smimeMessage;
    @Transient private EventVS eventVS;
    @Transient private T data;
    @Transient private ContentTypeVS contentType = ContentTypeVS.HTML;
    @Transient private File file;
    @Transient private List<String> errorList;
        
    public ResponseVS () {  }

    public ResponseVS (int statusCode) {
        this.statusCode = statusCode;    
    }

    public ResponseVS (int statusCode, ContentTypeVS contentType) {
        this.statusCode = statusCode;
        this.contentType = contentType;
    }

    public ResponseVS (int statusCode, String msg) {
        this.statusCode = statusCode;
        this.message = msg;
    }

    public ResponseVS (int statusCode, String msg, T data) {
        this.statusCode = statusCode; 
        this.message = msg;
        this.data = data;
    }

    public ResponseVS (TypeVS typeVS) {
        this.type = typeVS;
    }

    public ResponseVS (Integer statusCode, TypeVS typeVS, T data) {
        this.statusCode = statusCode;
        this.type = typeVS;
        this.data = data;
    }

    public ResponseVS (TypeVS typeVS, Integer statusCode, String msg) {
        this.statusCode = statusCode;
        this.message = msg;
        this.type = typeVS;
    }

    public ResponseVS (Integer statusCode, String msg, ContentTypeVS contentType) {
        this.statusCode = statusCode;
        this.message = msg;
        this.contentType = contentType;
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

    public ResponseVS (int statusCode, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
    }
        
    public ResponseVS (int statusCode, byte[] messageBytes, ContentTypeVS contentType) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
        this.contentType = contentType;
    }

    public String getMessage() {
        if(message == null && messageBytes != null) {
            try {
                message = new String(messageBytes, "UTF-8");
            } catch(Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        return message;
    }

    public JSON getMessageJSON() {
        if(messageJSON != null) return messageJSON;
        String message = getMessage();
        if(message != null) return JSONSerializer.toJSON(message.trim());
        return null;
    }

    public void setMessageJSON(JSON jsonObject) {
        this.messageJSON = jsonObject;
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

    public Integer getStatusCode() {
        return statusCode; 
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public TypeVS getType() {
        return type;
    }

    public ResponseVS setType(TypeVS type) {
        this.type = type;
        return this;
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

    public byte[] getMessageBytes() throws Exception {
        if(contentType!= null && contentType.isSigned() && messageBytes == null && messageSMIME != null)
            return messageSMIME.getSMIME().getBytes();
        if(messageBytes == null && message != null) return message.getBytes();
        return messageBytes;
    }

    public void setMessageBytes(byte[] messageBytes) {
        this.messageBytes = messageBytes;
    }

    public SMIMEMessage getSMIME() {
        return smimeMessage;
    }

    public void setSMIME(SMIMEMessage smimeMessage) {
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

	public ResponseVS setUserVS(UserVS userVS) {
		this.userVS = userVS;
        return this;
	}

    public <E> StatusVS<E> getStatus() {
        return (StatusVS<E>)status;
    }

    public <E> ResponseVS setStatus(StatusVS<E> status) {
        this.status = status;
        return this;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ContentTypeVS getContentType() {
        return contentType;
    }

    public void setContentType(ContentTypeVS contentType) {
        this.contentType = contentType;
    }

    public String getReason() {
        return reason;
    }

    public ResponseVS setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getMetaInf() {
        return metaInf;
    }

    public ResponseVS setMetaInf(String metaInf) {
        this.metaInf = metaInf;
        return this;
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public ResponseVS setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MessageSMIME refreshMessageSMIME() throws Exception {
        messageSMIME.getSMIME().setMessageID("/messageSMIME/" + messageSMIME.getId());
        messageSMIME.setContent(messageSMIME.getSMIME().getBytes());
        if(eventVS != null) messageSMIME.setEventVS(eventVS);
        if(type != null) messageSMIME.setType(type);
        if(reason != null) messageSMIME.setReason(reason);
        if(metaInf != null) messageSMIME.setMetaInf(metaInf);
        return messageSMIME;
    }

    public static ResponseVS OK(byte[] messageBytes) {
        return new ResponseVS(ResponseVS.SC_OK, messageBytes);
    }

    public static ResponseVS ERROR(String message) {
        return new ResponseVS(ResponseVS.SC_ERROR, message);
    }

    public static ResponseVS ERROR_REQUEST(String message) {
        return new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message);
    }

    public static ResponseVS ALERT(String message, String metaInf) {
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR, message);
        responseVS.setMetaInf(metaInf);
        responseVS.setType(TypeVS.ALERT);
        return responseVS;
    }

    public static ResponseVS EXCEPTION(String controller, Map actionMap, Exception exception, Throwable rootCause) {
        String action = (actionMap == null)?null:(String) actionMap.values().iterator().next();
        String metaInf = "EXCEPTION_" + controller + "Controller_" + action + "Action_" +
                rootCause.getClass().getSimpleName();
        if(exception instanceof ExceptionVS && ((ExceptionVS)exception).getMetInf() != null) {
            metaInf = metaInf + "_" +((ExceptionVS)exception).getMetInf();
            log.error(metaInf);
        } else log.error(metaInf, rootCause);
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, rootCause.getMessage());
        responseVS.setReason(rootCause.getMessage());
        responseVS.setMetaInf(metaInf);
        responseVS.setType(TypeVS.EXCEPTION);
        return responseVS;
    }

    public static ResponseVS EXCEPTION(String controller, String action, Exception exception, Throwable rootCause) {
        Map actionMap = new HashMap<>();
        actionMap.put("", action);
        return EXCEPTION(controller, actionMap, exception, rootCause);
    }

}