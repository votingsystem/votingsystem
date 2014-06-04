package org.votingsystem.vicket.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.ExceptionVS
import org.votingsystem.vicket.websocket.SessionVSHelper

import javax.websocket.CloseReason
import javax.websocket.Session
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class WebSocketService {

    public enum SocketOperation {LISTEN_TRANSACTIONS, MESSAGEVS}

    private static final ConcurrentHashMap<String, Session> connectionsMap = new ConcurrentHashMap<String, Session>();
    def grailsApplication
    def messageSource

    public void onTextMessage(Session session, String msg , boolean last) {
        log.debug("onTextMessage - session id: ${session.getId()} - last: ${last}")
        def messageJSON = JSON.parse(msg)
        messageJSON.userId = session.getId()
        processRequest(messageJSON)
    }

    public void onBinaryMessage(Session session, ByteBuffer bb, boolean last) {
        log.debug("onBinaryMessage")
        //session.getBasicRemote().sendBinary(bb, last);
    }

    public void onOpen(Session session) {
        log.debug("onOpen - session id: ${session.getId()}")
        SessionVSHelper.getInstance().put(session)
    }

    public void onClose(Session session, CloseReason closeReason) {
        log.debug("onClose - session id: ${session.getId()} - closeReason: ${closeReason}")
        SessionVSHelper.getInstance().remove(session)
    }


    public void broadcast(JSONObject messageJSON) {
        SessionVSHelper.getInstance().broadcast(messageJSON)
    }

    public ResponseVS broadcastList(Map dataMap, Set<String> listeners) {
        return SessionVSHelper.getInstance().broadcastList(dataMap, listeners)
    }

    /*
    * references to services from grailsApplication to avoid circular references
    */
    public void processRequest(JSONObject messageJSON) {
        String message = null;
        Locale locale = null
        log.debug("messageJSON: ${messageJSON}")
        try {
            if(!messageJSON.locale) {
                messageJSON.status = ResponseVS.SC_ERROR
                messageJSON.message = "missing message locale"
                processResponse(messageJSON)
                return;
            }
            locale = Locale.forLanguageTag(messageJSON.locale)
            SocketOperation socketOperation
            try { socketOperation = SocketOperation.valueOf(messageJSON.operation) }
            catch(Exception ex) {
                messageJSON.status = ResponseVS.SC_ERROR
                messageJSON.message = "Operaion '${messageJSON.operation}' is not a valid operation."
                processResponse(messageJSON)
                return;
            }
            switch(socketOperation) {
                case SocketOperation.LISTEN_TRANSACTIONS:
                    TransactionVSService transactionVSService = grailsApplication.mainContext.getBean("transactionVSService")
                    transactionVSService.addTransactionListener(messageJSON.userId)
                    break;
                case SocketOperation.MESSAGEVS:
                    MessageVSService messageVSService = grailsApplication.mainContext.getBean("messageVSService")
                    messageVSService.sendWebSocketMessage(messageJSON)
                default: throw new ExceptionVS(messageSource.getMessage("unknownSocketOperationErrorMsg",
                        [messageJSON.operation].toArray(), locale))
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            messageJSON.status = ResponseVS.SC_ERROR
            messageJSON.message = ex.getMessage()
            processResponse(messageJSON)
        }
    }

    public void processResponse(JSONObject messageJSON) {
        SessionVSHelper.getInstance().sendMessage(messageJSON.userId, messageJSON.toString());

    }

}