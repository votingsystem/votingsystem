package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.AccessRequestVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteProcessEvent
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class AccessRequestVSService {
	
	//static scope = "request"

    static transactional = true
	
	def messageSource
    def signatureVSService
    def grailsApplication
	def timeStampVSService
	
	//{"operation":"ACCESS_REQUEST","hashAccessRequestBase64":"...",
	// "eventId":"..","eventURL":"...","UUID":"..."}
	private ResponseVS checkAccessRequestJSONData(JSONObject accessDataJSON, Locale locale) {
		int status = ResponseVS.SC_ERROR_REQUEST
		TypeVS typeRespuesta = TypeVS.ACCESS_REQUEST_ERROR
		org.bouncycastle.tsp.TimeStampToken tms;
		String msg
		try {
			TypeVS operationType = TypeVS.valueOf(accessDataJSON.operation)
			if (accessDataJSON.eventId && accessDataJSON.eventURL &&
				accessDataJSON.hashAccessRequestBase64 &&
				(TypeVS.ACCESS_REQUEST == operationType)) {
				status = ResponseVS.SC_OK
			} else msg = messageSource.getMessage('accessRequestWithErrorsMsg', null, locale)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('accessRequestWithErrorsMsg', null, locale)
		}
		if(ResponseVS.SC_OK == status) typeRespuesta = TypeVS.ACCESS_REQUEST
		else log.error("checkAccessRequestJSONData - msg: ${msg} - data:${accessDataJSON.toString()}")
		return new ResponseVS(statusCode:status, message:msg, type:typeRespuesta)
	}
	
    ResponseVS saveRequest(MessageSMIME messageSMIMEReq, Locale locale) {
		UserVS signerVS = messageSMIMEReq.getUserVS()
		log.debug("saveRequest - signerVS: ${signerVS.nif}")
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		String msg
        try {
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			ResponseVS responseVS = checkAccessRequestJSONData(messageJSON, locale)
			if(ResponseVS.SC_OK !=  responseVS.statusCode) return responseVS
			def hashAccessRequestBase64
			def typeRespuesta
			def accessRequestVS
			def eventVSElection
			EventVSElection.withTransaction {
				eventVSElection = EventVSElection.findById(Long.valueOf(messageJSON.eventId))
			}
			if (eventVSElection) {
				if (!eventVSElection.isActive(DateUtils.getTodayDate())) {
					msg = messageSource.getMessage('eventVS.messageCerrado', null, locale)
					log.error("saveRequest - EVENT CLOSED - ${msg}")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
						type:TypeVS.ACCESS_REQUEST_ERROR, message:msg)
				}
				AccessRequestVS.withTransaction {
					accessRequestVS = AccessRequestVS.findWhere(userVS:signerVS, eventVSElection:eventVSElection, state:TypeVS.OK)
				}
				if (accessRequestVS){//Ha votado el userVS?
						msg = "${grailsApplication.config.grails.serverURL}/messageSMIME/${accessRequestVS.messageSMIME.id}"
						log.error("saveRequest - ACCESS REQUEST ERROR - ${msg}")
						return new ResponseVS(accessRequestVS:accessRequestVS,
							type:TypeVS.ACCESS_REQUEST_ERROR, message:msg, eventVS:eventVSElection,
							statusCode:ResponseVS.SC_ERROR_VOTE_REPEATED)
				} else {
					//TimeStamp comes cert validated from filters. Check date
                    Date signatureTime = signerVS.getTimeStampToken()?.getTimeStampInfo().getGenTime()
                    if(!eventVSElection.isActive(signatureTime)) {
                        msg = messageSource.getMessage("checkedDateRangeErrorMsg", [signatureTime,
                                eventVSElection.getDateBegin(), eventVSElection.getDateFinish()].toArray(), locale)
                        log.error(msg)
                        return new ResponseVS(statusCode:ResponseVS.SC_ERROR, data:accessRequestVS,
                                type:TypeVS.ACCESS_REQUEST_ERROR, message:msg, eventVS:eventVSElection)
                    }
					//es el hash unique?
					hashAccessRequestBase64 = messageJSON.hashAccessRequestBase64
					boolean hashSolicitudAccesoRepetido = (AccessRequestVS.findWhere(
							hashAccessRequestBase64:hashAccessRequestBase64) != null)
					if (hashSolicitudAccesoRepetido) {
						msg = messageSource.getMessage('hashRepeatedError', null, locale)
						log.error("saveRequest -ERROR ACCESS REQUEST HAS REPEATED -> ${hashAccessRequestBase64} - ${msg}")
						return new ResponseVS(type:TypeVS.ACCESS_REQUEST_ERROR, message:msg,
								statusCode:ResponseVS.SC_ERROR_REQUEST, eventVS:eventVSElection)
					} else {//Todo OK
					
					VoteProcessEvent votingEvent = null
					if(UserVS.Type.REPRESENTATIVE == signerVS.type) {
						votingEvent = VoteProcessEvent.ACCESS_REQUEST_REPRESENTATIVE.setData(
							signerVS, eventVSElection)
					} else if(signerVS.representative) {
						votingEvent = VoteProcessEvent.ACCESS_REQUEST_USER_WITH_REPRESENTATIVE.setData(
							signerVS, eventVSElection)
					} else {
						votingEvent = VoteProcessEvent.ACCESS_REQUEST.setData(
							signerVS, eventVSElection)
					}
					
					accessRequestVS = new AccessRequestVS(userVS:signerVS,
						messageSMIME:messageSMIMEReq,
						state: AccessRequestVS.State.OK,
						hashAccessRequestBase64:hashAccessRequestBase64,
						eventVSElection:eventVSElection)
					AccessRequestVS.withTransaction {
						if (!accessRequestVS.save()) {
							accessRequestVS.errors.each { log.error("- saveRequest - ERROR - ${it}")}
						}
					}
					return new ResponseVS(type:TypeVS.ACCESS_REQUEST, statusCode:ResponseVS.SC_OK,
                            eventVS:eventVSElection, data:accessRequestVS)
					}
				}
			} else {
				msg = messageSource.getMessage( 'eventVSNotFound',[messageJSON.eventId].toArray(), locale)
				log.error("saveRequest - Event Id not found - > ${messageJSON.eventId} - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
						type:TypeVS.ACCESS_REQUEST_ERROR, message:msg)
			}
		}catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.ACCESS_REQUEST_ERROR,
					message:messageSource.getMessage('accessRequestWithErrorsMsg', null, locale))
		}
    }
	
	def rechazarSolicitud(AccessRequestVS accessRequestVS, String detalles) {
		log.debug("rechazarSolicitud '${accessRequestVS.id}'")
		accessRequestVS.detalles = detalles
		accessRequestVS = accessRequestVS.merge()
		accessRequestVS.state = AccessRequestVS.State.CANCELLED
		accessRequestVS.save()
	}

}