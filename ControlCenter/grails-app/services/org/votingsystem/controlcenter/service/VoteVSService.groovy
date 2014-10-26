package org.votingsystem.controlcenter.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.MetaInfMsg
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.cert.X509Certificate
import static org.springframework.context.i18n.LocaleContextHolder.*

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
	
	
	public synchronized ResponseVS validateVote(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		EventVSElection eventVS = messageSMIMEReq.eventVS
		String msg
        VoteVS voteVS = messageSMIMEReq.getSMIME().getVoteVS()
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
                validTo:voteVS.getX509Certificate().getNotAfter()).save()
        String signedVoteDigest = messageSMIMEReq.getSMIME().getContentDigestStr()
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = eventVS.accessControlVS.name
        String subject = messageSource.getMessage('voteValidatedByAccessControlMsg', null, locale)
        messageSMIMEReq.getSMIME().setMessageID("${grailsApplication.config.grails.serverURL}/messageSMIME/${messageSMIMEReq.id}")

        SMIMEMessage smimeVoteValidation = signatureVSService.getSMIMEMultiSigned(
                fromUser, toUser, messageSMIMEReq.getSMIME(), subject)
        messageSMIMEReq.setType(TypeVS.CONTROL_CENTER_VALIDATED_VOTE).setSMIME(smimeVoteValidation).save()
        //byte[] encryptResponseBytes = encryptResponse.messageBytes
        //String encryptResponseStr = new String(encryptResponseBytes)
        //log.debug(" - encryptResponseStr: ${encryptResponseStr}")
        ResponseVS responseVS = HttpHelper.getInstance().sendData(messageSMIMEReq.content,
                ContentTypeVS.VOTE, eventVS.accessControlVS.getVoteServiceURL())
        if (ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS(messageSource.getMessage(
                'accessRequestVoteErrorMsg', [responseVS.message].toArray(), locale))
        //ResponseVS validatedVoteResponse = signatureVSService.decryptSMIME(responseVS.messageBytes)
        //SMIMEMessage smimeMessageResp = validatedVoteResponse.getSMIME();
        SMIMEMessage smimeMessageResp = new SMIMEMessage(new ByteArrayInputStream(responseVS.messageBytes))
        if(!smimeMessageResp.getContentDigestStr().equals(signedVoteDigest)) {
            log.error("validateVote - ERROR digest sent: " + signedVoteDigest +
                    " - digest received: " + smimeMessageResp.getContentDigestStr())
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.VOTE_ERROR,
                    eventVS:eventVS, message:messageSource. getMessage('voteContentErrorMsg', null, locale))
        }
        responseVS = signatureVSService.validateVoteCerts(smimeMessageResp, eventVS)
        if(ResponseVS.SC_OK != responseVS.statusCode) {
            log.error("validateVote - validateVoteValidationCerts ERROR - > ${responseVS.message}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS, message:responseVS.message)
        }
        messageSMIMEReq.setSMIME(smimeMessageResp).setType(TypeVS.ACCESS_CONTROL_VALIDATED_VOTE).save()

        new VoteVS(optionSelected:optionSelected, eventVS:eventVS, state:VoteVS.State.OK,
                certificateVS:certificateVS, messageSMIME:messageSMIMEReq).save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ACCESS_CONTROL_VALIDATED_VOTE,
                eventVS:eventVS, contentType: ContentTypeVS.VOTE, messageSMIME: messageSMIMEReq,
                url:"${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${messageSMIMEReq.id}" )
	}
	
	public synchronized ResponseVS processCancel (MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
		SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
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
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned(
                fromUser, toUser, smimeMessageReq, subject)
        messageSMIMEReq.setSMIME(smimeMessageResp)
        VoteVSCanceller voteVSCanceller = new VoteVSCanceller(voteVS:voteVS, certificateVS:certificateVS,
                eventVSElection:eventVS, state:VoteVSCanceller.State.CANCELLATION_OK, messageSMIME:messageSMIMEReq,
                originHashCertVSBase64:originHashCertVote, hashCertVSBase64:hashCertVSBase64).save()
        if (!voteVSCanceller) { voteVSCanceller.errors.each { log.error("processCancel - ${it}") } }
        log.debug("$methodName - voteVSCanceller.id: ${voteVSCanceller.id}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS,
                type:TypeVS.CANCEL_VOTE, contentType: ContentTypeVS.JSON_SIGNED, messageSMIME: messageSMIMEReq)
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