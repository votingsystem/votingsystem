package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.StringUtils
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSClaim
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.SignatureVS
import org.votingsystem.model.EventVSManifest
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.PDFDocumentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EventVSService {
		
	static transactional = true
	
	List<String> administradoresSistema
	def messageSource
	def subscriptionVSService
	def grailsApplication
	def signatureVSService
	def filesService

	
	ResponseVS checkDatesEventVS (EventVS eventVS, Locale locale) {
		log.debug("checkDatesEventVS")
		if(eventVS.state && eventVS.state == EventVS.State.CANCELLED) {
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS)
		}
		if(eventVS.dateBegin.after(eventVS.dateFinish)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:messageSource.getMessage('error.dateBeginAfterdateFinishalMsg', null, locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(eventVS.dateFinish) &&
			eventVS.state != EventVS.State.TERMINATED) {
			EventVSElection.withTransaction {
				eventVS.state = EventVS.State.TERMINATED
				eventVS.save()
			}
		} else if(eventVS.dateBegin.after(fecha) &&
			eventVS.state != EventVS.State.AWAITING) {
			EventVSElection.withTransaction {
				eventVS.state = EventVS.State.AWAITING
				eventVS.save()
			}
		} else if(eventVS.dateBegin.before(fecha) &&
			eventVS.dateFinish.after(fecha) &&
			eventVS.state != EventVS.State.ACTIVE) {
			EventVSElection.withTransaction {
				eventVS.state = EventVS.State.ACTIVE
				eventVS.save()
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS)
	}
	
   ResponseVS setEventDatesState (EventVS eventVS, Locale locale) {
		EventVS.State state
		if(eventVS.dateBegin.after(eventVS.dateFinish)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
                    'dateRangeErrorMsg', [eventVS.dateBegin, eventVS.dateFinish].toArray(), locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(eventVS.dateFinish)) eventVS.setState(EventVS.State.TERMINATED)
		if (fecha.after(eventVS.dateBegin) && fecha.before(eventVS.dateFinish))
			eventVS.setState(EventVS.State.ACTIVE)
		if (fecha.before(eventVS.dateBegin)) eventVS.setState(EventVS.State.AWAITING)
		log.debug("setEventDatesState - state ${eventVS.state.toString()}")
		return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS)
	}
	
	boolean isUserAdmin(String nif) {
		if(!administradoresSistema) {
			administradoresSistema = Arrays.asList(
			"${grailsApplication.config.VotingSystem.adminsDNI}".split(","))
		}
		return administradoresSistema.contains(nif)
	}
   
	//{"operation":"EVENT_CANCELLATION","accessControlURL":"...","eventId":"..","state":"CANCELLED","UUID":"..."}
	private ResponseVS checkCancelEventJSONData(JSONObject cancelDataJSON, Locale locale) {
		int status = ResponseVS.SC_ERROR_REQUEST
		TypeVS typeRespuesta = TypeVS.ERROR
		String msg
		try {
			TypeVS operationType = TypeVS.valueOf(cancelDataJSON.operation)
			if (cancelDataJSON.accessControlURL && cancelDataJSON.eventId && 
				cancelDataJSON.state && (TypeVS.EVENT_CANCELLATION == operationType) &&
				((EventVS.State.CANCELLED == EventVS.State.valueOf(cancelDataJSON.state)) ||
					(EventVS.State.DELETED_FROM_SYSTEM == EventVS.State.valueOf(cancelDataJSON.state)))) {
				String requestURL = StringUtils.getCheckURL(cancelDataJSON.accessControlURL)
				String serverURL = grailsApplication.config.grails.serverURL
				if(requestURL.equals(serverURL))  status = ResponseVS.SC_OK
				else msg = messageSource.getMessage('accessControlURLError', [serverURL, requestURL].toArray(), locale)

			} else {
				msg = messageSource.getMessage(
					'eventCancellationDataError', null, locale)
			}
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('eventCancellationDataError', null, locale)
		}
		if(ResponseVS.SC_OK == status) typeRespuesta = TypeVS.EVENT_CANCELLATION
		else log.error("checkCancelEventJSONData - msg: ${msg} - data:${cancelDataJSON.toString()}")
		return new ResponseVS(statusCode:status, message:msg, type:typeRespuesta)
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
			EventVS.withTransaction {
				eventVS = EventVS.findWhere(id:Long.valueOf(messageJSON.eventId))
			}
			if(!eventVS) {
				msg = messageSource.getMessage('eventVSNotFound', [messageJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
			} else if(eventVS.state != EventVS.State.ACTIVE) {
				msg = messageSource.getMessage('eventNotActiveMsg', [messageJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
			}
			if(eventVS.userVS?.nif.equals(signer.nif) || isUserAdmin(signer.nif)){
				log.debug("UserVS con privilegios para cancelar eventVS")
				switch(eventVS.state) {
					case EventVS.State.CANCELLED:
						 msg = messageSource.getMessage('eventCancelled', [messageJSON?.eventId].toArray(), locale)
						 break;
					 case EventVS.State.DELETED_FROM_SYSTEM:
						 msg = messageSource.getMessage('eventDeleted', [messageJSON?.eventId].toArray(), locale)
						 break;
				}
				SMIMEMessageWrapper smimeMessageResp
				String fromUser = grailsApplication.config.VotingSystem.serverName
				String toUser = null
				String subject = messageSource.getMessage(
					'mime.subject.eventCancellationValidated', null, locale)
				if(eventVS instanceof EventVSElection) {
					smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
						fromUser, toUser, smimeMessageReq, subject)
					String controlCenterUrl = ((EventVSElection)eventVS).getControlCenterVS().serverURL
					toUser = ((EventVSElection)eventVS).getControlCenterVS()?.name
					String cancelServiceURL = controlCenterUrl + "/eventVSElection/cancelled"
					ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(smimeMessageResp.getBytes(),
                            org.votingsystem.model.ContentTypeVS.SIGNED, cancelServiceURL);
					log.debug("responseVSControlCenter - status: ${responseVSControlCenter.statusCode}")
					if(ResponseVS.SC_OK == responseVSControlCenter.statusCode ||
						ResponseVS.SC_CANCELLATION_REPEATED == responseVSControlCenter.statusCode) {
						msg = msg + " - " + messageSource.getMessage(
							'controlCenterNotified', [controlCenterUrl].toArray(), locale)
					} else {
						msg = msg + " - " + messageSource.getMessage('controlCenterCommunicationErrorMsg',
							[controlCenterUrl].toArray(), locale)
						log.error("cancelEvent - msg: ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
							type:TypeVS.ERROR, message:msg, eventVS:eventVS)
					}
				} else {
					toUser = signer.getNif()
					smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
						fromUser, toUser, smimeMessageReq, subject)
				}
				log.debug("cancel event - msg:${msg}")
				MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
					smimeParent:messageSMIMEReq, eventVS:eventVS,  content:smimeMessageResp.getBytes())
				MessageSMIME.withTransaction {
					if (!messageSMIMEResp.save()) {
						messageSMIMEResp.errors.each {
							log.error("cancel event - save messageSMIMEResp error - ${it}")}
					}
					
				}
				eventVS.state = EventVS.State.valueOf(messageJSON.state)
				eventVS.dateCanceled = new Date(System.currentTimeMillis());
				log.debug("eventVS validated")
				EventVSElection.withTransaction {
					if (!eventVS.save()) {eventVS.errors.each {log.error("cancel event error saving eventVS - ${it}")}}
				}
				log.debug("updated eventVS.id: ${eventVS.id}")
				return new ResponseVS(statusCode:ResponseVS.SC_OK,message:msg, type:TypeVS.EVENT_CANCELLATION,
                        data:messageSMIMEResp, eventVS:eventVS)
			} else {
				msg = messageSource.getMessage('userWithoutPrivilege', null, locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR,
                        message:msg, eventVS:eventVS)
			}	
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('eventCancellationDataError', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                     eventVS:eventVS, type:TypeVS.ERROR)
		}
	}

	public Map getEventVSMap(EventVS eventVSItem) {
		if(eventVSItem instanceof EventVSElection) return getEventVSElectionMap(eventVSItem)
		else if(eventVSItem instanceof EventVSManifest) return getEventVSManifestMap(eventVSItem)
		else if(eventVSItem instanceof EventVSClaim) return getEventVSClaimMap(eventVSItem)
	}
	
	public Map getEventVSElectionMap(EventVSElection eventVSItem) {
		//log.debug("eventVSItem: ${eventVSItem.id} - state ${eventVSItem.state}")
		def eventVSMap = [id: eventVSItem.id, dateCreated: eventVSItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/eventVS/${eventVSItem.id}",
			publishRequestURL:"${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVSItem.id}/signed",
			validatedPublishRequestURL:"${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVSItem.id}/validated",
			subject:eventVSItem.subject, content:eventVSItem.content,
			cardinality:eventVSItem.cardinality?.toString(),
			tags:eventVSItem.tagVSSet?.collect {tag ->
						return [id:tag.id, content:tag.name]},
			duracion:DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(
                    eventVSItem.getDateBegin().getTime() - eventVSItem.getDateFinish().getTime()),
			backupAvailable:eventVSItem.backupAvailable, state:eventVSItem.state.toString(),
			voteVSInfoURL:"${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVSItem.id}/voteVSInfo",
			dateBegin:eventVSItem.getDateBegin(), dateFinish:eventVSItem.getDateFinish()]
		if(eventVSItem.userVS) eventVSMap.userVS = "${eventVSItem.userVS?.name} ${eventVSItem.userVS?.firstName}"
		def accessControlMap = [serverURL:grailsApplication.config.grails.serverURL,
				name:grailsApplication.config.VotingSystem.serverName]
		eventVSMap.accessControl = accessControlMap
		eventVSMap.fieldsEventVS = eventVSItem.fieldsEventVS?.collect {opcion ->
				return [id:opcion.id, content:opcion.content]}
		ControlCenterVS controlCenter = eventVSItem.controlCenterVS
		def controlCenterMap = [id:controlCenter.id, serverURL:controlCenter.serverURL, name:controlCenter.name,
			eventVSStatisticsURL:"${controlCenter.serverURL}/eventVSElection/statistics?eventAccessControlURL=${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVSItem.id}"]
		eventVSMap.controlCenter = controlCenterMap
		eventVSMap.eventCACertificate = "${grailsApplication.config.grails.serverURL}/certificateVS/eventCA/${eventVSItem.id}"
		return eventVSMap
	}


	public Map getEventVSManifestMap(EventVSManifest eventVSItem) {
		//log.debug("eventVSItem: ${eventVSItem.id} - state ${eventVSItem.state}")
		def eventVSMap = [id: eventVSItem.id, dateCreated: eventVSItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/eventVSManifest/${eventVSItem.id}",
			urlPDF:"${grailsApplication.config.grails.serverURL}/eventVSManifest/signed/${eventVSItem.id}",
			subject:eventVSItem.subject, content: eventVSItem.content,
			tags:eventVSItem.tagVSSet?.collect {tag -> return [id:tag.id, content:tag.name]},
			duracion:DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(
				eventVSItem.getDateBegin().getTime() - eventVSItem.getDateFinish().getTime()),
			state:eventVSItem.state.toString(),
			backupAvailable:eventVSItem.backupAvailable,
			dateBegin:eventVSItem.getDateBegin(),
			dateFinish:eventVSItem.getDateFinish()]
		if(eventVSItem.userVS) eventVSMap.userVS = "${eventVSItem.userVS?.name} ${eventVSItem.userVS?.firstName}"
		eventVSMap.numSignatures = PDFDocumentVS.countByEventVSAndState(eventVSItem,
			PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
		return eventVSMap
	}
	
	public Map getEventVSClaimMap(EventVSClaim eventVSItem) {
		//log.debug("eventVSItem: ${eventVSItem.id} - state ${eventVSItem.state}")
		def eventVSMap = [id: eventVSItem.id, dateCreated: eventVSItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/eventVS/${eventVSItem.id}",
			publishRequestURL:"${grailsApplication.config.grails.serverURL}/eventVSClaim/${eventVSItem.id}/signed",
			validatedPublishRequestURL:"${grailsApplication.config.grails.serverURL}/eventVSClaim/${eventVSItem.id}/validated",
			subject:eventVSItem.subject, content:eventVSItem.content,
			cardinality:eventVSItem.cardinality?.toString(),
			tags:eventVSItem.tagVSSet?.collect {tag ->
						return [id:tag.id, content:tag.name]},
			backupAvailable:eventVSItem.backupAvailable,
			duracion:DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(
				eventVSItem.getDateBegin().getTime() - eventVSItem.getDateFinish().getTime()),
			state:eventVSItem.state.toString(),
			dateBegin:eventVSItem.getDateBegin(),
			dateFinish:eventVSItem.getDateFinish()]
		if(eventVSItem.userVS) eventVSMap.userVS = "${eventVSItem.userVS?.name} ${eventVSItem.userVS?.firstName}"
		SignatureVS.withTransaction {
			eventVSMap.numSignatures = SignatureVS.countByEventVS(eventVSItem)
		}
		def accessControlMap = [serverURL:grailsApplication.config.grails.serverURL,
				name:grailsApplication.config.VotingSystem.serverName]
		eventVSMap.accessControl = accessControlMap
		eventVSMap.fieldsEventVS = eventVSItem.fieldsEventVS?.collect {campoItem ->
				return [id:campoItem.id, content:campoItem.content]}
		return eventVSMap
	}
	
	public Map getMetaInfMap(EventVS event) {
		Map eventMap = [:];
		eventMap.put("id", event.id);
		eventMap.put("serverURL", "${grailsApplication.config.grails.serverURL}")
		eventMap.put("subject", event.subject)
		eventMap.put("dateInit", DateUtils.getStringFromDate(event.getDateBegin()))
		eventMap.put("dateFinish", DateUtils.getStringFromDate(event.getDateFinish()))
		if(event instanceof EventVSElection) {
			eventMap.put("type", TypeVS.VOTING_EVENT.toString())
		} else if(event instanceof EventVSClaim) {
			eventMap.put("type", TypeVS.CLAIM_EVENT.toString());
		} else if(event instanceof EventVSManifest) {
			eventMap.put("type", TypeVS.MANIFEST_EVENT.toString());
		}
		log.debug("getMetaInfMap - Event type: ${eventMap.type?.toString()}")
		return eventMap
	}

}