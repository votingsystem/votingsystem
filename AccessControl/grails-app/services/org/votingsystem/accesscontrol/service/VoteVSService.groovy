package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import static org.springframework.context.i18n.LocaleContextHolder.*

@Transactional
class VoteVSService {
	
	def messageSource
	def grailsApplication
	def signatureVSService

    synchronized ResponseVS validateVote(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		EventVSElection eventVS = messageSMIMEReq.eventVS
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
        CertificateVS voteVSCertificate = smimeMessageReq.getVoteVS().getCertificateVS()
        FieldEventVS optionSelected = eventVS.checkOptionId(smimeMessageReq.getVoteVS().getOptionSelected().getId())
        if (!optionSelected) throw new ExceptionVS(messageSource.getMessage('voteOptionNotFoundErrorMsg',
                    [smimeMessageReq.getVoteVS().getOptionSelected().getId()].toArray(), locale))
        String fromUser = grailsApplication.config.vs.serverName
        String toUser = eventVS.controlCenterVS.serverURL
        String subject = messageSource.getMessage('voteValidatedByAccessControlMsg', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned(
                fromUser,toUser, smimeMessageReq, subject)
        messageSMIMEReq.setType(TypeVS.ACCESS_CONTROL_VALIDATED_VOTE).setSMIME(smimeMessageResp)
        voteVSCertificate.state = CertificateVS.State.USED;
        VoteVS voteVS = new VoteVS(optionSelected:optionSelected, eventVS:eventVS, state:VoteVS.State.OK,
                certificateVS:voteVSCertificate, messageSMIME:messageSMIMEReq).save()
        return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType:ContentTypeVS.VOTE,
                type:TypeVS.ACCESS_CONTROL_VALIDATED_VOTE, messageSMIME:messageSMIMEReq, eventVS:eventVS)
    }

