package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import net.sf.json.JSONArray
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.throwable.ValidationExceptionVS
import static org.springframework.context.i18n.LocaleContextHolder.*

class EventVSClaimSignatureCollectorService {

    static scope = "prototype"

	def signatureVSService
	def grailsApplication
	def messageSource

    ResponseVS save (MessageSMIME messageSMIMEReq) {
        UserVS userVS = messageSMIMEReq.getUserVS()
        SignatureRequest request = new SignatureRequest(messageSMIMEReq.getSMIME().getSignedContent(),
                userVS.getTimeStampToken()?.getTimeStampInfo().getGenTime())
        SignatureVS signatureVS = SignatureVS.findWhere(eventVS:request.eventVS, userVS:userVS)
        if(signatureVS && EventVS.Cardinality.EXCLUSIVE.equals(request.eventVS.cardinality))  throw new ExceptionVS(
                messageSource.getMessage('claimSignatureRepeated', [userVS.nif, request.eventVS.subject].toArray() , locale))
        signatureVS = new SignatureVS(userVS:userVS, eventVS:request.eventVS,
                type:TypeVS.CLAIM_EVENT_SIGN, messageSMIME:messageSMIMEReq).save();
        request.fieldsEventVS?.each { claimField ->
            FieldEventVS fieldEventVS = FieldEventVS.findWhere(id:Long.valueOf(claimField.id))
            if (fieldEventVS) {
                new FieldValueEventVS(value:claimField.value, signatureVS:signatureVS, fieldEventVS:fieldEventVS).save()
            } else throw new ExceptionVS("Signature with unknown fields '${claimField}'")
        }
        String fromUser = grailsApplication.config.vs.serverName
        String toUser = userVS.getNif()
        String subject = messageSource.getMessage('mime.subject.claimSignatureValidated', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned (fromUser, toUser,
                messageSMIMEReq.getSMIME(), subject)
        messageSMIMEReq.setSMIME(smimeMessageResp).setEventVS(request.eventVS)
        log.debug("save - claim signature OK - signer: ${userVS.nif}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIMEReq, eventVS:request.eventVS,
                messageSMIME: messageSMIMEReq, type:TypeVS.CLAIM_EVENT_SIGN, contentType:ContentTypeVS.JSON_SIGNED)
    }

	 public Map getStatsMap (EventVSClaim event) {
		 log.debug("getStatsMap - eventId: ${event?.id}")
		 if(!event) return null
		 def statsMap = new HashMap()
		 statsMap.fieldsEventVS = []
		 statsMap.id = event.id
		 statsMap.subject = event.subject
		 statsMap.numSignatures = SignatureVS.countByEventVS(event)
		 statsMap.state =  event.state.toString()
		 statsMap.dateBegin = event.getDateBegin()
		 statsMap.dateFinish = event.getDateFinish()
		 statsMap.publishRequestURL = "${grailsApplication?.config.grails.serverURL}" +
			 "/eventVSClaim/${event.id}/signed"
		 statsMap.validatedPublishRequestURL = "${grailsApplication?.config.grails.serverURL}" +
			 "/eventVSClaim/${event.id}/validated"
		 statsMap.signaturesInfoURL = "${grailsApplication?.config.grails.serverURL}" +
			 "/eventVSClaim/${event.id}/signaturesInfo"
		 statsMap.URL = "${grailsApplication.config.grails.serverURL}/eventVS/${event.id}"
		 event.fieldsEventVS.each { field ->
			 statsMap.fieldsEventVS.add(field.content)
		 }
		 return statsMap
	 }

    private class SignatureRequest {
        JSONArray fieldsEventVS;
        TypeVS operation;
        EventVSClaim eventVS;
        public SignatureRequest(String signedContent, Date timeStampDate) throws ExceptionVS {
            net.sf.json.JSONObject messageJSON = JSON.parse(signedContent)
            if(TypeVS.SMIME_CLAIM_SIGNATURE != TypeVS.valueOf(messageJSON.operation)) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'SMIME_CLAIM_SIGNATURE' - operation found: " + messageJSON.operation)
            eventVS = EventVSClaim.get(messageJSON.id)
            if (!eventVS) throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage('eventVSNotFound',
                        [messageJSON.id].toArray() , locale))
            if(!eventVS.isActive(timeStampDate)) {
                throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage("timeStampRangeErrorMsg",
                        [DateUtils.getDayWeekDateStr(timeStampDate), DateUtils.getDayWeekDateStr(eventVS.getDateBegin()),
                         DateUtils.getDayWeekDateStr(eventVS.getDateFinish())].toArray(), locale))
            }
            fieldsEventVS = messageJSON.fieldsEventVS
        }
    }

}