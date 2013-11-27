package org.votingsystem.simulation

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean
import grails.transaction.Transactional
import org.votingsystem.websocket.SocketServletVS.SocketMessageInbound;
import org.votingsystem.model.ResponseVS;
import org.apache.catalina.websocket.MessageInbound;
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.votingsystem.simulation.model.*

import grails.converters.JSON

import java.util.concurrent.ConcurrentHashMap;

class WebSocketService {
	

	private static final ConcurrentHashMap<String, MessageInbound> connectionsMap = new ConcurrentHashMap<String, MessageInbound>();

	def grailsApplication
	

	public void initService() throws Exception {
		log.debug("--- initService")

	}

	public void onOpen(SocketMessageInbound messageInbound) {
		connectionsMap.put(messageInbound.getBrowserId(), messageInbound);
	}

	public void onClose(SocketMessageInbound messageInbound, int status) {
		log.debug("onClose - status: " + status + " - BrowserId: " + messageInbound.getBrowserId())
		connectionsMap.remove(messageInbound.getBrowserId());
	}

	public void onBinaryMessage(SocketMessageInbound messageInbound,
			ByteBuffer message) {
		log.debug("onBinaryMessage")
	}

	public void onTextMessage(SocketMessageInbound messageInbound, CharBuffer message) {
		String messageStr = new String(message.array());
		def mensajeJSON = JSON.parse(messageStr)
		mensajeJSON.userId = messageInbound.getBrowserId()
		processRequest(mensajeJSON)
	}
	
	public void broadcast(JSONObject messageJSON) {
		String messageStr = messageJSON.toString()
		log.debug("--- broadcast - message: " + messageStr)
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
		CharBuffer messageBuffer = CharBuffer.wrap(messageStr);
		listeners.each {
			//log.debug("--- broadcastList - listener: " + it)
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
		if(messageJSON.service) {
			Object targetService = grailsApplication.mainContext.getBean(messageJSON.service)
			if(!targetService.metaClass.respondsTo(targetService, 'processRequest').isEmpty()) {
				try {
					targetService.processRequest(messageJSON)
				} catch(Exception ex) {
					log.error(ex.getMessage() + messageJSON.toString(), ex);
				}
			} else log.error("Target service '${messageJSON.service}' doesn't implements 'processRequest'")
		} else log.error("Message without target service '${messageJSON.service}'")
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