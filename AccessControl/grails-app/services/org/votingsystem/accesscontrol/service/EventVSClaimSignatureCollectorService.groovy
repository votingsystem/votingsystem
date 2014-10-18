package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import net.sf.json.JSONArray
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.ValidationExceptionVS
import static org.springframework.context.i18n.LocaleContextHolder.*

class EventVSClaimSignatureCollectorService {

    static scope = "prototype"

	def signatureVSService
	def grailsApplication
	def messageSource

    ResponseVS save (MessageSMIME messageSMIMEReq) {
        UserVS userVS = messageSMIMEReq.getUserVS()
        SignatureRequest request = new SignatureRequest(messageSMIMEReq.getSmimeMessage().getSignedContent(),
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
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = userVS.getNif()
        String subject = messageSource.getMessage('mime.subject.claimSignatureValidated', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getMultiSignedMimeMessage (fromUser, toUser,
                messageSMIMEReq.getSmimeMessage(), subject)
        messageSMIMEReq.setSmimeMessage(smimeMessageResp).setEventVS(request.eventVS)
        log.debug("save - claim signature OK - signer: ${userVS.nif}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIMEReq, eventVS:request.eventVS,
                messageSMIME: messageSMIMEReq, type:TypeVS.CLAIM_EVENT_SIGN, contentType:ContentTypeVS.JSON_SIGNED)
    }

	 public Map getStatisticsMap (EventVSClaim event) {
		 log.debug("getStatisticsMap - eventId: ${event?.id}")
		 if(!event) return null
		 def statisticsMap = new HashMap()
		 statisticsMap.fieldsEventVS = []
		 statisticsMap.id = event.id
		 statisticsMap.subject = event.subject
		 statisticsMap.numSignatures = SignatureVS.countByEventVS(event)
		 statisticsMap.state =  event.state.toString()
		 statisticsMap.dateBegin = event.getDateBegin()
		 statisticsMap.dateFinish = event.getDateFinish()
		 statisticsMap.publishRequestURL = "${grailsApplication?.config.grails.serverURL}" +
			 "/eventVSClaim/${event.id}/signed"
		 statisticsMap.validatedPublishRequestURL = "${grailsApplication?.config.grails.serverURL}" +
			 "/eventVSClaim/${event.id}/validated"
		 statisticsMap.signaturesInfoURL = "${grailsApplication?.config.grails.serverURL}" +
			 "/eventVSClaim/${event.id}/signaturesInfo"
		 statisticsMap.URL = "${grailsApplication.config.grails.serverURL}/eventVS/${event.id}"
		 event.fieldsEventVS.each { field ->
			 statisticsMap.fieldsEventVS.add(field.content)
		 }
		 return statisticsMap
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