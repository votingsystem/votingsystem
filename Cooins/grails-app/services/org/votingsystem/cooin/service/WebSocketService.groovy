package org.votingsystem.cooin.service

import net.sf.json.JSONNull
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.coyote.http11.upgrade.NioServletOutputStream
import org.apache.tomcat.websocket.WsRemoteEndpointAsync
import org.apache.tomcat.websocket.WsSession
import org.apache.tomcat.websocket.server.WsRemoteEndpointImplServer
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.cooin.model.MessageVS
import org.votingsystem.cooin.websocket.SessionVS
import org.votingsystem.cooin.websocket.SessionVSManager
import javax.websocket.CloseReason
import javax.websocket.Session
import java.nio.ByteBuffer
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

class WebSocketService {

    def grailsApplication
    def messageSource


    public void onTextMessage(Session session, String msg , boolean last) {
        WebSocketRequest request = null
        try {
            request = new WebSocketRequest(session, msg, last)
            processRequest(request)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            String message = ex.getMessage()
            if(message == null) message = messageSource.getMessage('socketRequestErrorMsg', null, locale)
            JSONObject responseJSON
            if(request) responseJSON = request.getResponse(ResponseVS.SC_ERROR, message)
            else responseJSON = getResponse(session.getId(),ResponseVS.SC_ERROR , null, ex.getMessage())
            processResponse(responseJSON);
        }
    }

    public void onBinaryMessage(Session session, ByteBuffer byteBuffer, boolean last) {
        log.debug("onBinaryMessage")
        //session.getBasicRemote().sendBinary(byteBuffer, last);
    }

    public void onOpen(Session session) {
        SessionVSManager.getInstance().put(session)
    }

    public void onClose(Session session, CloseReason closeReason) {
        log.debug("onClose - session id: ${session.getId()} - closeReason: ${closeReason}")
        SessionVSManager.getInstance().remove(session)
    }

    //TODO QUEUE
    public void processRequest(WebSocketRequest request) {
        switch(request.operation) {
            case TypeVS.LISTEN_TRANSACTIONS:
                TransactionVSService transactionVSService = grailsApplication.mainContext.getBean("transactionVSService")
                transactionVSService.addTransactionListener(request.messageJSON.userId)
                break;
            case TypeVS.MESSAGEVS_TO_DEVICE:
                if(SessionVSManager.getInstance().sendMessageToDevice(Long.valueOf(
                        request.messageJSON.deviceToId), request.messageJSON.toString())) {//message send OK
                    processResponse(request.getResponse(ResponseVS.SC_OK, null))
                } else processResponse(request.getResponse(ResponseVS.SC_ERROR,
                        messageSource.getMessage("webSocketDeviceSessionNotFoundErrorMsg",
                        [request.messageJSON.deviceToName].toArray(), locale)));
                break;
            case TypeVS.MESSAGEVS_FROM_DEVICE:
                if(!request.sessionVS) processResponse(request.getResponse(ResponseVS.SC_ERROR,
                        messageSource.getMessage("userNotAuthenticatedErrorMsg", null, request.locale)))
                Session originSession = SessionVSManager.getInstance().getAuthenticatedSession(request.messageJSON.sessionId)
                if(!originSession) originSession = SessionVSManager.getInstance().getSession(request.messageJSON.sessionId)
                if(!originSession) {
                    processResponse(request.getResponse(ResponseVS.SC_ERROR, messageSource.getMessage(
                                    "messagevsSignRequestorNotFound", null, locale)))
                } else {
                    originSession.getBasicRemote().sendText(request.messageJSON.toString())
                    processResponse(request.getResponse(ResponseVS.SC_OK, null))
                }
                break;
            case TypeVS.INIT_VALIDATED_SESSION:
                SignatureVSService signatureVSService = grailsApplication.mainContext.getBean("signatureVSService")
                SMIMEMessage smimeMessageReq = new SMIMEMessage(new ByteArrayInputStream(
                        request.messageJSON.smimeMessage.decodeBase64()))
                request.messageJSON.remove("smimeMessage")
                ResponseVS responseVS = signatureVSService.processSMIMERequest(smimeMessageReq, null)
                if(ResponseVS.SC_OK == responseVS.statusCode) {
                    UserVS userVS = responseVS.messageSMIME.userVS
                    SessionVSManager.getInstance().putAuthenticatedDevice(request.session, userVS)
                    processResponse(request.getResponse(ResponseVS.SC_OK, null, userVS.id))
                } else {
                    processResponse(request.getResponse(ResponseVS.SC_ERROR, responseVS.getMessage(), null))
                }
                break;
            case TypeVS.WEB_SOCKET_BAN_SESSION:
                //talks
                break;
            default: throw new ExceptionVS(messageSource.getMessage("unknownSocketOperationErrorMsg",
                    [request.messageJSON.operation].toArray(), request.locale))
        }
    }