	public synchronized ResponseVS processCancel (MessageSMIME messageSMIME) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
		UserVS signer = messageSMIME.getUserVS();
		SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        VoteVSRequest request = new VoteVSRequest(messageSMIME.getSMIME().getSignedContent()).getCancelRequest()
        def accessRequestVS = AccessRequestVS.findWhere(hashAccessRequestBase64:request.hashAccessRequestBase64,
                state:AccessRequestVS.State.OK)
        if (!accessRequestVS) throw new ValidationExceptionVS(messageSource.getMessage(
                'voteCancellationAccessRequestNotFoundError', null, locale),
                MetaInfMsg.getErrorMsg(methodName, "accessRequestVSNotFound"))
        CertificateVS certificateVS = CertificateVS.findWhere(hashCertVSBase64:request.hashCertVSBase64,
                state:CertificateVS.State.USED)
        if (!certificateVS) throw new ValidationExceptionVS(messageSource.getMessage(
                'voteCancellationCsrRequestNotFoundError', null, locale),
                MetaInfMsg.getErrorMsg(methodName, "voteVSCancellationCertificateVSMissing"))
        def voteVS = VoteVS.findWhere(certificateVS:certificateVS, state:VoteVS.State.OK)
        if(!voteVS) throw new ExceptionVS("VoteVS not found")
        Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().genTime
        if(!certificateVS.eventVSElection.isActive(timeStampDate)) throw new ValidationExceptionVS(messageSource.getMessage(
                'timestampDateErrorMsg', [timeStampDate, certificateVS.eventVSElection.dateBegin,
                certificateVS.eventVSElection.getDateFinish()].toArray(), locale),
                MetaInfMsg.getErrorMsg(methodName, "voteVSCancellationOutOfDate"))
        String fromUser = grailsApplication.config.vs.serverName
        String toUser = certificateVS.eventVSElection.controlCenterVS.serverURL
        String subject = messageSource.getMessage('mime.subject.voteCancellationValidated', null, locale)
        smimeMessage.setMessageID("${grailsApplication.config.grails.serverURL}/messageSMIME/${messageSMIME.id}")
        SMIMEMessage smimeMessageReq = signatureVSService.getSMIMEMultiSigned(fromUser, toUser, smimeMessage, subject)
        String controlCenterURL = certificateVS.eventVSElection.controlCenterVS.serverURL
        String eventURL = "${grailsApplication.config.grails.serverURL}/eventVSElection/${certificateVS.eventVSElection.id}"
        String voteCancellerURL = "${controlCenterURL}/voteVSCanceller?url=${eventURL}"
        ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(smimeMessageReq.getBytes(),
                ContentTypeVS.JSON_SIGNED, voteCancellerURL)
        if (ResponseVS.SC_OK == responseVSControlCenter.statusCode) {
            SMIMEMessage smimeMessageResp = new SMIMEMessage(new ByteArrayInputStream(
                    responseVSControlCenter.message.getBytes()))
            if(!smimeMessageReq.contentDigestStr.equals(smimeMessageResp.contentDigestStr))  throw new ValidationExceptionVS(
                    "smimeContentMismatchError", MetaInfMsg.getErrorMsg(methodName, "smimeContentMismatchError"))
            signatureVSService.validateSignersCerts(smimeMessageResp)
            messageSMIME.setType(TypeVS.CANCEL_VOTE).setSMIME(smimeMessageResp).save()
        } else {
            responseVSControlCenter.eventVS = certificateVS.eventVSElection
            responseVSControlCenter.metaInf = MetaInfMsg.getErrorMsg(methodName, "voteVSCancellationControlCenterCancellationError")
            return responseVSControlCenter
        }
        VoteVSCanceller voteCanceller = new VoteVSCanceller(messageSMIME:messageSMIME, accessRequestVS:accessRequestVS,
                state:VoteVSCanceller.State.CANCELLATION_OK,
                originHashAccessRequestBase64:request.originHashAccessRequest,
                originHashCertVSBase64:request.originHashCertVote,
                hashAccessRequestBase64:request.hashAccessRequestBase64,
                hashCertVSBase64:request.hashCertVSBase64,
                eventVSElection:certificateVS.eventVSElection, voteVS:voteVS).save()
        if (!voteCanceller) {voteCanceller.errors.each { log.error("$methodName - error - ${it}")}}
        voteVS.setState(VoteVS.State.CANCELLED).save()
        accessRequestVS.setState(AccessRequestVS.State.CANCELLED).save()
        certificateVS.setState(CertificateVS.State.CANCELLED).save()
        log.debug("$methodName - saved voteVSCanceller with id '${voteCanceller.id}'")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.CANCEL_VOTE, messageSMIME: messageSMIME,
                contentType:ContentTypeVS.JSON_SIGNED, eventVS:certificateVS.eventVSElection)
	}
	
	public Map getVoteVSMap(VoteVS voteVS) {
		if(!voteVS) return [:]
		HexBinaryAdapter hexConverter = new HexBinaryAdapter();
		String hashHex = hexConverter.marshal(voteVS.certificateVS?.hashCertVSBase64?.getBytes());
		Map voteVSMap = [id:voteVS.id, hashCertVSBase64:voteVS.certificateVS.hashCertVSBase64,
			fieldEventVSId:voteVS.getFieldEventVS.id, eventVSElectionId:voteVS.eventVSElection.id,
			eventVSElectionURL:"${grailsApplication.config.grails.serverURL}/eventVSElection/${voteVS.eventVSElection?.id}",
			state:voteVS?.state?.toString(),
			certificateURL:"${grailsApplication.config.grails.serverURL}/certificateVS/voteVS/hashHex/${hashHex}",
			voteVSSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${voteVS.messageSMIME.id}"]
		if(VoteVS.State.CANCELLED == voteVS?.state) {
			voteVSMap.anulacionURL="${grailsApplication.config.grails.serverURL}/voteVSCanceller/voteVS/${voteVS.id}"
		}
		return voteVSMap
	}
	
	public Map getVoteVSCancellerMap(VoteVSCanceller canceller) {
		if(!canceller) throw new ExceptionVS("VoteVSCanceller null")
		Map cancellerMap = [id:canceller.id, voteVSURL:"${grailsApplication.config.grails.serverURL}/voteVS/${canceller.voteVS.id}",
			anuladorSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${canceller.messageSMIME.id}"]
		return cancellerMap
	}

    private class VoteVSRequest {
        String originHashCertVote, hashCertVSBase64, originHashAccessRequest, hashAccessRequestBase64;
        TypeVS operation;
        JSONObject messageJSON
        public VoteVSRequest(String signedContent) { messageJSON = JSON.parse(signedContent) }

        public VoteVSRequest getCancelRequest() {
            operation = TypeVS.valueOf(messageJSON.operation)
            if(!operation) throw new ValidationExceptionVS(this.getClass(), "missing param 'operation'");
            if(TypeVS.CANCEL_VOTE != operation) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'CANCEL_VOTE' - operation found: " + messageJSON.operation)
            originHashCertVote = messageJSON.originHashCertVote
            if(!originHashCertVote) throw new ValidationExceptionVS(this.getClass(), "missing param 'originHashCertVote'");
            hashCertVSBase64 = messageJSON.hashCertVSBase64
            if(!hashCertVSBase64) throw new ValidationExceptionVS(this.getClass(), "missing param 'hashCertVSBase64'");
            originHashAccessRequest = messageJSON.originHashAccessRequest
            if(!originHashAccessRequest) throw new ValidationExceptionVS(this.getClass(), "missing param 'originHashAccessRequest'");
            hashAccessRequestBase64 = messageJSON.hashAccessRequestBase64
            if(!hashAccessRequestBase64) throw new ValidationExceptionVS(this.getClass(), "missing param 'hashAccessRequestBase64'");
            if(!hashAccessRequestBase64.equals(CMSUtils.getHashBase64(originHashAccessRequest,
                    ContextVS.VOTING_DATA_DIGEST))) throw new ValidationExceptionVS(this.getClass(),
                    message:messageSource.getMessage('voteCancellationAccessRequestHashError', null, locale));
            if(!hashCertVSBase64.equals(CMSUtils.getHashBase64(originHashCertVote,
                    ContextVS.VOTING_DATA_DIGEST))) throw new ValidationExceptionVS(this.getClass(),
                    message:messageSource.getMessage('voteCancellationHashCertificateError', null, locale));
            return this
        }
    }

}