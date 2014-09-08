package org.votingsystem.controlcenter.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
* */
class VoteVSService {

    static transactional = true

    LinkGenerator grailsLinkGenerator
	def messageSource
	def signatureVSService
    def grailsApplication
	
	
	public synchronized ResponseVS validateVote(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		EventVSElection eventVS = messageSMIMEReq.eventVS
		String msg
        VoteVS voteVS = messageSMIMEReq.getSmimeMessage().getVoteVS()
        X509Certificate responseReceiverCert = voteVS.getX509Certificate()
        FieldEventVS optionSelected = FieldEventVS.findWhere(eventVS:eventVS,
                accessControlFieldEventId:voteVS.getOptionSelected().getId())
        if (!optionSelected) {
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
                    'votingOptionNotFound', [voteVS.getOptionSelected()?.getId(), eventVS.id].toArray(), locale))
        }
        CertificateVS certificateVS = new CertificateVS(isRoot:false, state: CertificateVS.State.OK,
                type:CertificateVS.Type.VOTEVS, content:voteVS.getX509Certificate().getEncoded(),
                hashCertVSBase64:voteVS.getHashCertVSBase64(),
                userVS:messageSMIMEReq.userVS, eventVSElection:eventVS,
                serialNumber:voteVS.getX509Certificate().getSerialNumber().longValue(),
                validFrom:voteVS.getX509Certificate().getNotBefore(),
                validTo:voteVS.getX509Certificate().getNotAfter())
        certificateVS.save()
        String localServerURL = grailsApplication.config.grails.serverURL
        String signedVoteDigest = messageSMIMEReq.getSmimeMessage().getContentDigestStr()

        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = eventVS.accessControlVS.name
        String subject = messageSource.getMessage('voteValidatedByAccessControlMsg', null, locale)
        messageSMIMEReq.getSmimeMessage().setMessageID("${localServerURL}/messageSMIME/${messageSMIMEReq.id}")

        SMIMEMessageWrapper smimeVoteValidation = signatureVSService.getMultiSignedMimeMessage(
                fromUser, toUser, messageSMIMEReq.getSmimeMessage(), subject)
        messageSMIMEReq.type = TypeVS.CONTROL_CENTER_VALIDATED_VOTE
        messageSMIMEReq.content = smimeVoteValidation.getBytes()
        messageSMIMEReq.save();
        //byte[] encryptResponseBytes = encryptResponse.messageBytes
        //String encryptResponseStr = new String(encryptResponseBytes)
        //log.debug(" - encryptResponseStr: ${encryptResponseStr}")
        ResponseVS responseVS = HttpHelper.getInstance().sendData(messageSMIMEReq.content,
                ContentTypeVS.VOTE, eventVS.accessControlVS.getVoteServiceURL())
        if (ResponseVS.SC_OK == responseVS.statusCode) {
            //ResponseVS validatedVoteResponse = signatureVSService.decryptSMIMEMessage(responseVS.messageBytes, locale)
            //SMIMEMessageWrapper smimeMessageResp = validatedVoteResponse.getSmimeMessage();
            SMIMEMessageWrapper smimeMessageResp = new SMIMEMessageWrapper(new ByteArrayInputStream(responseVS.messageBytes))
            if(!smimeMessageResp.getContentDigestStr().equals(signedVoteDigest)) {
                log.error("validateVote - ERROR digest sent: " + signedVoteDigest +
                        " - digest received: " + smimeMessageResp.getContentDigestStr())
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.VOTE_ERROR,
                        eventVS:eventVS, message:messageSource. getMessage('voteContentErrorMsg', null, locale))
            }
            responseVS = signatureVSService.validateVoteCerts(smimeMessageResp, eventVS, locale)
            if(ResponseVS.SC_OK != responseVS.statusCode) {
                log.error("validateVote - validateVoteValidationCerts ERROR - > ${responseVS.message}")
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                        type:TypeVS.VOTE_ERROR, eventVS:eventVS, message:responseVS.message)
            }
            messageSMIMEReq.type = TypeVS.ACCESS_CONTROL_VALIDATED_VOTE
            messageSMIMEReq.content = smimeMessageResp.getBytes()
            MessageSMIME messageSMIMEResp = messageSMIMEReq
            messageSMIMEResp.save()
            new VoteVS(optionSelected:optionSelected, eventVS:eventVS, state:VoteVS.State.OK,
                    certificateVS:certificateVS, messageSMIME:messageSMIMEResp).save()

            String voteURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${messageSMIMEResp.id}"
            Map modelData = [voteURL:voteURL, receiverCert: responseReceiverCert,
                     responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ACCESS_CONTROL_VALIDATED_VOTE,
                     eventVS:eventVS, contentType: ContentTypeVS.VOTE, data:messageSMIMEResp)]

            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:modelData)
        } else {
            msg = messageSource.getMessage('accessRequestVoteErrorMsg', [responseVS.message].toArray(), locale)
            log.error("validateVote - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.VOTE_ERROR, message:msg)
        }
	}
	
	public synchronized ResponseVS processCancel (MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug ("processCancel")
		EventVS eventVS
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		try {
			def cancelDataJSON = JSON.parse(smimeMessageReq.getSignedContent())
			def originHashCertVote = cancelDataJSON.originHashCertVote
			def hashCertVSBase64 = cancelDataJSON.hashCertVSBase64
			def hashCertVoteVS = CMSUtils.getHashBase64(originHashCertVote, ContextVS.VOTING_DATA_DIGEST)
			if (!hashCertVSBase64.equals(hashCertVoteVS))
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
						message:messageSource.getMessage(
						'voteCancellationHashCertificateError', null, locale))
			VoteVSCanceller voteVSCanceller = VoteVSCanceller.findWhere(
				hashCertVSBase64:hashCertVSBase64)
			if(voteVSCanceller) {
				String voteURL = "${grailsApplication.config.grails.serverURL}/voteVS/${voteVSCanceller.getVoteVS.id}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST_REPEATED, data:voteVSCanceller.messageSMIME,
                        type:TypeVS.CANCEL_VOTE_ERROR,ContentTypeVS.SIGNED_AND_ENCRYPTED,
					    message:messageSource.getMessage('voteAlreadyCancelled',
						[voteURL].toArray(), locale), eventVS:voteVSCanceller.eventVS)
			}
			def certificateVS = CertificateVS.findWhere(hashCertVSBase64:hashCertVSBase64)
			if (!certificateVS)
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.CANCEL_VOTE_ERROR,
					message:messageSource.getMessage( 'certNotFoundErrorMsg', null, locale))
			def voteVS = VoteVS.findWhere(certificateVS:certificateVS)
			if(!voteVS) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.CANCEL_VOTE_ERROR,
					message:messageSource.getMessage( 'voteCancellationVoteNotFoundError', null, locale))
			eventVS = voteVS.eventVS
			voteVS.state = VoteVS.State.CANCELLED
			voteVS.save()
			certificateVS.state = CertificateVS.State.CANCELLED
			certificateVS.save()
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = messageSMIMEReq.getUserVS()?.getNif()
			String subject = messageSource.getMessage('mime.subject.voteCancellationValidated', null, locale)
			SMIMEMessageWrapper smimeMessageResp = signatureVSService.
					getMultiSignedMimeMessage(fromUser, toUser, smimeMessageReq, subject)
			MessageSMIME messageSMIMEResp = new MessageSMIME(content:smimeMessageResp.getBytes(),
                    smimeParent:messageSMIMEReq, eventVS:eventVS, type:TypeVS.RECEIPT)
			messageSMIMEResp.save()
			if (!messageSMIMEResp.save()) {
			    messageSMIMEResp.errors.each { log.error("processCancel - ${it}") }
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.CANCEL_VOTE_ERROR,
                        eventVS:eventVS)
			}
			voteVSCanceller = new VoteVSCanceller(voteVS:voteVS, certificateVS:certificateVS, eventVSElection:eventVS,
                    state:VoteVSCanceller.State.CANCELLATION_OK,messageSMIME:messageSMIMEResp,
				    originHashCertVSBase64:originHashCertVote, hashCertVSBase64:hashCertVSBase64)
			if (!voteVSCanceller.save()) {
			    voteVSCanceller.errors.each { log.error("processCancel - ${it}") }
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.CANCEL_VOTE_ERROR,
                        eventVS:eventVS)
			} else {
				log.debug("processCancel - voteVSCanceller.id: ${voteVSCanceller.id}")
                Map modelData = [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS,
                        type:TypeVS.CANCEL_VOTE, contentType: ContentTypeVS.JSON_SIGNED, data:messageSMIMEResp)]
				return new ResponseVS(statusCode:ResponseVS.SC_OK, data:modelData)
			}			
		}catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:messageSource.getMessage(eventVS:eventVS, 'error.encryptErrorMsg', null, locale), )
		}
	}
			
	public Map getVoteVSMap(VoteVS voteVS) {
		if(!voteVS) return [:]
		HexBinaryAdapter hexConverter = new HexBinaryAdapter();
		String hashHex = hexConverter.marshal(voteVS.certificateVS?.hashCertVSBase64?.getBytes());
		Map voteVSMap = [id:voteVS.id, hashCertVSBase64:voteVS.certificateVS.hashCertVSBase64,
			    fieldEventVSId:voteVS.getFieldEventVS.fieldEventVSId,
                accessControlEventVSId: voteVS.eventVS.accessControlEventVSId,
			eventVSElectionURL:voteVS.eventVS?.url, state:voteVS?.state?.toString(),
			certificateURL:"${grailsApplication.config.grails.serverURL}/certificateVS/voteVS/hashHex/${hashHex}",
			voteVSSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${voteVS.messageSMIME.id}"]
		if(VoteVS.State.CANCELLED == voteVS?.state) {
			voteVSMap.anulacionURL="${grailsApplication.config.grails.serverURL}/voteVSCanceller/voteVS/${voteVS.id}"
		}
		return voteVSMap
	}
 
	public Map getVoteVSCancellerMap(VoteVSCanceller anulador) {
		if(!anulador) return [:]
		Map anuladorMap = [id:anulador.id,
            voteVSURL:"${grailsApplication.config.grails.serverURL}/voteVS/${anulador.getVoteVS.id}",
			anuladorSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${anulador.messageSMIME.id}"]
		return anuladorMap
	}
}