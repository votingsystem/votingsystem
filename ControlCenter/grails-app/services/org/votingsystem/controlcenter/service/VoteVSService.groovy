package org.votingsystem.controlcenter.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.MetaInfMsg

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
            messageSMIMEReq.setSmimeMessage(smimeMessageResp)
            messageSMIMEReq.save()

            new VoteVS(optionSelected:optionSelected, eventVS:eventVS, state:VoteVS.State.OK,
                    certificateVS:certificateVS, messageSMIME:messageSMIMEReq).save()

            String voteURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${messageSMIMEReq.id}"
            Map modelData = [voteURL:voteURL, receiverCert: responseReceiverCert,
                     responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ACCESS_CONTROL_VALIDATED_VOTE,
                     eventVS:eventVS, contentType: ContentTypeVS.VOTE, data:messageSMIMEReq)]

            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:modelData)
        } else {
            msg = messageSource.getMessage('accessRequestVoteErrorMsg', [responseVS.message].toArray(), locale)
            log.error("validateVote - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.VOTE_ERROR, message:msg)
        }
	}
	
	public synchronized ResponseVS processCancel (MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        def cancelDataJSON = JSON.parse(smimeMessageReq.getSignedContent())
        def originHashCertVote = cancelDataJSON.originHashCertVote
        def hashCertVSBase64 = cancelDataJSON.hashCertVSBase64
        def hashCertVoteVS = CMSUtils.getHashBase64(originHashCertVote, ContextVS.VOTING_DATA_DIGEST)
        if (!hashCertVSBase64.equals(hashCertVoteVS)) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                message:messageSource.getMessage('voteCancellationHashCertificateError', null, locale),
                metaInf:MetaInfMsg.getErrorMsg(methodName, "voteVSCancellationDataError"))
        def certificateVS = CertificateVS.findWhere(hashCertVSBase64:hashCertVSBase64, state:CertificateVS.State.OK)
        if (!certificateVS) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.CANCEL_VOTE_ERROR,
                message:messageSource.getMessage('certNotFoundErrorMsg', null, locale),
                metaInf:MetaInfMsg.getErrorMsg(methodName, "voteVSCancellationCertificateVSMissing"))
        def voteVS = VoteVS.findWhere(certificateVS:certificateVS, state:VoteVS.State.OK)
        if(!voteVS) throw new ExceptionVS("VoteVS not found")
        EventVS eventVS = voteVS.eventVS
        voteVS.state = VoteVS.State.CANCELLED
        voteVS.save()
        certificateVS.state = CertificateVS.State.CANCELLED
        certificateVS.save()
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = messageSMIMEReq.getUserVS()?.getNif()
        String subject = messageSource.getMessage('mime.subject.voteCancellationValidated', null, locale)
        SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
                fromUser, toUser, smimeMessageReq, subject)
        MessageSMIME messageSMIMEResp = new MessageSMIME(content:smimeMessageResp.getBytes(),
                smimeParent:messageSMIMEReq, eventVS:eventVS, type:TypeVS.RECEIPT).save()
        if (!messageSMIMEResp) { messageSMIMEResp.errors.each { log.error("processCancel - ${it}") } }
        VoteVSCanceller voteVSCanceller = new VoteVSCanceller(voteVS:voteVS, certificateVS:certificateVS,
                eventVSElection:eventVS, state:VoteVSCanceller.State.CANCELLATION_OK, messageSMIME:messageSMIMEResp,
                originHashCertVSBase64:originHashCertVote, hashCertVSBase64:hashCertVSBase64).save()
        if (!voteVSCanceller) { voteVSCanceller.errors.each { log.error("processCancel - ${it}") } }
        log.debug("$methodName - voteVSCanceller.id: ${voteVSCanceller.id}")
        Map modelData = [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS,
                type:TypeVS.CANCEL_VOTE, contentType: ContentTypeVS.JSON_SIGNED, data:messageSMIMEResp)]
        return new ResponseVS(statusCode:ResponseVS.SC_OK, data:modelData)
    }
			
	public Map getVoteVSMap(VoteVS voteVS) {
		if(!voteVS) throw new ExceptionVS("VoteVS null")
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
 
	public Map getVoteVSCancellerMap(VoteVSCanceller canceller) {
		if(!canceller) throw new ExceptionVS("VoteVSCanceller null")
		Map cancellerMap = [id:canceller.id,
            voteVSURL:"${grailsApplication.config.grails.serverURL}/voteVS/${canceller.getVoteVS.id}",
			cancellerSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${canceller.messageSMIME.id}"]
		return cancellerMap
	}
}