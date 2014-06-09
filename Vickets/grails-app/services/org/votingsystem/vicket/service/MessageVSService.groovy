package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.votingsystem.model.EventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
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

    @Transactional
    public ResponseVS editMessage(JSONObject messageJSON, UserVS userVS, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        //[messageId:6, state:CONSUMED, locale:es, operation:MESSAGEVS_EDIT, sessionId:1] - userVS: 4
        if(TypeVS.MESSAGEVS_EDIT == TypeVS.valueOf(messageJSON.operation)) {
            MessageVS messageVS = MessageVS.findWhere(id:Integer.valueOf(messageJSON.messageId).longValue(), toUserVS:userVS)
            if(messageVS) {
                messageVS.state = MessageVS.State.valueOf(messageJSON.state)
                messageVS.save()
                log.debug("${methodName} - messageVS: ${messageVS.id} - to state '${messageJSON.state}'")
                return new ResponseVS(ResponseVS.SC_OK)
            } else return new ResponseVS(ResponseVS.SC_NOT_FOUND)
        } else return new ResponseVS(ResponseVS.SC_ERROR_REQUEST)

    }

    @Transactional
    public List getMessageList(UserVS toUserVS,  MessageVS.State state) {
        def pendingMessageVSList = MessageVS.findAllWhere(toUserVS: toUserVS, state:state)
        List messageVSList = []
        pendingMessageVSList.each { messageVS ->
            JSONObject messageVSJSON = JSONSerializer.toJSON(new String(messageVS.content, "UTF-8"));
            Map fromUser = [id:messageVS.fromUserVS.id, name:messageVS.fromUserVS.getDefaultName()]
            messageVSList.add([id:messageVS.id, fromUser: fromUser, dateCreated:DateUtils.getStringFromDate(messageVS.dateCreated),
                               encryptedDataList:messageVSJSON.encryptedDataList])
        }
        return messageVSList
    }

}

