package org.votingsystem.simulation

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.ExceptionVS
import javax.websocket.CloseReason
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.websocket.Session;


class WebSocketService {

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
        connectionsMap.put(session.getId(), session);
    }

    public void onClose(Session session, CloseReason closeReason) {
        log.debug("onClose - session id: ${session.getId()} - closeReason: ${closeReason}")
        connectionsMap.remove(session.getId());
    }


    public void broadcast(JSONObject messageJSON) {
        String messageStr = messageJSON.toString()
        log.debug("broadcast - message: " + messageStr)
        Enumeration<Session> sessions = connectionsMap.elements()
        while(sessions.hasMoreElements()) {
            Session session = sessions.nextElement()
            try {
                session.getBasicRemote().sendText(messageJSON.toString())
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex)
                try {
                    connectionsMap.remove(messageJSON.userId)
                    session.close();
                } catch (IOException ex1) {// Ignore
                }
            }
        }
    }

    public Map broadcastList(Map dataMap, Set<String> listeners) {
        String messageStr = "${dataMap as JSON}"
        //log.debug("--- broadcastList - message: " + messageStr)
        Map resultMap = [statusCode:ResponseVS.SC_OK]
        def errorList = []
        listeners.each {
            Session session = connectionsMap.get(it)
            if(session?.isOpen()) {
                try {
                    session.getBasicRemote().sendText(messageStr)
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex)
                    try {
                        connectionsMap.remove(messageJSON.userId)
                        session.close();
                    } catch (IOException e1) {// Ignore
                    }
                }
            } else {
                connectionsMap.remove(it)
                resultMap.statusCode = ResponseVS.SC_ERROR
                errorList.add(it)
            }
        }
        resultMap.errorList = errorList
        return resultMap
    }

    public void processResponse(JSONObject messageJSON) {
        Session session = connectionsMap.get(messageJSON.userId)
        if(session?.isOpen()) {
            try {
                session.getBasicRemote().sendText(messageJSON.toString())
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex)
                try {
                    connectionsMap.remove(messageJSON.userId)
                    session.close();
                } catch (IOException e1) {// Ignore
                }
            }
        } else {
            log.debug (" **** Lost message for session '${messageJSON.userId}' " +
                    " - message: " + messageJSON.toString())
            connectionsMap.remove(messageJSON.userId)
        }
    }

	/*
	 * references to services from grailsApplication to avoid circular references
	 */
	public void processRequest(JSONObject messageJSON) {
		String message = null;
        if(messageJSON.service) {
			Object targetService = grailsApplication.mainContext.getBean(messageJSON.service)
			if(!targetService.metaClass.respondsTo(targetService, 'processRequest').isEmpty()) {
				try {
					targetService.processRequest(messageJSON)
				} catch(Exception ex) {
					log.error(ex.getMessage() + messageJSON.toString(), ex);
				}
			} else {
                message = "Target service '${messageJSON.service}' doesn't implements 'processRequest'"
                log.error(message)
                messageJSON.status = ResponseVS.SC_ERROR
                messageJSON.message = message
                processResponse(messageJSON)
            }
		} else {
            message = "Message with target service: '${messageJSON.service}'"
            log.error(message)
            log.error("${messageJSON}")
            messageJSON.status = ResponseVS.SC_ERROR
            messageJSON.message = message
            processResponse(messageJSON)
        }
	}

}