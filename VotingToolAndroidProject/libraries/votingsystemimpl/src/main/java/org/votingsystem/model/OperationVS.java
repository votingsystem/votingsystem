package org.votingsystem.model;

import android.util.Log;
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

    public static final String OPERATION_KEY   = "operationKey";
    
    private TypeVS typeVS;
    private Integer statusCode;
    private String mensaje;
    private String urlDocumento;
    private String urlTimeStampServer;
    private String receiverSignServiceURL;
    private String receiverName;
    private String emailSolicitante;
    private String signedMessageSubject;
    private Boolean isResponseWithReceipt = false;
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
        return StringUtils.getCadenaNormalizada(receiverName);
    }

    /**
     * @param receiverName the receiverName to set
     */
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public static OperationVS parse (String operacionStr) throws JSONException, ParseException {
		Log.d(TAG + ".parse(...) ", "operacionStr: " + operacionStr);
        if(operacionStr == null) return null;
        OperationVS operacion = new OperationVS();
        JSONObject operacionJSON = new JSONObject(operacionStr);
        if (operacionJSON.has("operacion")) {
            operacion.setTypeVS(TypeVS.valueOf(operacionJSON.getString("operacion")));
        }
        if (operacionJSON.has("args")) {
            JSONArray arrayArgs = operacionJSON.getJSONArray("args");
            String[] args = new String[arrayArgs.length()];
            for(int i = 0; i < arrayArgs.length(); i++) {
                args[i] = arrayArgs.getString(i);
            }
            operacion.setArgs(args);
        }
        if (operacionJSON.has("statusCode")) {
            operacion.setStatusCode(operacionJSON.getInt("statusCode"));
        }
        if (operacionJSON.has("mensaje")) {
            operacion.setMensaje(operacionJSON.getString("mensaje"));
        }   
        if (operacionJSON.has("receiverSignServiceURL")) {
            operacion.setUrlEnvioDocumento(operacionJSON.getString("receiverSignServiceURL"));
        }  
        if (operacionJSON.has("urlDocumento")) {
            operacion.setUrlDocumento(operacionJSON.getString("urlDocumento"));
        }  
        if (operacionJSON.has("urlTimeStampServer")) {
            operacion.setUrlTimeStampServer(operacionJSON.getString("urlTimeStampServer"));
        }  
        if (operacionJSON.has("eventVS")) {
            EventVS eventVS = EventVS.parse(operacionJSON.getJSONObject("eventVS"));
            operacion.setEventVS(eventVS);
        }  
        if (operacionJSON.has("signedContent"))
             operacion.setContentFirma(operacionJSON.getJSONObject("signedContent"));
        if (operacionJSON.has("receiverName")) {
            operacion.setReceiverName(operacionJSON.getString("receiverName"));
        }
        if (operacionJSON.has("signedMessageSubject")) {
            operacion.setSignedMessageSubject(operacionJSON.getString("signedMessageSubject"));
        }
        if (operacionJSON.has("isResponseWithReceipt")) {
            operacion.setRespuestaConRecibo(operacionJSON.getBoolean("isResponseWithReceipt"));
        }
        if (operacionJSON.has("emailSolicitante")) {
            operacion.setEmailSolicitante(operacionJSON.getString("emailSolicitante"));
        }
        if (operacionJSON.has("sessionId")) {
        	operacion.setSessionId(operacionJSON.getString("sessionId"));
        }
        return operacion;
    }

    public JSONObject getJSON () throws JSONException {
    	JSONObject jsonObject = new JSONObject();
        if(statusCode != null) jsonObject.put("statusCode", statusCode);
        if(mensaje != null) jsonObject.put("mensaje", mensaje);
        if(typeVS != null) jsonObject.put("operacion", typeVS.toString());
        if(urlDocumento != null) jsonObject.put("urlDocumento", urlDocumento);
        if(receiverSignServiceURL != null) jsonObject.put("receiverSignServiceURL", receiverSignServiceURL);
        if(signedMessageSubject != null) jsonObject.put("signedMessageSubject", receiverSignServiceURL);
        if(receiverName != null) jsonObject.put("receiverName", receiverName);
        if(isResponseWithReceipt != null) jsonObject.put("isResponseWithReceipt", isResponseWithReceipt);
        if(urlTimeStampServer != null) jsonObject.put("urlTimeStampServer", urlTimeStampServer);
        if(args != null) jsonObject.put("args", args);
        if(sessionId != null) jsonObject.put("sessionId", sessionId);
        
        if(eventVS != null) jsonObject.put("eventVS", eventVS.toJSON());
        return jsonObject;
    }
    
    public String getJSONStr () throws JSONException {
        JSONObject operacionJSON = getJSON();
        if(operacionJSON == null) return null;
        else return operacionJSON.toString();
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
     * @return the isResponseWithReceipt
     */
    public boolean isRespuestaConRecibo() {
        return isResponseWithReceipt;
    }

    /**
     * @param isResponseWithReceipt the isResponseWithReceipt to set
     */
    public void setRespuestaConRecibo(boolean isResponseWithReceipt) {
        this.isResponseWithReceipt = isResponseWithReceipt;
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


