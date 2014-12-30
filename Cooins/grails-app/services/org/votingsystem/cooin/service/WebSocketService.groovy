package org.votingsystem.cooin.service

import net.sf.json.JSONObject
import org.votingsystem.cooin.websocket.SessionVSManager
import org.votingsystem.groovy.util.SocketServiceRequest
import org.votingsystem.model.DeviceVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.throwable.ExceptionVS
import javax.websocket.CloseReason
import javax.websocket.Session
import java.nio.ByteBuffer
import static org.springframework.context.i18n.LocaleContextHolder.getLocale

class WebSocketService {

    def grailsApplication
    def messageSource

    public void onTextMessage(Session session, String msg , boolean last) {
        try {
            SocketServiceRequest request = new SocketServiceRequest(session, msg, last)
            processRequest(request)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            String message = ex.getMessage()
            if(message == null) message = messageSource.getMessage('socketRequestErrorMsg', null, locale)
            processResponse(SocketServiceRequest.getResponse(ResponseVS.SC_ERROR, message, session, msg));
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
    public void processRequest(SocketServiceRequest request) {
        switch(request.operation) {
            case TypeVS.LISTEN_TRANSACTIONS:
                TransactionVSService transactionVSService = grailsApplication.mainContext.getBean("transactionVSService")
                transactionVSService.addTransactionListener(request.messageJSON.userId)
                break;
            case TypeVS.MESSAGEVS_TO_DEVICE:
                if(SessionVSManager.getInstance().sendMessageToDevice(Long.valueOf(
                        request.messageJSON.deviceToId), request.messageJSON.toString())) {//message send OK
                    processResponse(request.getResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null))
                } else processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND, null, null));
                break;
            case TypeVS.MESSAGEVS_FROM_DEVICE:
                if(!request.sessionVS) processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND,
                        messageSource.getMessage("userNotAuthenticatedErrorMsg", null, request.locale)))
                Session callerSession = SessionVSManager.getInstance().getAuthenticatedSession(request.messageJSON.sessionId)
                if(!callerSession) callerSession = SessionVSManager.getInstance().getSession(request.messageJSON.sessionId)
                if(!callerSession) {
                    processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_NOT_FOUND, messageSource.getMessage(
                                    "messagevsSignRequestorNotFound", null, locale)))
                } else {
                    callerSession.getBasicRemote().sendText(request.messageJSON.toString())
                    processResponse(request.getResponse(ResponseVS.SC_WS_MESSAGE_SEND_OK, null))
                }
                break;
            case TypeVS.INIT_VALIDATED_SESSION:
                SignatureVSService signatureVSService = grailsApplication.mainContext.getBean("signatureVSService")
                ResponseVS responseVS = signatureVSService.processSMIMERequest(request.getSMIME(), null)
                if(ResponseVS.SC_OK == responseVS.statusCode) {
                    UserVS userVS = responseVS.messageSMIME.userVS
                    if(userVS.getDeviceVS()) {
                        //check if accessing from one device and signing from another
                        if(!userVS.getDeviceVS().getDeviceId().equals(request.deviceFromId)) userVS.setDeviceVS(null)
                    }
                    if(!userVS.getDeviceVS() && request.deviceFromId){
                        DeviceVS deviceVS = DeviceVS.findByDeviceIdAndUserVS(request.deviceFromId, userVS)
                        userVS.setDeviceVS(deviceVS)
                    }
                    if(userVS.getDeviceVS()) {
                        SessionVSManager.getInstance().putAuthenticatedDevice(request.session, userVS)
                        processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_INIT_OK, null, userVS.deviceVS.id))
                    } else processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_INIT_ERROR,
                            messageSource.getMessage("certWithoutDeviceVSInfoErrorMsg", null, locale), null))
                } else processResponse(request.getResponse(ResponseVS.SC_WS_CONNECTION_INIT_ERROR,
                        responseVS.getMessage(), null))
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

}