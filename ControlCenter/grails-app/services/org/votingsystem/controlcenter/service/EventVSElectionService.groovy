package org.votingsystem.controlcenter.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.StringUtils
import java.security.cert.X509Certificate
import static org.springframework.context.i18n.LocaleContextHolder.*

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
* */
@Transactional
class EventVSElectionService {
	
	//static scope = "session"
    LinkGenerator grailsLinkGenerator
	def messageSource
	def subscriptionVSService
    def grailsApplication
	def tagVSService
	def signatureVSService
	
	ResponseVS saveEvent(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		ResponseVS responseVS
		SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
		String msg
        AccessControlVS accessControl = subscriptionVSService.checkAccessControl(
                smimeMessageReq.getHeader("serverURL")[0])
        if(!accessControl) {
            msg = messageSource.getMessage('accessControlNotFound', [serverURL].toArray(), locale)
            log.debug("$methodName - ${msg}")
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "accessControlNotFound"))
        }
        def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
        if(!messageJSON.certCAVotacion || !messageJSON.userVS || !messageJSON.id ||
                !messageJSON.fieldsEventVS || !messageJSON.URL || !messageJSON.controlCenterURL) {
            msg = messageSource.getMessage('documentParamsErrorMsg', null, locale)
            log.error("$methodName - ERROR - ${msg} - document: ${messageJSON as JSON}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg,type:TypeVS.ERROR,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "documentParamsError"))
        }
        String controlCenterURL = grailsApplication.config.grails.serverURL
        String requestServerURL = StringUtils.checkURL(messageJSON.controlCenterURL)
        if (!controlCenterURL.equals(requestServerURL)) {
            log.debug("$methodName - WARNING - serverURL: ${controlCenterURL} - messageJSON.controlCenterURL: ${messageJSON.controlCenterURL}")
        }
        X509Certificate certCAVotacion = CertUtils.fromPEMToX509Cert(messageJSON.certCAVotacion?.bytes)

        X509Certificate userCert = CertUtils.fromPEMToX509Cert(messageJSON.userVS?.bytes)

        UserVS user = UserVS.getUserVS(userCert);
        //Publish request comes with Access Control cert
        responseVS = subscriptionVSService.checkUser(user)
        if(ResponseVS.SC_OK != responseVS.statusCode) {
            log.error("$methodName - USER CHECK ERROR - ${responseVS.message}")
            return  new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:responseVS.message, type:TypeVS.ERROR,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "checkAccesControlError"))
        }
        user = responseVS.userVS
        def eventVS = new EventVSElection(accessControlEventVSId:messageJSON.id, subject:messageJSON.subject,
                content:messageJSON.content, url:messageJSON.URL, accessControlVS:accessControl,
                userVS:user, dateBegin:DateUtils.getDateFromString(messageJSON.dateBegin),
                dateFinish:DateUtils.getDateFromString(messageJSON.dateFinish))
        responseVS = setEventDatesState(eventVS)
        if(ResponseVS.SC_OK != responseVS.statusCode) {
            return  new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:responseVS.message,
                    type:TypeVS.ERROR,  metaInf:MetaInfMsg.getErrorMsg(methodName, "setEventDatesState"))
        } else eventVS.setState(null)
        eventVS.save()

        X509Certificate controlCenterX509Cert = signatureVSService.getServerCert()
        CertificateVS eventVSControlCenterCertificate = new CertificateVS(
                state:CertificateVS.State.OK, type:CertificateVS.Type.ACTOR_VS, eventVSElection:eventVS,
                content:controlCenterX509Cert.getEncoded(), serialNumber:controlCenterX509Cert.getSerialNumber().longValue(),
                validFrom:controlCenterX509Cert?.getNotBefore(), validTo:controlCenterX509Cert?.getNotAfter()).save()

        Collection<X509Certificate> accessControlCerts = CertUtils.fromPEMToX509CertCollection (messageJSON.certChain?.getBytes())
        X509Certificate accessControlX509Cert = accessControlCerts.iterator().next()
        CertificateVS eventVSAccessControlCertificate = new CertificateVS(actorVS:accessControl,
                state:CertificateVS.State.OK, type:CertificateVS.Type.ACTOR_VS, eventVSElection:eventVS,
                content:accessControlX509Cert.getEncoded(), serialNumber:accessControlX509Cert.getSerialNumber().longValue(),
                validFrom:accessControlX509Cert?.getNotBefore(), validTo:accessControlX509Cert?.getNotAfter()).save()

        CertificateVS eventVSRootCertificate = new CertificateVS(actorVS:accessControl,
                state:CertificateVS.State.OK, type:CertificateVS.Type.VOTEVS_ROOT, eventVSElection:eventVS,
                content:certCAVotacion.getEncoded(), serialNumber:certCAVotacion.getSerialNumber().longValue(),
                validFrom:certCAVotacion?.getNotBefore(), validTo:certCAVotacion?.getNotAfter())
        eventVSRootCertificate.save()

        log.debug("$methodName - eventVSAccessControlCertificate.id: '${eventVSAccessControlCertificate.id}' " +
                " - eventVSRootCertificate: '${eventVSRootCertificate.id}'")

        saveFieldsEventVS(eventVS, messageJSON.fieldsEventVS)
        if (messageJSON.tags) {
            Set<TagVS> tags = tagVSService.save(messageJSON.tags)
            eventVS.setTagVSSet(tags)
        }
        eventVS.setState(EventVS.State.ACTIVE)
        eventVS.save()
        log.debug("$methodName - ACTIVATED event - '${eventVS.id}'")
        return new ResponseVS(statusCode:ResponseVS.SC_OK,  eventVS:eventVS, type:TypeVS.VOTING_EVENT)
	}
	
    Set<FieldEventVS> saveFieldsEventVS(EventVS eventVS, JSONArray fieldsEventVS) {
        log.debug("saveFieldsEventVS")
        def fieldsEventVSSet = fieldsEventVS.collect { opcionItem ->
            return  new FieldEventVS(eventVS:eventVS, content:opcionItem.content,
                    accessControlFieldEventId:opcionItem.id).save()
        }
        return fieldsEventVSSet
    }

	ResponseVS setEventDatesState (EventVS eventVS) {
		if(eventVS.dateBegin.after(eventVS.dateFinish)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
                    'dateRangeErrorMsg', [eventVS.dateBegin, eventVS.dateFinish].toArray(), locale))
		}
		Date fecha = Calendar.getInstance().getTime()
		if (fecha.after(eventVS.dateFinish)) eventVS.setState(EventVS.State.TERMINATED)
		if (fecha.after(eventVS.dateBegin) && fecha.before(eventVS.dateFinish)) eventVS.setState(EventVS.State.ACTIVE)
		if (fecha.before(eventVS.dateBegin)) eventVS.setState(EventVS.State.PENDING)
		log.debug("setEventDatesState - state ${eventVS.state.toString()}")
		return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}

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
	
	//{"operation":"EVENT_CANCELLATION","accessControlURL":"...","eventId":"..","state":"CANCELLED","UUID":"..."}
	private ResponseVS checkCancelEventJSONData(JSONObject cancelDataJSON) {
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
			} else msg = messageSource.getMessage('eventCancellationDataError', null, locale)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('eventCancellationDataError', null, locale)
		}
		if(ResponseVS.SC_OK == status) typeVS = TypeVS.EVENT_CANCELLATION
		else log.error("checkCancelEventJSONData - msg: ${msg} - data:${cancelDataJSON.toString()}")
		return new ResponseVS(statusCode:status, message:msg, type:typeVS)
	}
	
	public ResponseVS cancelEvent(MessageSMIME messageSMIMEReq) {
		SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
		UserVS signer = messageSMIMEReq.userVS
		EventVS eventVS
		String msg
        log.debug("cancelEvent - message: ${smimeMessageReq.getSignedContent()}")
        def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
        ResponseVS responseVS = checkCancelEventJSONData(messageJSON)
        if(ResponseVS.SC_OK !=  responseVS.statusCode) return responseVS
        EventVSElection.withTransaction {
            eventVS = EventVSElection.findWhere(accessControlEventVSId:Long.valueOf(messageJSON.eventId))
        }
        if(!eventVS) {
            msg = messageSource.getMessage('eventVSNotFound', [messageJSON?.eventId].toArray(),
                    locale)
            log.error("cancelEvent - msg: ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
        }
        if(eventVS.state != EventVS.State.ACTIVE) {
            msg = messageSource.getMessage('eventAllreadyCancelledMsg', [messageJSON?.eventId].toArray(),
                    locale)
            log.error("cancelEvent - msg: ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST_REPEATED, type:TypeVS.ERROR, message:msg)
        }
        CertificateVS accessControlCert = CertificateVS.findWhere(eventVSElection:eventVS, type:CertificateVS.Type.ACTOR_VS,
                actorVS:eventVS.accessControlVS)
        if(!accessControlCert) throw new ExceptionVS("missing Access Control Cert")

        if(!signatureVSService.isSignerCertificate(messageSMIMEReq.getSigners(), accessControlCert.getX509Cert())) {
            msg = messageSource.getMessage('eventCancelacionCertError', null, locale)
            log.error("cancelEvent - msg: ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg,
                    eventVS:eventVS)
        }
        //new state must be or CANCELLED or DELETED
        EventVS.State newState = EventVS.State.valueOf(messageJSON.state)
        if(!(newState == EventVS.State.DELETED_FROM_SYSTEM || newState == EventVS.State.CANCELLED)) {
            msg = messageSource.getMessage('eventCancelacionStateError', [messageJSON.state].toArray(), locale)
            log.error("cancelEvent new state error - msg: ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg, eventVS:eventVS)
        }
        String fromUser = grailsApplication.config.vs.serverName
        String toUser = eventVS.accessControlVS.serverURL
        String subject = messageSource.getMessage('mime.subject.eventCancellationValidated', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.
                getSMIMEMultiSigned(fromUser, toUser, smimeMessageReq, subject)
        messageSMIMEReq.setSMIME(smimeMessageResp)
        eventVS.state = newState
        eventVS.dateCanceled = Calendar.getInstance().getTime();
        eventVS.save()
        log.debug("cancelEvent - cancelled event with id: ${eventVS.id}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK,message:msg, type:TypeVS.EVENT_CANCELLATION,
                messageSMIME: messageSMIMEReq, eventVS:eventVS, contentType: ContentTypeVS.JSON_SIGNED)
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
            dateBeginStr:DateUtils.getDateStr(eventVS.getDateBegin()),
            dateFinishStr:DateUtils.getDateStr(eventVS.getDateFinish()),
            accessControlEventVSId: eventVS.accessControlEventVSId,
			eventVSRootCertURL:"${grailsLinkGenerator.link(controller:"certificateVS", action:"eventCA")}eventCA?eventAccessControlURL=${eventVS.url}",
			accessControlVoteVSInfoURL: "${eventVS.accessControlVS?.serverURL}/eventVS/${eventVS.accessControlEventVSId}/voteVSInfo",
			voteVSInfoURL:"${grailsApplication.config.grails.serverURL}/eventVS/votes?eventAccessControlURL=${eventVS.url}"]
			def accessControlMap = [serverURL:eventVS.accessControlVS?.serverURL, name:eventVS.accessControlVS?.name]
			eventVSMap.accessControl = accessControlMap
            def controlCenterMap = [serverURL:"${grailsApplication.config.grails.serverURL}",
                    name:"${grailsApplication.config.vs.serverName}"]
            eventVSMap.controlCenter = controlCenterMap
			if(eventVS.userVS) eventVSMap.userVS = "${eventVS.userVS?.name} ${eventVS.userVS?.firstName}"
			else eventVSMap.userVS = null
		eventVSMap.fieldsEventVS = eventVS.fieldsEventVS?.collect {option ->
            return [id:option.id, content:option.content]}
		return eventVSMap
	}

}