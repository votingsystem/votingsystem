package org.votingsystem.vicket.service

import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.WebSocketUtils
import org.votingsystem.vicket.model.MessageVS
import org.votingsystem.vicket.websocket.SessionVS
import org.votingsystem.vicket.websocket.SessionVSHelper
import javax.websocket.CloseReason
import javax.websocket.Session
import java.nio.ByteBuffer
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

class WebSocketService {

    def grailsApplication
    def messageSource
    def messageVSService

    public void onTextMessage(Session session, String msg , boolean last) {
        try {
            processRequest(new WebSocketRequest(session, msg, last))
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            processResponse(WebSocketUtils.getResponse(session.getId(),
                    new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage())))
        }
    }

    public void onBinaryMessage(Session session, ByteBuffer byteBuffer, boolean last) {
        log.debug("onBinaryMessage")
        //session.getBasicRemote().sendBinary(byteBuffer, last);
    }

    public void onOpen(Session session) {
        SessionVSHelper.getInstance().put(session)
    }

    public void onClose(Session session, CloseReason closeReason) {
        log.debug("onClose - session id: ${session.getId()} - closeReason: ${closeReason}")
        SessionVSHelper.getInstance().remove(session)
    }

    public void processRequest(WebSocketRequest request) {
        switch(request.operation) {
            case TypeVS.LISTEN_TRANSACTIONS:
                TransactionVSService transactionVSService = grailsApplication.mainContext.getBean("transactionVSService")
                transactionVSService.addTransactionListener(request.messageJSON.userId)
                break;
            case TypeVS.MESSAGEVS:
                MessageVSService messageVSService = grailsApplication.mainContext.getBean("messageVSService")
                messageVSService.sendWebSocketMessage(request.messageJSON)
                break;
            case TypeVS.MESSAGEVS_EDIT:
                if(!request.sessionVS) processResponse(request.getResponse(new ResponseVS(ResponseVS.SC_ERROR,
                        messageSource.getMessage("userNotAuthenticatedErrorMsg", null, request.locale))))
                grailsApplication.mainContext.getBean("messageVSService").editMessage(
                        request. messageJSON, request.sessionVS.userVS, request.locale)
                break;
            case TypeVS.MESSAGEVS_GET:
                if(!request.sessionVS) processResponse(request.getResponse(new ResponseVS(ResponseVS.SC_ERROR,
                        messageSource.getMessage("userNotAuthenticatedErrorMsg", null, request.locale))))
                request.messageJSON.userId = request.sessionVS.userVS.id
                request.messageJSON.messageVSList = messageVSService.getMessageList(request.sessionVS.userVS,
                        MessageVS.State.valueOf(request.messageJSON.state))
                request.messageJSON.status = ResponseVS.SC_OK
                processResponse(request.messageJSON)
                break;
            case TypeVS.MESSAGEVS_TO_DEVICE:
                if(SessionVSHelper.getInstance().sendMessageToDevice(Long.valueOf(
                        request.messageJSON.deviceToId), request.messageJSON.toString())) {//message send OK
                    processResponse(request.getResponse(new ResponseVS(TypeVS.MESSAGEVS_TO_DEVICE, ResponseVS.SC_OK,null)))
                } else processResponse(request.getResponse(new ResponseVS(TypeVS.MESSAGEVS_TO_DEVICE, ResponseVS.SC_ERROR,
                        messageSource.getMessage("webSocketDeviceSessionNotFoundErrorMsg",
                        [request.messageJSON.deviceToName].toArray(), locale))));
                break;
            case TypeVS.MESSAGEVS_FROM_DEVICE:
                if(!request.sessionVS) processResponse(request.getResponse(new ResponseVS(ResponseVS.SC_ERROR,
                        messageSource.getMessage("userNotAuthenticatedErrorMsg", null, request.locale))))
                Session originSession = SessionVSHelper.getInstance().getSession(request.messageJSON.sessionId)
                if(!originSession) processResponse(request.getResponse(
                        new ResponseVS(TypeVS.MESSAGEVS_SIGN_RESPONSE, ResponseVS.SC_ERROR, messageSource.getMessage(
                                "messagevsSignRequestorNotFound", null, locale))))
                originSession.getBasicRemote().sendText(request.messageJSON.toString())
                processResponse(request.getResponse(new ResponseVS(
                        TypeVS.MESSAGEVS_SIGN_RESPONSE, ResponseVS.SC_OK, null)))
                break;
            case TypeVS.INIT_VALIDATED_SESSION:
                SignatureVSService signatureVSService = grailsApplication.mainContext.getBean("signatureVSService")
                SMIMEMessage smimeMessageReq = new SMIMEMessage(new ByteArrayInputStream(
                        request.messageJSON.smimeMessage.decodeBase64()))
                request.messageJSON.remove("smimeMessage")
                ResponseVS responseVS = signatureVSService.processSMIMERequest(smimeMessageReq, null)
                if(ResponseVS.SC_OK == responseVS.statusCode) {
                    UserVS userVS = responseVS.messageSMIME.userVS
                    SessionVSHelper.getInstance().putAuthenticatedDevice(request.session, userVS)
                    request.messageJSON.userId = userVS.id
                    request.messageJSON.messageVSDataMap = [(MessageVS.State.PENDING.toString()):
                                messageVSService.getMessageList(userVS, MessageVS.State.PENDING)]
                    request.messageJSON.statusCode = ResponseVS.SC_OK
                    processResponse(request.messageJSON)
                } else {
                    request.messageJSON.statusCode = ResponseVS.SC_ERROR
                    request.messageJSON.message = responseVS.getMessage()
                    processResponse(request.messageJSON)
                }
                break;
            case TypeVS.WEB_SOCKET_BAN_SESSION:
                break;
            default: throw new ExceptionVS(messageSource.getMessage("unknownSocketOperationErrorMsg",
                    [request.messageJSON.operation].toArray(), request.locale))
        }
    }

    public void broadcast(JSONObject messageJSON) {
        SessionVSHelper.getInstance().broadcast(messageJSON)
    }

    public void broadcastToAuthenticatedUsers(JSONObject messageJSON) {
        SessionVSHelper.getInstance().broadcastToAuthenticatedUsers(messageJSON)
    }

    public ResponseVS broadcastList(Map dataMap, Set<String> listeners) {
        return SessionVSHelper.getInstance().broadcastList(dataMap, listeners)
    }

    public ResponseVS broadcastAuthenticatedList(Map dataMap, Set<String> listeners) {
        return SessionVSHelper.getInstance().broadcastAuthenticatedList(dataMap, listeners)
    }

    public void processResponse(JSONObject messageJSON) {
        log.debug("processResponse - messageJSON: ${messageJSON.message}")
        SessionVSHelper.getInstance().sendMessage(((String)messageJSON.sessionId), messageJSON.toString());
    }

    public class WebSocketRequest {
        Session session;
        JSONObject messageJSON;
        SessionVS sessionVS
        Locale locale
        TypeVS operation
        WebSocketRequest(Session session, String msg, boolean last) {
            this.session = session;
            messageJSON = (JSONObject)JSONSerializer.toJSON(msg);
            messageJSON.sessionId = session.getId()
            if(!messageJSON.locale) throw new ExceptionVS("missing message 'locale'")
            locale = Locale.forLanguageTag(messageJSON.locale)
            if(!messageJSON.operation) throw new ExceptionVS("missing message 'operation'")
            operation = TypeVS.valueOf(messageJSON.operation)
            if(TypeVS.MESSAGEVS_SIGN == operation) {
                if(!messageJSON.deviceId) throw new ExceptionVS("missing message 'deviceId'")
            }
            sessionVS = SessionVSHelper.getInstance().getAuthenticatedSession(session)
            log.debug("session id: ${session.getId()} - operation : ${messageJSON?.operation} - last: ${last}")
        }
        JSONObject getResponse(ResponseVS responseVS){
            return WebSocketUtils.getResponse(session.getId(), responseVS)
        }
    }

}