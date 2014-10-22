package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONObject
import static org.springframework.context.i18n.LocaleContextHolder.*
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS

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
	
    ResponseVS saveRequest(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		UserVS signerVS = messageSMIMEReq.getUserVS()
        AccessRequest request = new AccessRequest(messageSMIMEReq.getSMIME().getSignedContent(),
                signerVS.getTimeStampToken()?.getTimeStampInfo().getGenTime())
		String msg
        AccessRequestVS accessRequestVS = AccessRequestVS.findWhere(
                userVS:signerVS, eventVSElection:request.eventVS, state:TypeVS.OK)
        if (accessRequestVS){
            msg = "${grailsApplication.config.grails.serverURL}/messageSMIME/${accessRequestVS.messageSMIME.id}"
            log.error("$methodName - ACCESS REQUEST_REPEATED - ${msg}")
            return new ResponseVS(data:accessRequestVS, type:TypeVS.ACCESS_REQUEST_ERROR, message:msg,
                    eventVS:request.eventVS, statusCode:ResponseVS.SC_ERROR_REQUEST_REPEATED,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "requestRepeated"))
        } else {
            accessRequestVS = new AccessRequestVS(userVS:signerVS, messageSMIME:messageSMIMEReq,
                    state: AccessRequestVS.State.OK, hashAccessRequestBase64:request.hashAccessRequestBase64,
                    eventVSElection:request.eventVS).save()
            if (!accessRequestVS) { accessRequestVS.errors.each { log.error("$methodName - ERROR - ${it}")}}
            return new ResponseVS(type:TypeVS.ACCESS_REQUEST, statusCode:ResponseVS.SC_OK,
                    eventVS:request.eventVS, data:accessRequestVS)
        }
    }

    private class AccessRequest {
        String eventURL, hashAccessRequestBase64;
        TypeVS operation;
        EventVSElection eventVS;
        public AccessRequest(String signedContent, Date timeStampDate) throws ExceptionVS {
            JSONObject messageJSON = JSON.parse(signedContent)
            if(!messageJSON.eventId) throw new ValidationExceptionVS(this.getClass(), "missing param 'eventId'");
            if(!messageJSON.eventURL) throw new ValidationExceptionVS(this.getClass(), "missing param 'eventURL'");
            eventURL = messageJSON.eventURL
            if(!messageJSON.hashAccessRequestBase64) throw new ValidationExceptionVS(this.getClass(), "missing param 'hashAccessRequestBase64'");
            hashAccessRequestBase64 = messageJSON.hashAccessRequestBase64
            eventVS = EventVSElection.get(Long.valueOf(messageJSON.eventId))
            if(!eventVS) throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage('eventVSNotFound',
                    [messageJSON.eventId].toArray(), locale))
            if(!eventVS.isActive(timeStampDate)) {
                throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage("timeStampRangeErrorMsg",
                        [DateUtils.getDayWeekDateStr(timeStampDate), DateUtils.getDayWeekDateStr(eventVS.getDateBegin()),
                         DateUtils.getDayWeekDateStr(eventVS.getDateFinish())].toArray(), locale))
            }
            AccessRequestVS accessRequestVS = AccessRequestVS.findWhere(hashAccessRequestBase64:hashAccessRequestBase64)
            if (accessRequestVS) {
                log.error("AccessRequest -  hashRepeated: '$hashAccessRequestBase64'")
                throw new ValidationExceptionVS(this.getClass(), messageSource.getMessage(
                        'hashRepeatedError', null, locale))
            }
        }

    }


}