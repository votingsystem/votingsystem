package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class AccessRequestVSService {
	
	//static scope = "request"

	def messageSource
    def signatureVSService
    def grailsApplication
	
    ResponseVS saveRequest(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		UserVS signerVS = messageSMIMEReq.getUserVS()
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		String msg
        def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
        if (!messageJSON.eventId || !messageJSON.eventURL && !messageJSON.hashAccessRequestBase64 &&
                (TypeVS.ACCESS_REQUEST != TypeVS.valueOf(messageJSON.operation))) {
            throw new ExceptionVS(messageSource.getMessage('requestWithErrorsMsg', null, locale))
        }
        EventVSElection eventVSElection = EventVSElection.get(Long.valueOf(messageJSON.eventId))
        if(!eventVSElection) {
            msg = messageSource.getMessage( 'eventVSNotFound',[messageJSON.eventId].toArray(), locale)
            log.error("saveRequest - Event Id not found - > ${messageJSON.eventId} - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    type:TypeVS.ACCESS_REQUEST_ERROR, message:msg)
        }
        if (!eventVSElection.isActive(Calendar.getInstance().getTime())) {
            if(EventVS.State.PENDING == eventVSElection.state)
                msg = messageSource.getMessage('eventVSPendingMsg', null, locale)
            else msg = messageSource.getMessage('eventVSClosedMsg', null, locale)
            log.error("$methodName - EventVS NOT ACTIVE - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.ACCESS_REQUEST_ERROR, metaInf:MetaInfMsg.getErrorMsg(methodName, "eventVSState"))
        }
        AccessRequestVS accessRequestVS = AccessRequestVS.findWhere(
                userVS:signerVS, eventVSElection:eventVSElection, state:TypeVS.OK)
        if (accessRequestVS){
            msg = "${grailsApplication.config.grails.serverURL}/messageSMIME/${accessRequestVS.messageSMIME.id}"
            log.error("saveRequest - ACCESS REQUEST ERROR - ${msg}")
            return new ResponseVS(data:accessRequestVS, type:TypeVS.ACCESS_REQUEST_ERROR, message:msg,
                    eventVS:eventVSElection, statusCode:ResponseVS.SC_ERROR_REQUEST_REPEATED,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "requestRepeated"))
        } else {
            Date signatureTime = signerVS.getTimeStampToken()?.getTimeStampInfo().getGenTime()
            if(!eventVSElection.isActive(signatureTime)) {
                msg = messageSource.getMessage("checkedDateRangeErrorMsg", [signatureTime,
                    eventVSElection.getDateBegin(), eventVSElection.getDateFinish()].toArray(), locale)
                log.error(msg)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR, data:accessRequestVS,
                        type:TypeVS.ACCESS_REQUEST_ERROR, message:msg, eventVS:eventVSElection,
                        metaInf:MetaInfMsg.getErrorMsg(methodName, "timeStampError"))
            }
            accessRequestVS = AccessRequestVS.findWhere(hashAccessRequestBase64:messageJSON.hashAccessRequestBase64)
            if (accessRequestVS) {
                msg = messageSource.getMessage('hashRepeatedError', null, locale)
                log.error("$methodName - $msg - hashRepeated: '${messageJSON.hashAccessRequestBase64}'")
                return new ResponseVS(type:TypeVS.ACCESS_REQUEST_ERROR, message:msg,
                        statusCode:ResponseVS.SC_ERROR_REQUEST, eventVS:eventVSElection,
                        metaInf:MetaInfMsg.getErrorMsg(methodName, "hashRepeated"))
            } else {
                accessRequestVS = new AccessRequestVS(userVS:signerVS, messageSMIME:messageSMIMEReq,
                        state: AccessRequestVS.State.OK, hashAccessRequestBase64:messageJSON.hashAccessRequestBase64,
                        eventVSElection:eventVSElection)
                if (!accessRequestVS.save()) { accessRequestVS.errors.each { log.error("$methodName - ERROR - ${it}")}}
                return new ResponseVS(type:TypeVS.ACCESS_REQUEST, statusCode:ResponseVS.SC_OK,
                        eventVS:eventVSElection, data:accessRequestVS)
            }
        }
    }

}