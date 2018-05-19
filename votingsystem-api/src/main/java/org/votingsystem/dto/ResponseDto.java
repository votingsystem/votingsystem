package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.http.ContentType;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ExceptionBase;
import org.votingsystem.util.AppCode;
import org.votingsystem.util.JSON;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.SystemOperation;
import org.votingsystem.xml.XML;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@JacksonXmlRootElement(localName = "Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDto<T> implements Serializable {

    public static final long serialVersionUID = 1L;

    private static Logger log = java.util.logging.Logger.getLogger(ResponseDto.class.getName());


    public enum Status {
        OK, ERROR
    }
    
    public static final int SC_OK                       = 200;
    public static final int SC_MESSAGE_FROM_VS          = 277;
    public static final int SC_REQUEST_TIMEOUT          = 408;
    public static final int SC_ERROR_REQUEST            = 400;
    public static final int SC_NOT_FOUND                = 404;
    public static final int SC_PRECONDITION_FAILED      = 412;
    public static final int SC_ERROR_REQUEST_REPEATED   = 409;
    public static final int SC_EXCEPTION                = 490;
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

    public static final int SC_CANCELED                 = 0;
    public static final int SC_INITIALIZED              = 1;
    public static final int SC_PAUSED                   = 10;

    @JacksonXmlProperty(localName = "OperationType", isAttribute = true)
    private SystemOperation type;
    @JacksonXmlProperty(localName = "StatusCode", isAttribute = true)
    private Integer statusCode;
    @JacksonXmlProperty(localName = "Reason")
    private String reason;
    @JacksonXmlProperty(localName = "Code", isAttribute = true)
    private AppCode code;
    @JacksonXmlProperty(localName = "MetaInf")
    private String metaInf;
    @JacksonXmlProperty(localName = "Message")
    private String message;
    @JacksonXmlProperty(localName = "Caption")
    private String caption;
    @JacksonXmlProperty(localName = "base64Data")
    private String base64Data;

    @JsonIgnore
    private User user;
    @JsonIgnore
    private byte[] messageBytes;
    @JsonIgnore
    private T data;
    @JsonIgnore
    private ContentType contentType = ContentType.HTML;
    @JsonIgnore
    private File file;

        
    public ResponseDto() {  }

    public ResponseDto(int statusCode) {
        this.statusCode = statusCode;    
    }

    public ResponseDto(int statusCode, ContentType contentType) {
        this.statusCode = statusCode;
        this.contentType = contentType;
    }

    public ResponseDto(int statusCode, String msg) {
        this.statusCode = statusCode;
        this.message = msg;
    }

    public ResponseDto(int statusCode, String msg, T data) {
        this.statusCode = statusCode; 
        this.message = msg;
        this.data = data;
    }

    public ResponseDto(AppCode appCode, String message) {
        this.code = appCode;
        this.statusCode = appCode.getStatusCode();
        this.message = message;
    }

    public ResponseDto(OperationType operationType) {
        this.type = operationType;
    }

    public ResponseDto(Integer statusCode, OperationType operationType, T data) {
        this.statusCode = statusCode;
        this.type = operationType;
        this.data = data;
    }

    public ResponseDto(OperationType operationType, Integer statusCode, String msg) {
        this.statusCode = statusCode;
        this.message = msg;
        this.type = operationType;
    }

    public ResponseDto(Integer statusCode, String msg, ContentType contentType) {
        this.statusCode = statusCode;
        this.message = msg;
        this.contentType = contentType;
    }

    public ResponseDto(Integer statusCode, String msg, String contentTypeStr) {
        this.statusCode = statusCode;
        this.message = msg;
        try {
            this.contentType = ContentType.getByName(contentTypeStr);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public ResponseDto(int statusCode, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
    }
        
    public ResponseDto(int statusCode, byte[] messageBytes, ContentType contentType) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
        this.contentType = contentType;
    }

    public ResponseDto(Integer statusCode, byte[] messageBytes, String contentTypeStr) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
        try {
            this.contentType = ContentType.getByName(contentTypeStr);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public ResponseDto setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getMessage() {
        if(message == null && messageBytes != null) {
            try {
                message = new String(messageBytes, "UTF-8");
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return message;
    }

    public <T> T getMessage(Class<T> type) throws Exception {
        return new JSON().getMapper().readValue(getMessage(), type);
    }

    public <T> T getMessage(TypeReference typeReference) throws Exception {
        return new JSON().getMapper().readValue(getMessage(), typeReference);
    }
    
    public String toString () {
    	StringBuilder response = new StringBuilder();
    	response.append("statusCode: ").append(statusCode).append(" - Message: ").append(message);
    	if (type != null) {
    		response.append(" - Type: ").append(type.toString());
    	}
    	return response.toString();
    }

    public Integer getStatusCode() {
        return statusCode; 
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public SystemOperation getType() {
        return type;
    }

    public ResponseDto setType(SystemOperation type) {
        this.type = type;
        return this;
    }

    @JsonIgnore
    public T getData() {
        return data;
    }

    public ResponseDto setData(T data) {
        this.data = data;
        return this;
    }

    public void appendMessage(String msg) {
        if(message != null) message = message + "\n" + msg;
        else message = msg;
    }

    @JsonIgnore
    public byte[] getMessageBytes() throws Exception {
        if(messageBytes == null && message != null) return message.getBytes();
        return messageBytes;
    }

    public void setMessageBytes(byte[] messageBytes) {
        this.messageBytes = messageBytes;
    }

    @JsonIgnore
	public User getUser() {
		return user;
	}

	public ResponseDto setUser(User user) {
		this.user = user;
        return this;
	}

    @JsonIgnore
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public ResponseDto setContentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public ResponseDto setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getMetaInf() {
        return metaInf;
    }

    public ResponseDto setMetaInf(String metaInf) {
        this.metaInf = metaInf;
        return this;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public ResponseDto setBase64Data(String base64Data) {
        this.base64Data = base64Data;
        return this;
    }

    @JsonIgnore
    public ExceptionBase getException() {
        return new ExceptionBase(getMessage());
    }

    @JsonIgnore
    public ExceptionBase getException(String msg) {
        return new ExceptionBase(msg + ": " + getMessage());
    }

    public static ResponseDto OK() {
        return new ResponseDto(ResponseDto.SC_OK);
    }

    public static ResponseDto OK(byte[] messageBytes) {
        return new ResponseDto(ResponseDto.SC_OK, messageBytes);
    }

    public static ResponseDto ERROR(String message) {
        return new ResponseDto(ResponseDto.SC_ERROR, message);
    }

    public static ResponseDto ERROR_REQUEST(String message) {
        return new ResponseDto(ResponseDto.SC_ERROR_REQUEST, message);
    }

    public String getCaption() {
        return caption;
    }

    public ResponseDto setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public AppCode getCode() {
        return code;
    }

    public ResponseDto setCode(AppCode applicationCode) {
        this.code = applicationCode;
        return this;
    }

    public static ResponseDto EXCEPTION(String controller, Map actionMap, Exception exception, Throwable rootCause) {
        String action = (actionMap == null)?null:(String) actionMap.values().iterator().next();
        String metaInf = "EXCEPTION_" + controller + "Controller_" + action + "Action_" +
                rootCause.getClass().getSimpleName();
        if(exception instanceof ExceptionBase && ((ExceptionBase)exception).getMetInf() != null) {
            metaInf = metaInf + "_" +((ExceptionBase)exception).getMetInf();
            log.log(Level.SEVERE, metaInf);
        } else log.log(Level.SEVERE, metaInf, rootCause);
        ResponseDto response = new ResponseDto(ResponseDto.SC_ERROR_REQUEST, rootCause.getMessage());
        response.setReason(rootCause.getMessage());
        response.setMetaInf(metaInf);
        return response;
    }

    public static ResponseDto EXCEPTION(String controller, String action, Exception exception, Throwable rootCause) {
        Map actionMap = new HashMap<>();
        actionMap.put("", action);
        return EXCEPTION(controller, actionMap, exception, rootCause);
    }

    @JsonIgnore
    public ResponseDto getErrorResponse() throws IOException {
        ResponseDto responseDto = null;
        if(contentType == null)
            return new ResponseDto(statusCode, messageBytes, contentType);
        switch (contentType) {
            case JSON:
                responseDto = new JSON().getMapper().readValue(messageBytes, ResponseDto.class);
                break;
            case XML:
                responseDto = new XML().getMapper().readValue(messageBytes, ResponseDto.class);
                break;
            default:
                responseDto = new ResponseDto(statusCode, messageBytes, contentType);
        }
        return responseDto;
    }

}