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

import javax.xml.bind.annotation.adapters.HexBinaryAdapter

@Transactional
class VoteVSService {
	
	def messageSource
	def grailsApplication
	def signatureVSService
	
    synchronized ResponseVS validateVote(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		EventVSElection eventVS = messageSMIMEReq.eventVS
		String localServerURL = grailsApplication.config.grails.serverURL
		String msg
		try {
			SMIMEMessage smimeMessageReq = messageSMIMEReq.getSmimeMessage()
			CertificateVS voteVSCertificate = smimeMessageReq.getVoteVS().getCertificateVS()
			FieldEventVS optionSelected = eventVS.checkOptionId(smimeMessageReq.getVoteVS().getOptionSelected().getId())
			if (!optionSelected) {
				msg = messageSource.getMessage('voteOptionNotFoundErrorMsg',
					[smimeMessageReq.getVoteVS().getOptionSelected().getId()].toArray(), locale)
				log.error ("$methodName - ERROR OPTION -> '${msg}'")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                        message:msg, type:TypeVS.VOTE_ERROR, eventVS:eventVS)
			}
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = eventVS.controlCenterVS.serverURL
			String subject = messageSource.getMessage('voteValidatedByAccessControlMsg', null, locale)
			smimeMessageReq.setMessageID("${localServerURL}/messageSMIME/${messageSMIMEReq.id}")
			SMIMEMessage smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
				fromUser,toUser, smimeMessageReq, subject)
			messageSMIMEReq.type = TypeVS.ACCESS_CONTROL_VALIDATED_VOTE
			messageSMIMEReq.content = smimeMessageResp.getBytes()
            messageSMIMEReq.save()
			voteVSCertificate.state = CertificateVS.State.USED;
			VoteVS voteVS = new VoteVS(optionSelected:optionSelected, eventVS:eventVS, state:VoteVS.State.OK,
                    certificateVS:voteVSCertificate, messageSMIME:messageSMIMEReq).save()
            ResponseVS modelResponseVS = new ResponseVS(statusCode: ResponseVS.SC_OK, contentType:ContentTypeVS.VOTE,
                    type:TypeVS.ACCESS_CONTROL_VALIDATED_VOTE, data:messageSMIMEReq, eventVS:eventVS)
			return new ResponseVS(statusCode:ResponseVS.SC_OK, data:[responseVS:modelResponseVS])
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.VOTE_ERROR, eventVS:eventVS,
                    message:messageSource.getMessage('voteErrorMsg', null, locale))
		}
    }
	
	private ResponseVS checkCancelJSONData(JSONObject cancelDataJSON, Locale locale) {
		def originHashCertVote = cancelDataJSON.originHashCertVote
		def hashCertVSBase64 = cancelDataJSON.hashCertVSBase64
		def originHashAccessRequest = cancelDataJSON.originHashAccessRequest
		def hashAccessRequestBase64 = cancelDataJSON.hashAccessRequestBase64
		if(!originHashCertVote || !hashCertVSBase64 || !originHashAccessRequest || !hashAccessRequestBase64 ||
                (TypeVS.CANCEL_VOTE != TypeVS.valueOf(cancelDataJSON.operation))) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.CANCEL_VOTE_ERROR,
				message:messageSource.getMessage('voteCancellationDataError', null, locale))
		}
		def hashCertVoteVS = CMSUtils.getHashBase64(originHashCertVote, ContextVS.VOTING_DATA_DIGEST)
		def hashRequest = CMSUtils.getHashBase64(originHashAccessRequest, ContextVS.VOTING_DATA_DIGEST)
		if (!hashAccessRequestBase64.equals(hashRequest))
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
				'voteCancellationAccessRequestHashError', null, locale), type:TypeVS.CANCEL_VOTE_ERROR)
		if (!hashCertVSBase64.equals(hashCertVoteVS))
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
					'voteCancellationHashCertificateError', null, locale), type:TypeVS.CANCEL_VOTE_ERROR)
		return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}

	public synchronized ResponseVS processCancel (MessageSMIME messageSMIME, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		UserVS signer = messageSMIME.getUserVS();
		SMIMEMessage smimeMessage = messageSMIME.getSmimeMessage();
		log.debug ("$methodName")
		MessageSMIME messageSMIMEResp;
		EventVSElection eventVSElection;
        def cancelDataJSON = JSON.parse(messageSMIME.getSmimeMessage().getSignedContent())
        ResponseVS responseVS = checkCancelJSONData(cancelDataJSON, locale)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        def hashCertVSBase64 = cancelDataJSON.hashCertVSBase64
        def hashAccessRequestBase64 = cancelDataJSON.hashAccessRequestBase64
        String msg
        def accessRequestVS = AccessRequestVS.findWhere(hashAccessRequestBase64:hashAccessRequestBase64,
                state:AccessRequestVS.State.OK)
        if (!accessRequestVS) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                message:messageSource.getMessage('voteCancellationAccessRequestNotFoundError', null, locale),
                type:TypeVS.CANCEL_VOTE_ERROR, metaInf:MetaInfMsg.getErrorMsg(methodName, "accessRequestVSNotFound"))
        CertificateVS certificateVS = CertificateVS.findWhere(hashCertVSBase64:hashCertVSBase64, state:CertificateVS.State.USED)
        if (!certificateVS){
            msg = messageSource.getMessage('voteCancellationCsrRequestNotFoundError', null, locale)
            log.error("$methodName - ERROR CSR NOT FOUND - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.CANCEL_VOTE_ERROR,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "voteVSCancellationCertificateVSMissing"))
        }
        else eventVSElection = certificateVS.eventVSElection
        def voteVS = VoteVS.findWhere(certificateVS:certificateVS, state:VoteVS.State.OK)
        if(!voteVS) throw new ExceptionVS("VoteVS not found")
        Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().genTime
        if(!eventVSElection.isActive(timeStampDate)) {
            msg = messageSource.getMessage('timestampDateErrorMsg',
                    [timeStampDate, eventVSElection.dateBegin, eventVSElection.getDateFinish()].toArray(), locale)
            log.error("$methodName - DATE ERROR - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg, type:TypeVS.CANCEL_VOTE_ERROR,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "voteVSCancellationOutOfDate"))
        }
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = eventVSElection.controlCenterVS.serverURL
        String subject = messageSource.getMessage('mime.subject.voteCancellationValidated', null, locale)
        smimeMessage.setMessageID("${grailsApplication.config.grails.serverURL}/messageSMIME/${messageSMIME.id}")
        SMIMEMessage smimeMessageReq = signatureVSService.getMultiSignedMimeMessage(
                fromUser, toUser, smimeMessage, subject)
        messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIME, eventVS:eventVSElection)
        String controlCenterURL = eventVSElection.controlCenterVS.serverURL
        String eventURL = "${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVSElection.id}"
        String voteCancellerURL = "${controlCenterURL}/voteVSCanceller?url=${eventURL}"
        ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(smimeMessageReq.getBytes(),
                ContentTypeVS.JSON_SIGNED, voteCancellerURL)
        if (ResponseVS.SC_OK == responseVSControlCenter.statusCode) {
            SMIMEMessage smimeMessageResp = new SMIMEMessage(new ByteArrayInputStream(
                    responseVSControlCenter.message.getBytes()))
            if(!smimeMessageReq.contentDigestStr.equals(smimeMessageResp.contentDigestStr)) {
                msg = messageSource.getMessage('controlCenterCommunicationErrorMsg', [responseVS.message].toArray(), locale)
                log.debug ("$methodName - ${msg}")
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, eventVS:eventVSElection,
                        type:TypeVS.CANCEL_VOTE_ERROR, metaInf:MetaInfMsg.getErrorMsg(methodName, "smimeContentMismatchError"))
            }
            signatureVSService.validateSignersCerts(smimeMessageResp, locale)
            messageSMIMEResp.content = smimeMessageResp.getBytes()
            messageSMIMEResp.smimeMessage = smimeMessageResp
            messageSMIMEResp.save()
        } else {
            responseVSControlCenter.eventVS = eventVSElection
            responseVSControlCenter.metaInf = MetaInfMsg.getErrorMsg(methodName, "voteVSCancellationControlCenterCancellationError")
            return responseVSControlCenter
        }
        VoteVSCanceller voteCanceller = new VoteVSCanceller(messageSMIME:messageSMIMEResp, accessRequestVS:accessRequestVS,
                state:VoteVSCanceller.State.CANCELLATION_OK,
                originHashAccessRequestBase64:cancelDataJSON.originHashAccessRequest,
                originHashCertVSBase64:cancelDataJSON.originHashCertVote,
                hashAccessRequestBase64:hashAccessRequestBase64,
                hashCertVSBase64:hashCertVSBase64,
                eventVSElection:eventVSElection, voteVS:voteVS).save()
        if (!voteCanceller) {voteCanceller.errors.each { log.error("$methodName - error - ${it}")}}
        voteVS.state = VoteVS.State.CANCELLED
        voteVS.save()
        accessRequestVS.state = AccessRequestVS.State.CANCELLED
        accessRequestVS.save()
        certificateVS.state = CertificateVS.State.CANCELLED
        certificateVS.save()
        log.error("$methodName - saved voteVSCanceller.id: '${voteCanceller.id}'")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.CANCEL_VOTE, data:messageSMIMEResp,
                contentType:ContentTypeVS.JSON_SIGNED, eventVS:eventVSElection)
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
	
}