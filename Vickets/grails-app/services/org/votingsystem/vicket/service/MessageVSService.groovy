package org.votingsystem.vicket.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.EventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.MessageVS

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class MessageVSService {
	
	static transactional = true

	def grailsApplication
	def messageSource

	public void init() { }

    public ResponseVS send(MessageVS messageVS , Locale locale) {
        log.debug("send - messageVS: ${messageVS.id}")
        return new ResponseVS(statusCode: ResponseVS.SC_OK)

    }

    public ResponseVS sendWebSocketMessage(JSONObject messageJSON) {}


}

