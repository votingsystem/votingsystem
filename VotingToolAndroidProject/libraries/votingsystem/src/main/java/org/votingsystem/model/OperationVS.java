package org.votingsystem.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.util.StringUtils;

import java.text.ParseException;

/**
 *
 * @author jgzornoza
 */
public class OperationVS {

	public static final String TAG = "Operacion";

    public static final String TYPEVS_KEY   = "operationKey";
    
    private TypeVS typeVS;
    private Integer statusCode;
    private String mensaje;
    private String urlDocumento;
    private String urlTimeStampServer;
    private String receiverSignServiceURL;
    private String receiverName;
    private String emailSolicitante;
    private String signedMessageSubject;
    private JSONObject signedContent;
    private EventVS eventVS;
    private String sessionId;
    private String[] args;

    
    public OperationVS() {}
    
    public OperationVS(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public OperationVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }
    
    public OperationVS(String typeVS) {
        this.typeVS = TypeVS.valueOf(typeVS);
    }
    
    public OperationVS(int statusCode, String mensaje) {
        this.statusCode = statusCode;
        this.mensaje = mensaje;
    }
    
    public OperationVS(int statusCode, String mensaje, TypeVS typeVS) {
        this.statusCode = statusCode;
        this.mensaje = mensaje;
        this.typeVS = typeVS;
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
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * @param urlTimeStampServer the urlTimeStampServer to set
     */
    public void setUrlTimeStampServer(String urlTimeStampServer) {
        this.urlTimeStampServer = urlTimeStampServer;
    }
     
    /**
     * @return the typeVS
     */
    public TypeVS getTypeVS() {
        return typeVS;
    }

    /**
     * @param typeVS the typeVS to set
     */
    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    /**
     * @return the statusCode
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
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
     * @return the receiverSignServiceURL
     */
    public String getUrlEnvioDocumento() {
        return receiverSignServiceURL;
    }

    /**
     * @param receiverSignServiceURL the receiverSignServiceURL to set
     */
    public void setUrlEnvioDocumento(String receiverSignServiceURL) {
        this.receiverSignServiceURL = receiverSignServiceURL;
    }
    
    
    /**
     * @return the eventVS
     */
    public EventVS getEventVS() {
        return eventVS;
    }

    /**
     * @param eventVS the eventVS to set
     */
    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }
    
    
    /**
     * @return the signedContent
     */
    public JSONObject getContentFirma() {
        return signedContent;
    }

    /**
     * @param signedContent the signedContent to set
     */
    public void setContentFirma(JSONObject signedContent) {
        this.signedContent = signedContent;
    }
    
    /**
     * @return the receiverName
     */
    public String getReceiverName() {
        return receiverName;
    }
    
    public String getNormalizedReceiverName() {
        if(receiverName == null) return null;
        return StringUtils.getNormalized(receiverName);
    }

    /**
     * @param receiverName the receiverName to set
     */
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public static OperationVS parse (String operationStr) throws JSONException, ParseException {
        if(operationStr == null) return null;
        OperationVS operation = new OperationVS();
        JSONObject operationJSON = new JSONObject(operationStr);
        if (operationJSON.has("operation")) {
            operation.setTypeVS(TypeVS.valueOf(operationJSON.getString("operation")));
        }
        if (operationJSON.has("args")) {
            JSONArray arrayArgs = operationJSON.getJSONArray("args");
            String[] args = new String[arrayArgs.length()];
            for(int i = 0; i < arrayArgs.length(); i++) {
                args[i] = arrayArgs.getString(i);
            }
            operation.setArgs(args);
        }
        if (operationJSON.has("statusCode")) {
            operation.setStatusCode(operationJSON.getInt("statusCode"));
        }
        if (operationJSON.has("mensaje")) {
            operation.setMensaje(operationJSON.getString("mensaje"));
        }   
        if (operationJSON.has("receiverSignServiceURL")) {
            operation.setUrlEnvioDocumento(operationJSON.getString("receiverSignServiceURL"));
        }  
        if (operationJSON.has("urlDocumento")) {
            operation.setUrlDocumento(operationJSON.getString("urlDocumento"));
        }  
        if (operationJSON.has("urlTimeStampServer")) {
            operation.setUrlTimeStampServer(operationJSON.getString("urlTimeStampServer"));
        }  
        if (operationJSON.has("eventVS")) {
            EventVS eventVS = EventVS.parse(operationJSON.getJSONObject("eventVS"));
            operation.setEventVS(eventVS);
        }  
        if (operationJSON.has("signedContent"))
             operation.setContentFirma(operationJSON.getJSONObject("signedContent"));
        if (operationJSON.has("receiverName")) {
            operation.setReceiverName(operationJSON.getString("receiverName"));
        }
        if (operationJSON.has("signedMessageSubject")) {
            operation.setSignedMessageSubject(operationJSON.getString("signedMessageSubject"));
        }
        if (operationJSON.has("emailSolicitante")) {
            operation.setEmailSolicitante(operationJSON.getString("emailSolicitante"));
        }
        if (operationJSON.has("sessionId")) {
        	operation.setSessionId(operationJSON.getString("sessionId"));
        }
        return operation;
    }

    public JSONObject getJSON () throws JSONException {
    	JSONObject jsonObject = new JSONObject();
        if(statusCode != null) jsonObject.put("statusCode", statusCode);
        if(mensaje != null) jsonObject.put("mensaje", mensaje);
        if(typeVS != null) jsonObject.put("operation", typeVS.toString());
        if(urlDocumento != null) jsonObject.put("urlDocumento", urlDocumento);
        if(receiverSignServiceURL != null) jsonObject.put("receiverSignServiceURL", receiverSignServiceURL);
        if(signedMessageSubject != null) jsonObject.put("signedMessageSubject", receiverSignServiceURL);
        if(receiverName != null) jsonObject.put("receiverName", receiverName);
        if(urlTimeStampServer != null) jsonObject.put("urlTimeStampServer", urlTimeStampServer);
        if(args != null) jsonObject.put("args", args);
        if(sessionId != null) jsonObject.put("sessionId", sessionId);
        if(eventVS != null) jsonObject.put("eventVS", eventVS.toJSON());
        return jsonObject;
    }

    /**
     * @return the signedMessageSubject
     */
    public String getSignedMessageSubject() {
        return signedMessageSubject;
    }

    /**
     * @param signedMessageSubject the signedMessageSubject to set
     */
    public void setSignedMessageSubject(String signedMessageSubject) {
        this.signedMessageSubject = signedMessageSubject;
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


