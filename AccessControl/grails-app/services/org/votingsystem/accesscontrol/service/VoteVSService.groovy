package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.AccessRequestVS
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.FieldEventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteRequestCsrVS
import org.votingsystem.model.VoteVS
import org.votingsystem.model.VoteVSCanceller
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.HttpHelper

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.cert.X509Certificate

class VoteVSService {
	
	def messageSource
	def grailsApplication
	def signatureVSService
	
    synchronized ResponseVS validateVote(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug ("validateVote - ")
		EventVSElection eventVS = messageSMIMEReq.eventVS
		String localServerURL = grailsApplication.config.grails.serverURL
		String msg
		try {
			SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
			CertificateVS voteVSCertificate = smimeMessageReq.getVoteVS().getCertificateVS()
			FieldEventVS optionSelected = eventVS.checkOptionId(smimeMessageReq.getVoteVS().getOptionSelected().getId())
			if (!optionSelected) {
				msg = messageSource.getMessage('voteOptionNotFoundErrorMsg',
					[smimeMessageReq.getVoteVS().getOptionSelected().getId()].toArray(), locale)
				log.error ("validateVote - ERROR OPTION -> '${msg}'")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                        message:msg, type:TypeVS.VOTE_ERROR, eventVS:eventVS)
			}
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = eventVS.controlCenterVS.serverURL
			String subject = messageSource.getMessage('voteValidatedByAccessControlMsg', null, locale)
			smimeMessageReq.setMessageID("${localServerURL}/messageSMIME/${messageSMIMEReq.id}")
			SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
				fromUser,toUser, smimeMessageReq, subject)
			messageSMIMEReq.type = TypeVS.ACCESS_CONTROL_VALIDATED_VOTE
			messageSMIMEReq.content = smimeMessageResp.getBytes()
			MessageSMIME messageSMIMEResp = messageSMIMEReq
			MessageSMIME.withTransaction { messageSMIMEResp.save() }
			voteVSCertificate.state = CertificateVS.State.USED;
			VoteVS voteVS = new VoteVS(optionSelected:optionSelected, eventVS:eventVS, state:VoteVS.State.OK,
                    certificateVS:voteVSCertificate, messageSMIME:messageSMIMEResp)
			VoteVS.withTransaction { voteVS.save() }
			//X509Certificate controlCenterCert = smimeMessageReq.getVoteVS()?.getServerCerts()?.iterator()?.next()
            X509Certificate controlCenterCert = CertUtil.fromPEMToX509CertCollection(
                    eventVS.certChainControlCenter)?.iterator()?.next()
			Map data = [certificate:controlCenterCert, messageSMIME:messageSMIMEResp]
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS,
				type:TypeVS.ACCESS_CONTROL_VALIDATED_VOTE, data:data)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.VOTE_ERROR, eventVS:eventVS,
                    message:messageSource.getMessage('voteErrorMsg', null, locale))
		}
    }
	
	private ResponseVS checkCancelJSONData(JSONObject cancelDataJSON) {
		def originHashCertVote = cancelDataJSON.originHashCertVote
		def hashCertVoteBase64 = cancelDataJSON.hashCertVoteBase64
		def originHashAccessRequest = cancelDataJSON.originHashAccessRequest
		def hashAccessRequestBase64 = cancelDataJSON.hashAccessRequestBase64
		if(!originHashCertVote || !hashCertVoteBase64 || !originHashAccessRequest || !hashAccessRequestBase64) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.CANCEL_VOTE_ERROR,
				message:messageSource.getMessage('voteCancellationDataError', null, locale))
		}
		def hashCertVoteVS = CMSUtils.getHashBase64(originHashCertVote,
			"${grailsApplication.config.VotingSystem.votingHashAlgorithm}")
		def hashRequest = CMSUtils.getHashBase64(originHashAccessRequest,
			"${grailsApplication.config.VotingSystem.votingHashAlgorithm}")
		if (!hashAccessRequestBase64.equals(hashRequest))
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
				'voteCancellationAccessRequestHashError', null, locale), type:TypeVS.CANCEL_VOTE_ERROR)
		if (!hashCertVoteBase64.equals(hashCertVoteVS))
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
					'voteCancellationHashCertificateError', null, locale), type:TypeVS.CANCEL_VOTE_ERROR)
		return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}
	
	private ResponseVS checkCancelResponseJSONData(JSONObject requestDataJSON,
           JSONObject responseDataJSON, Locale locale) {
		ResponseVS responseVS = checkCancelJSONData(responseDataJSON)
		if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
		if(!(requestDataJSON.hashCertVoteBase64.equals(responseDataJSON.hashCertVoteBase64)) ||
			!(requestDataJSON.hashAccessRequestBase64.equals(responseDataJSON.hashAccessRequestBase64))){
			String msg = messageSource.getMessage('cancelDataWithErrorsMsg', null, locale)
			log.error("checkCancelResponseJSONData - requestDataJSON: '${requestDataJSON}'" + 
				" - responseDataJSON: '${responseDataJSON}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.CANCEL_VOTE_ERROR)
		} else return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}
	
	public synchronized ResponseVS processCancel (MessageSMIME messageSMIME, Locale locale) {
		UserVS signer = messageSMIME.getUserVS();
		SMIMEMessageWrapper smimeMessage = messageSMIME.getSmimeMessage();
		log.debug ("processCancel - ${smimeMessage.getSignedContent()}")
		MessageSMIME messageSMIMEResp;
		EventVSElection eventVSElection;
		try {
			def cancelDataJSON = JSON.parse(messageSMIME.getSmimeMessage().getSignedContent())
			ResponseVS responseVS = checkCancelJSONData(cancelDataJSON)
			if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
			def hashCertVoteBase64 = cancelDataJSON.hashCertVoteBase64
			def hashAccessRequestBase64 = cancelDataJSON.hashAccessRequestBase64

			String msg
			def accessRequestVS = AccessRequestVS.findWhere(hashAccessRequestBase64:hashAccessRequestBase64)
			if (!accessRequestVS) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:messageSource.getMessage('voteCancellationAccessRequestNotFoundError', null, locale),
				type:TypeVS.CANCEL_VOTE_ERROR)
			if(accessRequestVS.state.equals(AccessRequestVS.State.CANCELLED)) {
				msg = messageSource.getMessage(
					'voteCancellationAlreadyCancelledError', null, locale)
				log.error("processCancel - ERROR ACCESS REQUEST ALREADY CANCELLED - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_CANCELLATION_REPEATED, 
					message:msg, type:TypeVS.CANCEL_VOTE_ERROR)
			}
			CertificateVS certificateVS = CertificateVS.findWhere(hashCertVoteBase64:hashCertVoteBase64)
			if (!certificateVS){
				msg = messageSource.getMessage(
					'voteCancellationCsrRequestNotFoundError', null, locale)
				log.error("processCancel - ERROR CSR NOT FOUND - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.CANCEL_VOTE_ERROR)
			} 
			else eventVSElection = certificateVS.eventVSElection
			def voteVS = VoteVS.findWhere(certificateVS:certificateVS)
			VoteVSCanceller anuladorVoto = VoteVSCanceller.findWhere(
				hashCertVoteBase64:hashCertVoteBase64)
			if(anuladorVoto) {
				String voteURL = "${grailsApplication.config.grails.serverURL}/voteVS/${anuladorVoto.voteVS.id}"
				msg = messageSource.getMessage('voteAlreadyCancelled',
						[voteURL].toArray(), locale)
				log.error("processCancel - REAPEATED CANCEL REQUEST - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_CANCELLATION_REPEATED,
					data:anuladorVoto.messageSMIME, type:TypeVS.CANCEL_VOTE_ERROR,
					message:msg, eventVS:eventVSElection)
			}
			Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().genTime
			if(!eventVSElection.isActive(timeStampDate)) {
				msg = messageSource.getMessage('timestampDateErrorMsg', 
					[timeStampDate, eventVSElection.dateBegin,
						eventVSElection.getDateFinish()].toArray(), locale)
				log.error("processCancel - DATE ERROR - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.CANCEL_VOTE_ERROR)
			}
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = eventVSElection.controlCenterVS.serverURL
			String subject = messageSource.getMessage(
				'mime.subject.voteCancellationValidated', null, locale)
			smimeMessage.setMessageID("${grailsApplication.config.grails.serverURL}" +
				"/messageSMIME/${messageSMIME.id}")
			SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
				fromUser, toUser, smimeMessage, subject)
			messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIME, eventVS:eventVSElection)
			VoteVSCanceller.State cancellerState
			if(!voteVS){//Access request without vote
				msg = messageSource.getMessage('voteCancellationVoteNotFoundError', null, locale)
				log.debug ("processCancel - VOTE NOT FOUND - ${msg}")
				messageSMIMEResp.content = smimeMessageResp.getBytes()
				cancellerState = VoteVSCanceller.State.CANCELLATION_WITHOUT_VOTE//Access request without vote
			} else {//Notify Control Center
				def cancelDataJSONResp
				String msgArg
				String controlCenterURL = eventVSElection.controlCenterVS.serverURL
				String eventURL = "${grailsApplication.config.grails.serverURL}/eventVSElection/${eventVSElection.id}"
				String urlAnulacionVoto = "${controlCenterURL}/voteVSCanceller?url=${eventURL}"
				ResponseVS encryptResponse = signatureVSService.encryptSMIMEMessage(
					smimeMessageResp.getBytes(), eventVSElection.getControlCenterCert(), locale)
				if (ResponseVS.SC_OK != encryptResponse.statusCode) return encryptResponse
				ResponseVS responseVSControlCenter = HttpHelper.getInstance().sendData(encryptResponse.messageBytes,
                        ContentTypeVS.SIGNED_AND_ENCRYPTED, urlAnulacionVoto)
				if (ResponseVS.SC_OK == responseVSControlCenter.statusCode) {
					responseVSControlCenter = signatureVSService.decryptSMIMEMessage(
							responseVSControlCenter.message.getBytes(), locale)
					if(ResponseVS.SC_OK != responseVSControlCenter.statusCode) {
						msgArg = messageSource.getMessage(
							'encryptedMessageErrorMsg', null, locale)
						msg = messageSource.getMessage('controlCenterCommunicationErrorMsg',
							[msgArg].toArray(), locale)
						log.debug ("processCancel --- Problem with response encryption - ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
								message:msg, eventVS:eventVSElection, type:TypeVS.CANCEL_VOTE_ERROR)
					}
					smimeMessageResp = responseVSControlCenter.smimeMessage;//already decrypted
					//check message content
					cancelDataJSONResp = JSON.parse(smimeMessageResp.getSignedContent())
					responseVS = checkCancelResponseJSONData(cancelDataJSON, cancelDataJSONResp, locale)
					if(ResponseVS.SC_OK != responseVS.statusCode) {
						msg = messageSource.getMessage('controlCenterCommunicationErrorMsg',
							[responseVS.message].toArray(), locale)
						log.debug ("processCancel - ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
								message:msg, eventVS:eventVSElection, type:TypeVS.CANCEL_VOTE_ERROR)
					}
					messageSMIMEResp.content = smimeMessageResp.getBytes()
					cancellerState = VoteVSCanceller.State.CANCELLATION_OK
				} else if(ResponseVS.SC_CANCELLATION_REPEATED) {
					responseVSControlCenter =  signatureVSService.decryptSMIMEMessage(
						responseVSControlCenter.message.getBytes(), locale)
					if(ResponseVS.SC_OK != responseVSControlCenter.statusCode) {
						msgArg = messageSource.getMessage(
							'encryptedMessageErrorMsg', null, locale) 
						msg = messageSource.getMessage('controlCenterCommunicationErrorMsg',
							[msgArg].toArray(), locale)
						log.debug ("processCancel *** Problem with response encryption - ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
								message:msg, eventVS:eventVSElection, type:TypeVS.CANCEL_VOTE_ERROR)
					}
					smimeMessageResp = responseVSControlCenter.smimeMessage
					ResponseVS validationResponseVS = signatureVSService.
							validateSignersCertificate(smimeMessageResp, locale)
					if(!signatureVSService.isSystemSignedMessage((Set<UserVS>)validationResponseVS.data)) {
						msgArg = messageSource.getMessage('unknownReceipt', null, locale)
						msg = messageSource.getMessage('controlCenterCommunicationErrorMsg',
							[msgArg].toArray(), locale)
						log.error("processCancel - Not local receipt - ${msg}")
						messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT_ERROR,
                                content:smimeMessageResp.getBytes(), eventVS:eventVSElection, metaInf:msg)
						MessageSMIME.withTransaction {
							messageSMIMEResp.save()
						}
						return new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
							message:msg, eventVS:eventVSElection, type:TypeVS.CANCEL_VOTE_ERROR)
					}
					cancelDataJSONResp = JSON.parse(smimeMessageResp.getSignedContent())
					responseVS = checkCancelResponseJSONData(cancelDataJSON, cancelDataJSONResp)
					if(ResponseVS.SC_OK != responseVS.statusCode) {
						msg = messageSource.getMessage('controlCenterCommunicationErrorMsg',
							[responseVS.message].toArray(), locale)
						log.error("processCancel - response data with errors - ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
							message:msg, eventVS:eventVSElection, type:TypeVS.CANCEL_VOTE_ERROR)
					}		
					messageSMIMEResp.content = smimeMessageResp.getBytes()
					cancellerState = VoteVSCanceller.State.CANCELLATION_OK
				} else {
					responseVSControlCenter.eventVS = eventVSElection
					return responseVSControlCenter
				}
			}
			MessageSMIME.withTransaction { messageSMIMEResp.save()}
			anuladorVoto = new VoteVSCanceller(
				messageSMIME:messageSMIMEResp, state:cancellerState,
				accessRequestVS:accessRequestVS, voteRequestCsrVS:certificate.voteRequestCsrVS,
				originHashAccessRequestBase64:cancelDataJSON.originHashAccessRequest,
				originHashCertVoteBase64:cancelDataJSON.originHashCertVote,
				hashAccessRequestBase64:hashAccessRequestBase64,
				hashCertVoteBase64:hashCertVoteBase64,
				eventVSElection:eventVSElection, voteVS:voteVS)
			if (!anuladorVoto.save()) {anuladorVoto.errors.each { log.error("processCancel - error - ${it}")}}
			if(voteVS) {
				voteVS.state = VoteVS.State.CANCELLED
				voteVS.save()
			}
			accessRequestVS.state = AccessRequestVS.State.CANCELLED
			accessRequestVS.save()
			certificate.voteRequestCsrVS.state = VoteRequestCsrVS.State.CANCELLED
			certificate.state = CertificateVS.State.CANCELLED
			certificate.save()
			return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.CANCEL_VOTE, 
				data:messageSMIMEResp, eventVS:eventVSElection)
		}catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:ex.getMessage(), eventVS:eventVSElection, type:TypeVS.CANCEL_VOTE_ERROR)
		}
	}
	
	public Map getVotoMap(VoteVS voteVS) {
		if(!voteVS) return [:]
		HexBinaryAdapter hexConverter = new HexBinaryAdapter();
		String hashHex = hexConverter.marshal(voteVS.certificateVS?.hashCertVoteBase64?.getBytes());
		Map voteVSMap = [id:voteVS.id, hashCertVoteBase64:voteVS.certificateVS.hashCertVoteBase64,
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
	
	public Map getAnuladorVotoMap(VoteVSCanceller anulador) {
		if(!anulador) return [:]
		Map anuladorMap = [id:anulador.id,
			voteVSURL:"${grailsApplication.config.grails.serverURL}/voteVS/${anulador.voteVS.id}",
			anuladorSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${anulador.messageSMIME.id}"]
		return anuladorMap
	}
	
}