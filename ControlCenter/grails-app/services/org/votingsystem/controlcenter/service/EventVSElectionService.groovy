package org.votingsystem.controlcenter.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.AccessControlVS
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.FieldEventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TagVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.StringUtils

import java.security.cert.X509Certificate
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* */
class EventVSElectionService {
	
    static transactional = false
	
	//static scope = "session"
    LinkGenerator grailsLinkGenerator
	def messageSource
	def subscriptionVSService
    def grailsApplication
	def tagVSService
	def signatureVSService
	
	List<String> administradoresSistema
	
	ResponseVS saveEvent(MessageSMIME messageSMIMEReq, Locale locale) {
        log.debug("- saveEvent")
		ResponseVS responseVS
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		String msg
		try {
			AccessControlVS accessControl = subscriptionVSService.checkAccessControl(
                    smimeMessageReq.getHeader("serverURL")[0])
			if(!accessControl) {
				msg = message(code:'accessControlNotFound', args:[serverURL])
				log.debug("- saveEvent - ${msg}")
				return new ResponseVS(type:TypeVS.VOTING_EVENT_ERROR, message:msg,
                        statusCode:ResponseVS.SC_ERROR_REQUEST)
			}
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			if(!messageJSON.certCAVotacion || !messageJSON.userVS || !messageJSON.id ||
                    !messageJSON.fieldsEventVS || !messageJSON.URL || !messageJSON.controlCenter) {
				msg = messageSource.getMessage('documentParamsErrorMsg', null, locale)
				log.error("saveEvent - ERROR - ${msg} - document: ${messageJSON as JSON}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg,type:TypeVS.VOTING_EVENT_ERROR)
			}
			String serverURL = grailsApplication.config.grails.serverURL
            String requestServerURL = StringUtils.checkURL(messageJSON.controlCenter.serverURL)
			if (!serverURL.equals(requestServerURL)) {
				msg = messageSource.getMessage('localServerURLErrorMsg',[serverURL, requestServerURL].toArray(), locale)
				log.error("saveEvent - ERROR - ${msg} - document: ${messageJSON as JSON}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg,type:TypeVS.VOTING_EVENT_ERROR)
			} else messageJSON.controlCenter.serverURL = requestServerURL
			X509Certificate certCAVotacion = CertUtil.fromPEMToX509Cert(messageJSON.certCAVotacion?.bytes)
			byte[] certChain = messageJSON.certChain?.getBytes()
			X509Certificate userCert = CertUtil.fromPEMToX509Cert(messageJSON.userVS?.bytes)
			
			UserVS user = UserVS.getUsuario(userCert);
			//Publish request comes with Access Control cert
			responseVS = subscriptionVSService.checkUser(user, locale)
			if(ResponseVS.SC_OK != responseVS.statusCode) {
				log.error("saveEvent - USER CHECK ERROR - ${responseVS.message}")
				return  new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:responseVS.message, type:TypeVS.VOTING_EVENT_ERROR)
			} 
			user = responseVS.userVS
			def eventVS = new EventVSElection(accessControlEventVSId:messageJSON.id,
				subject:messageJSON.subject, certChainAccessControl:certChain,
				content:messageJSON.content, url:messageJSON.URL, accessControlVS:accessControl,
				userVS:user, dateBegin:DateUtils.getDateFromString(messageJSON.dateBegin),
				dateFinish:DateUtils.getDateFromString(messageJSON.dateFinish))
			responseVS = setEventDatesState(eventVS, locale)
			if(ResponseVS.SC_OK != responseVS.statusCode) {
				return  new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:responseVS.message, type:TypeVS.VOTING_EVENT_ERROR)
			}
			EventVS.withTransaction { eventVS.save() }
			CertificateVS eventVSRootCertificate = new CertificateVS(actorVS:accessControl,
                state:CertificateVS.State.OK, type:CertificateVS.Type.VOTEVS_ROOT, eventVSElection:eventVS,
				content:certCAVotacion.getEncoded(), serialNumber:certCAVotacion.getSerialNumber().longValue(),
				validFrom:certCAVotacion?.getNotBefore(), validTo:certCAVotacion?.getNotAfter())
			CertificateVS.withTransaction {eventVSRootCertificate.save()}
			saveFieldsEventVS(eventVS, messageJSON)
			if (messageJSON.tags) {
				Set<TagVS> tags = tagVSService.save(messageJSON.tags)
				eventVS.setTagVSSet(tags)
			}
			eventVS.save()
			log.debug("saveEvent - SAVED event - '${eventVS.id}'")
			return new ResponseVS(statusCode:ResponseVS.SC_OK,  eventVS:eventVS, type:TypeVS.VOTING_EVENT)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: messageSource.getMessage(
                    'saveDocumentoErrorMsg', null, locale), type:TypeVS.VOTING_EVENT_ERROR)
		}
	}
	
    Set<FieldEventVS> saveFieldsEventVS(EventVS eventVS, JSONObject json) {
        log.debug("saveFieldsEventVS - ")
        def fieldsEventVSSet = json.fieldsEventVS.collect { opcionItem ->
                def opcion = new FieldEventVS(eventVS:eventVS, content:opcionItem.content,
                        accessControlFieldEventId:opcionItem.id)
                return opcion.save();
        }
        return fieldsEventVSSet
    }

	ResponseVS setEventDatesState (EventVS eventVS, Locale locale) {
		if(eventVS.dateBegin.after(eventVS.dateFinish)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
                    'dateRangeErrorMsg', [eventVS.dateBegin, eventVS.dateFinish].toArray(), locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(eventVS.dateFinish)) eventVS.setState(EventVS.State.TERMINATED)
		if (fecha.after(eventVS.dateBegin) && fecha.before(eventVS.dateFinish)) eventVS.setState(EventVS.State.ACTIVE)
		if (fecha.before(eventVS.dateBegin)) eventVS.setState(EventVS.State.AWAITING)
		log.debug("setEventDatesState - state ${eventVS.state.toString()}")
		return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}
	
	
	ResponseVS checkDatesEventVS (EventVS eventVS, Locale locale) {
		log.debug("checkDatesEventVS")
		if(eventVS.state && eventVS.state == EventVS.State.CANCELLED) {
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS)
		}
		if(eventVS.dateBegin.after(eventVS.dateFinish)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:messageSource.getMessage(
                'error.dateBeginAfterdateFinishalMsg', null, locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(eventVS.dateFinish) && eventVS.state != EventVS.State.TERMINATED) {
			EventVS.withTransaction {
				eventVS.state = EventVS.State.TERMINATED
				eventVS.save()
			}
		} else if(eventVS.dateBegin.after(fecha) && eventVS.state != EventVS.State.AWAITING) {
			EventVS.withTransaction {
				eventVS.state = EventVS.State.AWAITING
				eventVS.save()
			}
		} else if(eventVS.dateBegin.before(fecha) && eventVS.dateFinish.after(fecha) &&
                eventVS.state != EventVS.State.ACTIVE) {
			EventVS.withTransaction {
				eventVS.state = EventVS.State.ACTIVE
				eventVS.save()
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, message:eventVS?.estado?.toString())
	}
	
	//{"operation":"EVENT_CANCELLATION","accessControlURL":"...","eventId":"..","state":"CANCELLED","UUID":"..."}
	private ResponseVS checkCancelEventJSONData(JSONObject cancelDataJSON, Locale locale) {
		int status = ResponseVS.SC_ERROR_REQUEST
		TypeVS typeVS = TypeVS.ERROR
		String msg
		try {
			TypeVS operationType = TypeVS.valueOf(cancelDataJSON.operation)
			if (cancelDataJSON.accessControlURL && cancelDataJSON.eventId &&
				cancelDataJSON.state && (TypeVS.EVENT_CANCELLATION == operationType) &&
				((EventVS.State.CANCELLED == EventVS.State.valueOf(cancelDataJSON.state)) ||
					(EventVS.State.DELETED_FROM_SYSTEM == EventVS.State.valueOf(cancelDataJSON.state)))) {
				status = ResponseVS.SC_OK
			} else {
				msg = messageSource.getMessage('eventCancellationDataError', null, locale)
			}
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('eventCancellationDataError', null, locale)
		}
		if(ResponseVS.SC_OK == status) typeVS = TypeVS.EVENT_CANCELLATION
		else log.error("checkCancelEventJSONData - msg: ${msg} - data:${cancelDataJSON.toString()}")
		return new ResponseVS(statusCode:status, message:msg, type:typeVS)
	}
	
	public ResponseVS cancelEvent(MessageSMIME messageSMIMEReq, Locale locale) {
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		UserVS signer = messageSMIMEReq.userVS
		EventVS eventVS
		String msg
		try {
			log.debug("cancelEvent - message: ${smimeMessageReq.getSignedContent()}")
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			ResponseVS responseVS = checkCancelEventJSONData(messageJSON, locale)
			if(ResponseVS.SC_OK !=  responseVS.statusCode) return responseVS
			byte[] certChainBytes
			EventVS.withTransaction {
				eventVS = EventVS.findWhere(accessControlEventVSId:Long.valueOf(messageJSON.eventId))
				certChainBytes = eventVS?.certChainAccessControl
			}
			if(!eventVS) {
				msg = messageSource.getMessage('eventVSNotFound', [messageJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
			}
			if(eventVS.state != EventVS.State.ACTIVE) {
				msg = messageSource.getMessage('eventAllreadyCancelledMsg', [messageJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_CANCELLATION_REPEATED, type:TypeVS.ERROR, message:msg)
            }
			Collection<X509Certificate> certColl = CertUtil.fromPEMToX509CertCollection(certChainBytes)
			X509Certificate accessControlCert = certColl.iterator().next()
			if(!signatureVSService.isSignerCertificate(messageSMIMEReq.getSigners(), accessControlCert)) {
				msg = messageSource.getMessage('eventCancelacionCertError', null, locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					type:TypeVS.ERROR, message:msg, eventVS:eventVS)
			}
			//new state must be or CANCELLED or DELETED
			EventVS.State newState = EventVS.State.valueOf(messageJSON.state)
			if(!(newState == EventVS.State.DELETED_FROM_SYSTEM || newState == EventVS.State.CANCELLED)) {
				msg = messageSource.getMessage('eventCancelacionStateError', [messageJSON.state].toArray(), locale)
				log.error("cancelEvent new state error - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR,
                        message:msg, eventVS:eventVS)
			}
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = eventVS.accessControlVS.serverURL
			String subject = messageSource.getMessage('mime.subject.eventCancellationValidated', null, locale)
			SMIMEMessageWrapper smimeMessageResp = signatureVSService.
					getMultiSignedMimeMessage(fromUser, toUser, smimeMessageReq, subject)
			MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
				smimeParent:messageSMIMEReq, eventVS:eventVS, content:smimeMessageResp.getBytes())
			if (!messageSMIMEResp.validate()) {
				messageSMIMEResp.errors.each {
					log.debug("messageSMIMEResp - error: ${it}")
				}
			}
			MessageSMIME.withTransaction {
				if (!messageSMIMEResp.save()) {
					messageSMIMEResp.errors.each {
						log.error("cancel event error saving messageSMIMEResp - ${it}")}
				}
			}
			eventVS.state = newState
			eventVS.dateCanceled = DateUtils.getTodayDate();
			EventVS.withTransaction {
				if (!eventVS.save()) {
					eventVS.errors.each {
						log.error("cancel event error saving eventVS - ${it}")}
				}
			}
			log.debug("cancelEvent - cancelled event with id: ${eventVS.id}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK,message:msg, type:TypeVS.EVENT_CANCELLATION,
                    data:messageSMIMEResp, eventVS:eventVS)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('eventCancellationDataError', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg,eventVS:eventVS,type:TypeVS.ERROR)
		}
	}

	public Map getEventVSElectionMap(EventVSElection eventVS) {
		def eventVSMap = [id: eventVS.id, dateCreated: eventVS.dateCreated,
			subject:eventVS.subject, content:eventVS.content,
			tags:eventVS.tagVSSet?.collect {tagItem -> return [id:tagItem.id, content:tagItem.name]},
			duracion:DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(eventVS.getDateBegin().getTime() -
                    eventVS.getDateFinish().getTime()),
			URL:eventVS.url,
			state:eventVS.state.toString(),
			dateBegin: eventVS.getDateBegin(),
			dateFinish:eventVS.getDateFinish(),
            dateBeginStr:DateUtils.getSpanishFormattedStringFromDate(eventVS.getDateBegin()),
            dateFinishStr:DateUtils.getSpanishFormattedStringFromDate(eventVS.getDateFinish()),
            accessControlEventVSId: eventVS.accessControlEventVSId,
			eventVSRootCertURL:"${grailsLinkGenerator.link(controller:"certificateVS", action:"eventCA")}eventCA?eventAccessControlURL=${eventVS.url}",
			accessControlVoteVSInfoURL: "${eventVS.accessControlVS?.serverURL}/eventVS/${eventVS.accessControlEventVSId}/voteVSInfo",
			voteVSInfoURL:"${grailsApplication.config.grails.serverURL}/eventVS/votes?eventAccessControlURL=${eventVS.url}"]
			def accessControlMap = [serverURL:eventVS.accessControlVS?.serverURL, name:eventVS.accessControlVS?.name]
			eventVSMap.accessControl = accessControlMap
            def controlCenterMap = [serverURL:"${grailsApplication.config.grails.serverURL}",
                    name:"${grailsApplication.config.VotingSystem.serverName}"]
            eventVSMap.controlCenterMap = controlCenterMap
			if(eventVS.userVS) eventVSMap.userVS = "${eventVS.userVS?.name} ${eventVS.userVS?.firstName}"
			else eventVSMap.userVS = null
		eventVSMap.fieldsEventVS = eventVS.fieldsEventVS?.collect {option ->
            return [id:option.id, content:option.content]}
		return eventVSMap
	}
	
	boolean isUserAdmin(String nif) {
		log.debug("isUserAdmin - nif: ${nif}")
		if(!administradoresSistema) {
			administradoresSistema = Arrays.asList("${grailsApplication.config.VotingSystem.adminsDNI}".split(","))
		}
		return administradoresSistema.contains(nif)
	}

}