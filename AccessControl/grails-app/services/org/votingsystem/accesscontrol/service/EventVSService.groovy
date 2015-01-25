package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.throwable.ValidationExceptionVS

import static org.springframework.context.i18n.LocaleContextHolder.*
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.StringUtils

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class EventVSService {
		
	static transactional = true

	def messageSource
	def subscriptionVSService
	def grailsApplication
	def signatureVSService
	def systemService
	
	ResponseVS checkEventVSDates (EventVS eventVS) {
		if(eventVS.state && eventVS.state == EventVS.State.CANCELLED) {
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS)
		}
		if(eventVS.dateBegin.after(eventVS.dateFinish)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:messageSource.getMessage('error.dateBeginAfterdateFinishalMsg', null, locale))
		}
		Date currentDate = Calendar.getInstance().getTime()
		if (currentDate.after(eventVS.dateFinish) &&
			eventVS.state != EventVS.State.TERMINATED) {
            eventVS.state = EventVS.State.TERMINATED
            eventVS.save()
		} else if(eventVS.dateBegin.after(currentDate) &&
			eventVS.state != EventVS.State.PENDING) {
            eventVS.state = EventVS.State.PENDING
            eventVS.save()
		} else if(eventVS.dateBegin.before(currentDate) &&
			eventVS.dateFinish.after(currentDate) &&
			eventVS.state != EventVS.State.ACTIVE) {
            eventVS.state = EventVS.State.ACTIVE
            eventVS.save()
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS, message:eventVS?.state?.toString())
	}
	
   ResponseVS setEventDatesState (EventVS eventVS) {
       if(!eventVS.dateBegin) eventVS.dateBegin = Calendar.getInstance().getTime();
       Date todayDate = new Date(System.currentTimeMillis() + 1);// to avoid race conditions
       if(eventVS.dateBegin.after(eventVS.dateFinish)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
                    'dateRangeErrorMsg', [eventVS.dateBegin, eventVS.dateFinish].toArray(), locale))
		}
		if (todayDate.after(eventVS.dateFinish)) eventVS.setState(EventVS.State.TERMINATED)
		if (todayDate.after(eventVS.dateBegin) && todayDate.before(eventVS.dateFinish))
			eventVS.setState(EventVS.State.ACTIVE)
		if (todayDate.before(eventVS.dateBegin)) eventVS.setState(EventVS.State.PENDING)
		return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS)
	}

    private class EventVSCancelRequest {
        String accessControlURL;
        EventVS.State state
        TypeVS operation;
        EventVS eventVS;
        public EventVSCancelRequest(String signedContent) throws ExceptionVS {
            JSONObject messageJSON = JSON.parse(signedContent)
            if(!messageJSON.eventId) throw new ValidationExceptionVS(this.getClass(), "missing param 'eventId'");
            eventVS = EventVS.get(Long.valueOf(messageJSON.eventId))
            if(!eventVS) throw new ExceptionVS(messageSource.getMessage('eventVSNotFound',
                    [messageJSON.eventId].toArray(), locale))
            if(eventVS.state != EventVS.State.ACTIVE) throw new ExceptionVS(messageSource.getMessage(
                    'eventNotActiveMsg', [messageJSON.eventId].toArray(), locale))
            if(!messageJSON.state) throw new ValidationExceptionVS(this.getClass(), "missing param 'state'");
            if(!messageJSON.accessControlURL) throw new ValidationExceptionVS(this.getClass(), "missing param 'accessControlURL'");
            if(!messageJSON.operation) throw new ValidationExceptionVS(this.getClass(), "missing param 'operation'");
            if(TypeVS.EVENT_CANCELLATION != TypeVS.valueOf(messageJSON.operation)) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'EVENT_CANCELLATION' - operation found: " + messageJSON.operation)
            state = EventVS.State.valueOf(messageJSON.state)
            if(!((EventVS.State.CANCELLED == state) || (EventVS.State.DELETED_FROM_SYSTEM == state))) {
                throw new ValidationExceptionVS(this.getClass(), "invalid 'EVENT_CANCELLATION' state: '${state.toString()}'");
            }
            String requestURL = StringUtils.checkURL(messageJSON.accessControlURL)
            if(!requestURL.equals(grailsApplication.config.grails.serverURL))  throw new ValidationExceptionVS(
                    this.getClass(),messageSource.getMessage('accessControlURLError', [
                    grailsApplication.config.grails.serverURL, requestURL].toArray(), locale))
        }
    }
	
	public ResponseVS cancelEvent(MessageSMIME messageSMIMEReq) {
		SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
		UserVS signer = messageSMIMEReq.userVS
        EventVSCancelRequest request = new EventVSCancelRequest(smimeMessageReq.getSignedContent())
        if(!(request.eventVS.userVS?.nif.equals(signer.nif) || systemService.isUserAdmin(signer.nif))) throw new ExceptionVS(
                messageSource.getMessage('userWithoutPrivilege', null, locale))
        String msg = (request.state == EventVS.State.CANCELLED)? messageSource.getMessage(
                'eventCancelled', [request.eventVS.id].toArray(), locale) : messageSource.getMessage(
                'eventDeleted', [request.eventVS.id].toArray(),  locale)
        SMIMEMessage smimeMessageResp
        String fromUser = grailsApplication.config.vs.serverName
        String subject = messageSource.getMessage('mime.subject.eventCancellationValidated', null, locale)
        if(request.eventVS instanceof EventVSElection) {
            String toUser = ((EventVSElection)request.eventVS).getControlCenterVS()?.name
            smimeMessageResp = signatureVSService.getSMIMEMultiSigned(
                    fromUser, toUser, smimeMessageReq, subject)
            String controlCenterUrl = ((EventVSElection)request.eventVS).getControlCenterVS().serverURL
            ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(smimeMessageResp.getBytes(),
                    ContentTypeVS.SIGNED, "$controlCenterUrl/eventVSElection/cancelled");
            if(ResponseVS.SC_OK == responseVSControlCenter.statusCode ||
                    ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVSControlCenter.statusCode) {
                msg = "$msg - " + messageSource.getMessage('controlCenterNotified', [controlCenterUrl].toArray(), locale)
            } else {
                msg = "$msg - " + messageSource.getMessage('controlCenterCommunicationErrorMsg',
                        [controlCenterUrl].toArray(), locale)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                        type:TypeVS.ERROR, message:msg, eventVS:request.eventVS)
            }
        } else smimeMessageResp = signatureVSService.getSMIMEMultiSigned(
                fromUser, signer.getNif(), smimeMessageReq, subject)
        messageSMIMEReq.setSMIME(smimeMessageResp)
        request.eventVS.state = request.state
        request.eventVS.dateCanceled = Calendar.getInstance().getTime()
        request.eventVS.save()
        log.debug("EventVS with id '${request.eventVS.id}' changed to state '${request.state.toString()}'")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, type:TypeVS.EVENT_CANCELLATION,
                messageSMIME:messageSMIMEReq, eventVS:request.eventVS, contentType: ContentTypeVS.JSON_SIGNED)

    }

	public Map getEventVSMap(EventVS eventVSItem) {
		if(eventVSItem instanceof EventVSElection) return getEventVSElectionMap(eventVSItem)
		else if(eventVSItem instanceof EventVSManifest) return getEventVSManifestMap(eventVSItem)
		else if(eventVSItem instanceof EventVSClaim) return getEventVSClaimMap(eventVSItem)
	}
	
	public Map getEventVSElectionMap(EventVSElection eventVSItem) {
		//log.debug("eventVSItem: ${eventVSItem.id} - state ${eventVSItem.state}")
		def eventVSMap = [id: eventVSItem.id, dateCreated: eventVSItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVSItem.id}",
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
			dateBegin:eventVSItem.getDateBegin(), dateFinish:eventVSItem.getDateFinish(),
            dateBeginStr:DateUtils.getDateStr(eventVSItem.getDateBegin(), "dd/MMM/yyyy"),
            dateFinishStr:DateUtils.getDateStr(eventVSItem.getDateFinish(),"dd/MMM/yyyy HH:mm")]
		if(eventVSItem.userVS) eventVSMap.userVS = "${eventVSItem.userVS?.name} ${eventVSItem.userVS?.firstName}"
		def accessControlMap = [serverURL:grailsApplication.config.grails.serverURL,
				name:grailsApplication.config.vs.serverName]
		eventVSMap.accessControl = accessControlMap
		eventVSMap.fieldsEventVS = eventVSItem.fieldsEventVS?.collect {opcion ->
				return [id:opcion.id, content:opcion.content]}
		ControlCenterVS controlCenter = eventVSItem.controlCenterVS
		def controlCenterMap = [id:controlCenter.id, serverURL:controlCenter.serverURL, name:controlCenter.name,
			eventVSstatsURL:"${controlCenter.serverURL}/eventVSElection/stats?eventAccessControlURL=${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVSItem.id}"]
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
			dateFinish:eventVSItem.getDateFinish(),
            dateBeginStr:DateUtils.getDateStr(eventVSItem.getDateBegin(), "dd/MMM/yyyy HH:mm"),
            dateFinishStr:DateUtils.getDateStr(eventVSItem.getDateFinish(), "dd/MMM/yyyy HH:mm")]
		if(eventVSItem.userVS) eventVSMap.userVS = "${eventVSItem.userVS?.name} ${eventVSItem.userVS?.firstName}"
		eventVSMap.numSignatures = PDFDocumentVS.countByEventVSAndState(eventVSItem,
			PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
		return eventVSMap
	}
	
	public Map getEventVSClaimMap(EventVSClaim eventVSItem) {
		//log.debug("eventVSItem: ${eventVSItem.id} - state ${eventVSItem.state}")
		def eventVSMap = [id: eventVSItem.id, dateCreated: eventVSItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/eventVSClaim/${eventVSItem.id}",
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
			dateFinish:eventVSItem.getDateFinish(),
            dateBeginStr:DateUtils.getDateStr(eventVSItem.getDateBegin(), "dd/MMM/yyyy HH:mm"),
            dateFinishStr:DateUtils.getDateStr(eventVSItem.getDateFinish(), "dd/MMM/yyyy HH:mm")]
		if(eventVSItem.userVS) eventVSMap.userVS = "${eventVSItem.userVS?.name} ${eventVSItem.userVS?.firstName}"
		SignatureVS.withTransaction {
			eventVSMap.numSignatures = SignatureVS.countByEventVS(eventVSItem)
		}
		def accessControlMap = [serverURL:grailsApplication.config.grails.serverURL,
				name:grailsApplication.config.vs.serverName]
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
		eventMap.put("dateInit", DateUtils.getDateStr(event.getDateBegin()))
		eventMap.put("dateFinish", DateUtils.getDateStr(event.getDateFinish()))
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