    public void broadcast(JSONObject messageJSON) {
        SessionVSManager.getInstance().broadcast(messageJSON)
    }

    public void broadcastToAuthenticatedUsers(JSONObject messageJSON) {
        SessionVSManager.getInstance().broadcastToAuthenticatedUsers(messageJSON)
    }

    public ResponseVS broadcastList(Map dataMap, Set<String> listeners) {
        return SessionVSManager.getInstance().broadcastList(dataMap, listeners)
    }

    public ResponseVS broadcastAuthenticatedList(Map dataMap, Set<String> listeners) {
        return SessionVSManager.getInstance().broadcastAuthenticatedList(dataMap, listeners)
    }

    public void processResponse(JSONObject messageJSON) {
        log.debug("processResponse - messageJSON: ${messageJSON.message}")
        SessionVSManager.getInstance().sendMessage(((String)messageJSON.sessionId), messageJSON.toString());
    }

    public JSONObject getResponse(String sessionId, Integer statusCode, TypeVS typeVS, String message){
        JSONObject result = new JSONObject();
        if(typeVS != null) result.put("operation", typeVS.toString());
        else result.put("operation", TypeVS.WEB_SOCKET_MESSAGE.toString());
        result.put("sessionId", sessionId);
        result.put("statusCode", statusCode);
        result.put("message", message);
        return result;
    }

    public class WebSocketRequest {
        Session session;
        JSONObject messageJSON;
        SessionVS sessionVS
        Locale locale
        TypeVS operation
        InetSocketAddress remoteAddress
        public WebSocketRequest(Session session, String msg, boolean last) {
            this.remoteAddress = ((InetSocketAddress)((NioServletOutputStream)((WsRemoteEndpointImplServer)((WsRemoteEndpointAsync)
                    ((WsSession)session).remoteEndpointAsync).base).sos).socketWrapper.socket.sc.remoteAddress);
            this.session = session;
            messageJSON = (JSONObject)JSONSerializer.toJSON(msg);
            if(!messageJSON.sessionId) messageJSON.sessionId = session.getId()
            if(!messageJSON.locale) throw new ExceptionVS("missing message 'locale'")
            locale = Locale.forLanguageTag(messageJSON.locale)
            if(!messageJSON.operation || JSONNull.getInstance().equals(messageJSON.operation))
                throw new ExceptionVS("missing message 'operation'")
            operation = TypeVS.valueOf(messageJSON.operation)
            if(TypeVS.MESSAGEVS_SIGN == operation) {
                if(!messageJSON.deviceId) throw new ExceptionVS("missing message 'deviceId'")
            }
            sessionVS = SessionVSManager.getInstance().getAuthenticatedSession(session)
            log.debug("session id: ${session.getId()} - operation : ${messageJSON?.operation} - " +
                    "remoteIp: ${remoteAddress.address} - last: ${last}")
        }
        JSONObject getResponse(Integer statusCode, String message){
            return getResponse(session.getId(), statusCode, operation, message);
        }

        JSONObject getResponse(Integer statusCode, String message, Long userId){
            messageJSON.statusCode = statusCode;
            messageJSON.userId = userId;
            messageJSON.message = message;
            return messageJSON;
        }

    }

}