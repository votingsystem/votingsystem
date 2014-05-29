package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper

class EventVSClaimSignatureCollectorService {

	def signatureVSService
	def grailsApplication
	def messageSource

    ResponseVS save (MessageSMIME messageSMIMEReq, Locale locale) {
        log.debug("save")
        def msg
        SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
        def messageJSON = JSON.parse(smimeMessage.getSignedContent())
        UserVS userVS = messageSMIMEReq.getUserVS()
        ResponseVS responseVS = checkClaimJSONData(messageJSON, locale)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        //log.debug("messageJSON: ${smimeMessage.getSignedContent()}")
        EventVSClaim eventVSClaim = EventVSClaim.get(messageJSON.id)
        if (!eventVSClaim) {
            msg = messageSource.getMessage('eventVSNotFound', [messageJSON.id].toArray() , locale)
            log.debug("save - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                type:TypeVS.CLAIM_EVENT_SIGNATURE_ERROR)
        } else {
            Date signatureTime = userVS.getTimeStampToken()?.getTimeStampInfo().getGenTime()
            if(!eventVSClaim.isActive(signatureTime)) {
                msg = messageSource.getMessage("checkedDateRangeErrorMsg", [signatureTime,
                        eventVSClaim.getDateBegin(), eventVSClaim.getDateFinish()].toArray(), locale)
                log.error(msg)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.CLAIM_EVENT_SIGNATURE_ERROR,
                        message:msg, eventVS:eventVSClaim)
            }
        }
        SignatureVS signatureVS = SignatureVS.findWhere(eventVS:eventVSClaim, userVS:userVS)
        if (!signatureVS || EventVS.Cardinality.MULTIPLE.equals(eventVSClaim.cardinality)) {
        log.debug("save - claim signature OK - signer: ${userVS.nif}")
        signatureVS = new SignatureVS(userVS:userVS, eventVS:eventVSClaim,
            type:TypeVS.CLAIM_EVENT_SIGN, messageSMIME:messageSMIMEReq)
        signatureVS.save();
        messageJSON.fieldsEventVS?.each { campoItem ->
            FieldEventVS campo = FieldEventVS.findWhere(id:campoItem.id?.longValue())
            if (campo) {
                new FieldValueEventVS(value:campoItem.value, signatureVS:signatureVS, fieldEventVS:campo).save()
            }
        }

        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = userVS.getNif()
        String subject = messageSource.getMessage('mime.subject.claimSignatureValidated', null, locale)

        SMIMEMessageWrapper smimeMessageResp = signatureVSService.
            getMultiSignedMimeMessage (fromUser, toUser, smimeMessage, subject)
        MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
            smimeParent:messageSMIMEReq, eventVS:eventVSClaim, content:smimeMessageResp.getBytes())
        MessageSMIME.withTransaction {
            messageSMIMEResp.save()
        }
        messageSMIMEResp.smimeMessage = smimeMessageResp
        return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIMEResp, eventVS:eventVSClaim,
            smimeMessage:smimeMessage, type:TypeVS.CLAIM_EVENT_SIGN, contentType:ContentTypeVS.JSON_SIGNED)
        } else {
            msg = messageSource.getMessage('claimSignatureRepeated',
                [userVS.nif, eventVSClaim.subject].toArray() , locale)
            log.error("save - ${msg} - signer: ${userVS.nif}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, eventVS:eventVSClaim, message:msg,
                type:TypeVS.CLAIM_EVENT_SIGNATURE_ERROR)
        }
    }

	 private ResponseVS checkClaimJSONData(JSONObject claimDataJSON, Locale locale) {
		 int status = ResponseVS.SC_ERROR_REQUEST
		 String msg
		 try {
			 TypeVS operationType = TypeVS.valueOf(claimDataJSON.operation)
			 if (claimDataJSON.id && claimDataJSON.URL && (TypeVS.SMIME_CLAIM_SIGNATURE == operationType)) {
				 status = ResponseVS.SC_OK
			 } else msg = messageSource.getMessage('claimSignatureWithErrorsMsg', null, locale)
		 } catch(Exception ex) {
			 log.error(ex.getMessage(), ex)
			 msg = messageSource.getMessage('claimSignatureWithErrorsMsg', null, locale)
		 }
		 if(ResponseVS.SC_OK != status) log.error("checkClaimJSONData - msg: ${msg} - data:${claimDataJSON.toString()}")
		 return new ResponseVS(statusCode:status, message:msg, type:TypeVS.SMIME_CLAIM_SIGNATURE)
	 }

	 public Map getStatisticsMap (EventVSClaim event, Locale locale) {
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
		 event.fieldsEventVS.each { campo ->
			 statisticsMap.fieldsEventVS.add(campo.content)
		 }
		 return statisticsMap
	 }

}