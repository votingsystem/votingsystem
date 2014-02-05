package org.votingsystem.ticket.service

import grails.converters.JSON
import org.apache.catalina.websocket.MessageInbound
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.ExceptionVS
import org.votingsystem.websocket.SocketServletVS.SocketMessageInbound

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.util.concurrent.ConcurrentHashMap

class WebSocketService {

    public enum SocketOperation {LISTEN_TRANSACTIONS}

	private static final ConcurrentHashMap<String, MessageInbound> connectionsMap = new ConcurrentHashMap<String, MessageInbound>();

	def grailsApplication
    def messageSource

	public void init() throws Exception {
		log.debug("init")
	}

	public void onOpen(SocketMessageInbound messageInbound) {
		connectionsMap.put(messageInbound.getBrowserId(), messageInbound);
	}

	public void onClose(SocketMessageInbound messageInbound, int status) {
		log.debug("onClose - status: " + status + " - BrowserId: " + messageInbound.getBrowserId())
		connectionsMap.remove(messageInbound.getBrowserId());
	}

	public void onBinaryMessage(SocketMessageInbound messageInbound, ByteBuffer message) {
		log.debug("onBinaryMessage")
	}

	public void onTextMessage(SocketMessageInbound messageInbound, CharBuffer message) {
		String messageStr = new String(message.array());
		def messageJSON = JSON.parse(messageStr)
		messageJSON.userId = messageInbound.getBrowserId()
		processRequest(messageJSON)
	}
	
	public void broadcast(JSONObject messageJSON) {
		String messageStr = messageJSON.toString()
		log.debug("broadcast - message: " + messageStr)
		Enumeration<MessageInbound> connections = connectionsMap.elements()
		while(connections.hasMoreElements()) {
			MessageInbound connection = connections.nextElement()
			CharBuffer buffer = CharBuffer.wrap(messageStr);
			try {
				connection.getWsOutbound().writeTextMessage(buffer);
			} catch (IOException ex) {
				log.error(ex.getMessage(), ex);
			}
		}
	}
	
	public Map broadcastList(Map dataMap, Set<String> listeners) {
		String messageStr = "${dataMap as JSON}"
		//log.debug("--- broadcastList - message: " + messageStr)
		Map resultMap = [statusCode:ResponseVS.SC_OK]
		def errorList = []
		listeners.each {
            CharBuffer messageBuffer = CharBuffer.wrap(messageStr);
            MessageInbound messageInbound = connectionsMap.get(it)
			if(messageInbound) {
				try {
					messageInbound.getWsOutbound().writeTextMessage(messageBuffer);
				} catch (IOException ex) {
					log.error(ex.getMessage(), ex);
				}
			} else {
				resultMap.statusCode = ResponseVS.SC_ERROR
				errorList.add(it)
			}
		}
		resultMap.errorList = errorList
		return resultMap
	}
	
	/*
	 * references to services from grailsApplication to avoid circular references
	 */
	public void processRequest(JSONObject messageJSON) {
		String message = null;
        Locale locale = null
        try {
            locale = Locale.forLanguageTag(messageJSON.locale)
            SocketOperation socketOperation = SocketOperation.valueOf(messageJSON.operation)
            switch(socketOperation) {
                case SocketOperation.LISTEN_TRANSACTIONS:
                    TransactionVSService transactionVSService = grailsApplication.mainContext.getBean("transactionVSService")
                    transactionVSService.addTransactionListener(messageJSON.userId)
                    break;
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
		MessageInbound connection = connectionsMap.get(messageJSON.userId)
		if(connection) {
			CharBuffer buffer = CharBuffer.wrap(messageJSON.toString());
			try {
				connection.getWsOutbound().writeTextMessage(buffer);
			} catch (IOException ex) {
				log.error(ex.getMessage(), ex);
			}
		} else log.debug (" **** Lost message for user '${messageJSON.userId}' " + 
			" - message: " + messageJSON.toString())
	}


}