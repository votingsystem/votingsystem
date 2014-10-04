package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.NifUtils

import java.security.cert.X509Certificate

@Transactional
class RepresentativeDelegationService {
	
	enum State {WITHOUT_ACCESS_REQUEST, WITH_ACCESS_REQUEST, WITH_VOTE}
	
	def subscriptionVSService
	def messageSource
	def grailsApplication
	def mailSenderService
	def signatureVSService
	def eventVSService
	def sessionFactory
	LinkGenerator grailsLinkGenerator

	
	//{"operation":"REPRESENTATIVE_SELECTION","representativeNif":"...","representativeName":"...","UUID":"..."}
	public synchronized ResponseVS saveDelegation(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug("saveDelegation")
		//def future = callAsync {}
		//return future.get(30, TimeUnit.SECONDS)
		MessageSMIME messageSMIME = null
		SMIMEMessage smimeMessage = messageSMIMEReq.getSmimeMessage()
		RepresentationDocumentVS representationDocument = null
		UserVS userVS = messageSMIMEReq.getUserVS()
		String msg = null
        ResponseVS responseVS = checkUserDelegationStatus(userVS, locale)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        def messageJSON = JSON.parse(smimeMessage.getSignedContent())
        String requestValidatedNIF =  NifUtils.validate(messageJSON.representativeNif)
        if(userVS.nif == requestValidatedNIF) throw new ExceptionVS(messageSource.getMessage('representativeSameUserNifErrorMsg',
                [requestValidatedNIF].toArray(), locale))
        if(!requestValidatedNIF || !messageJSON.operation || !messageJSON.representativeNif ||
                (TypeVS.REPRESENTATIVE_SELECTION != TypeVS.valueOf(messageJSON.operation))) {
            throw new ExceptionVS(messageSource.getMessage('representativeSelectionDataErrorMsg', null, locale))
        }
        UserVS representative = UserVS.findWhere(nif:requestValidatedNIF, type:UserVS.Type.REPRESENTATIVE)
        if(!representative)  throw new ExceptionVS(
                messageSource.getMessage('representativeNifErrorMsg', [requestValidatedNIF].toArray(), locale))
        userVS.representative = representative
        RepresentationDocumentVS.withTransaction {
            representationDocument = RepresentationDocumentVS.findWhere(
                    userVS:userVS, state:RepresentationDocumentVS.State.OK)
            if(representationDocument) {
                representationDocument.state = RepresentationDocumentVS.State.CANCELLED
                representationDocument.cancellationSMIME = messageSMIMEReq
                representationDocument.dateCanceled = userVS.getTimeStampToken().getTimeStampInfo().getGenTime();
                representationDocument.save(flush:true)
                log.debug("saveDelegation - cancelled representationDocument ${representationDocument.id} " +
                        "by user '${userVS.nif}'")
            } else log.debug("saveDelegation - user '${userVS.nif}' firs public delegation")
            representationDocument = new RepresentationDocumentVS(activationSMIME:messageSMIMEReq,
                    userVS:userVS, representative:representative, state:RepresentationDocumentVS.State.OK).save()
        }

        msg = messageSource.getMessage('representativeAssociatedMsg',
                [messageJSON.representativeName, userVS.nif].toArray(), locale)
        log.debug "saveDelegation - ${msg}"

        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = userVS.getNif()
        String subject = messageSource.getMessage('representativeSelectValidationSubject', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
                fromUser, toUser, smimeMessage, subject)
        MessageSMIME messageSMIMEResp = new MessageSMIME( smimeMessage:smimeMessageResp,
                type:TypeVS.RECEIPT, smimeParent: messageSMIMEReq, content:smimeMessageResp.getBytes()).save();
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, data:messageSMIMEResp,
                type:TypeVS.REPRESENTATIVE_SELECTION)
	}

    private ResponseVS checkUserDelegationStatus(UserVS userVS, Locale locale) {
        String msg = null
        if(UserVS.Type.REPRESENTATIVE == userVS.type) {
            msg = messageSource.getMessage('userIsRepresentativeErrorMsg', [userVS.nif].toArray(), locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        int statusCode = ResponseVS.SC_OK
        String userDelegationURL = null
        AnonymousDelegation anonymousDelegation = AnonymousDelegation.findWhere(userVS:userVS,
                status:AnonymousDelegation.Status.OK)
        if(anonymousDelegation && Calendar.getInstance().getTime().after(anonymousDelegation.getDateTo())) {
            anonymousDelegation.setStatus(AnonymousDelegation.Status.FINISHED)
        }
        if(anonymousDelegation && AnonymousDelegation.Status.OK == anonymousDelegation.getStatus()) {
            statusCode = ResponseVS.SC_ERROR_REQUEST_REPEATED
            msg = messageSource.getMessage('userWithPreviousDelegationErrorMsg' ,[userVS.nif,
                    anonymousDelegation.getDateTo().format("dd/MMM/yyyy' 'HH:mm")].toArray(), locale)
            userDelegationURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${anonymousDelegation.delegationSMIME.id}"
        }
        Map responseDataMap = [message:msg, URL:userDelegationURL, statusCode:ResponseVS.SC_ERROR_REQUEST_REPEATED]
        return new ResponseVS(statusCode:statusCode,data:responseDataMap, message: msg, contentType:ContentTypeVS.JSON);
    }

    ResponseVS validateAnonymousRequest(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS userVS = messageSMIMEReq.getUserVS()
        String msg
        ResponseVS responseVS = checkUserDelegationStatus(userVS, locale)
        if(ResponseVS.SC_OK != responseVS.statusCode) {
            log.error("$methodName - ${responseVS.message}")
            responseVS.metaInf = MetaInfMsg.getErrorMsg(methodName, "delegationStatusError")
            responseVS.reason = responseVS.message
            return responseVS
        }
        def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
        if (!messageJSON.accessControlURL || !messageJSON.weeksOperationActive ||
                (TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('requestWithErrorsMsg', null, locale)
            log.error("$methodName - msg: ${msg} - ${messageJSON}")
            return new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST, reason: msg,
                    metaInf: MetaInfMsg.getErrorMsg(methodName, "paramsError"), type:TypeVS.ERROR,
                    contentType:ContentTypeVS.JSON, data:[statusCode: ResponseVS.SC_ERROR_REQUEST, message:msg])
        }
        messageSMIMEReq.setType(TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST);
        Calendar calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Date dateFrom = DateUtils.getMonday(calendar).getTime()
        calendar.add(Calendar.DAY_OF_YEAR, 7 * Integer.valueOf(messageJSON.weeksOperationActive));
        Date dateTo = calendar.getTime()
        msg = messageSource.getMessage('anonymousDelegationRangeMsg', [userVS.getNif(),
                   DateUtils.getDateStr(dateFrom), DateUtils.getDateStr(dateTo)].toArray(), locale)
        log.debug("$methodName - ${msg}")
        AnonymousDelegation anonymousDelegation = new AnonymousDelegation(status:AnonymousDelegation.Status.OK,
                delegationSMIME:messageSMIMEReq, userVS:userVS, dateFrom:dateFrom, dateTo:dateTo).save();
        return new ResponseVS(statusCode: ResponseVS.SC_OK, type: TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST,
                userVS:userVS)
    }

    @Transactional
    public ResponseVS saveAnonymousDelegation(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName)
        MessageSMIME messageSMIME = null
        SMIMEMessage smimeMessage = messageSMIMEReq.getSmimeMessage()
        X509Certificate x509UserCert =  messageSMIMEReq.getAnonymousSigner().getCertificate()
        String msg = null
        CertificateVS certificateVS = CertificateVS.findWhere(serialNumber:x509UserCert.serialNumber.longValue(),
                type: CertificateVS.Type.ANONYMOUS_REPRESENTATIVE_DELEGATION, state: CertificateVS.State.OK)
        if(!certificateVS) throw new ExceptionVS(messageSource.getMessage('certificateVSUnknownErrorMsg' , null, locale))
        def messageJSON = JSON.parse(smimeMessage.getSignedContent())
        String requestValidatedNIF =  NifUtils.validate(messageJSON.representativeNif)
        if(!requestValidatedNIF || !messageJSON.operation || !messageJSON.representativeNif ||
                (TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION != TypeVS.valueOf(messageJSON.operation))) {
            throw new ExceptionVS(messageSource.getMessage('representativeSelectionDataErrorMsg', null, locale))
        }
        UserVS representative = UserVS.findWhere(nif:requestValidatedNIF, type:UserVS.Type.REPRESENTATIVE)
        if(!representative) throw new ExceptionVS(
                messageSource.getMessage('representativeNifErrorMsg', [requestValidatedNIF].toArray(), locale))

        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = certificateVS.hashCertVSBase64
        String subject = messageSource.getMessage('representativeSelectValidationSubject', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
                fromUser, toUser, smimeMessage, subject)
        MessageSMIME messageSMIMEResp = new MessageSMIME(smimeMessage:smimeMessageResp,
                type:TypeVS.RECEIPT, smimeParent: messageSMIMEReq, content:smimeMessageResp.getBytes()).save()
        certificateVS.state = CertificateVS.State.USED
        certificateVS.messageSMIME = messageSMIMEResp
        certificateVS.save()
        RepresentationDocumentVS representationDocument =new RepresentationDocumentVS(representative:representative,
                activationSMIME:messageSMIMEResp, state:RepresentationDocumentVS.State.OK).save();
        msg = messageSource.getMessage('anonymousRepresentativeAssociatedMsg',
                [messageJSON.representativeName].toArray(), locale)
        log.debug "$methodName - representationDocument: ${representationDocument.id}"
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, data:messageSMIMEResp,
                type:TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION, contentType: ContentTypeVS.JSON_SIGNED)
    }

    public ResponseVS cancelAnonymousDelegation(MessageSMIME messageSMIMEReq, Locale locale) {
        SMIMEMessage smimeMessage = messageSMIMEReq.getSmimeMessage()
        UserVS userVS = messageSMIMEReq.getUserVS()
        String msg
        MessageSMIME userDelegation = MessageSMIME.findWhere(type:TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION,
                userVS:userVS)
        if(!userDelegation) {
            return new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST, message:messageSource.
                    getMessage('userWithoutAnonymousDelegationErrorMsg',
                    [userVS.nif].toArray(), locale))
        }
        def messageJSON = JSON.parse(smimeMessage.getSignedContent())
        throw new ExceptionVS(" === TODO ====")
    }

	private void cancelRepresentationDocument(MessageSMIME messageSMIMEReq, UserVS userVS) {
		RepresentationDocumentVS.withTransaction {
			RepresentationDocumentVS representationDocument = RepresentationDocumentVS.
				findWhere(userVS:userVS, state:RepresentationDocumentVS.State.OK)
			if(representationDocument) {
				log.debug("cancelRepresentationDocument - User changing representative")
				representationDocument.state = RepresentationDocumentVS.State.CANCELLED
				representationDocument.cancellationSMIME = messageSMIMEReq
				representationDocument.dateCanceled = userVS.getTimeStampToken().getTimeStampInfo().getGenTime();
				representationDocument.save(flush:true)
				log.debug("cancelRepresentationDocument - user '${userVS.nif}' " +
                        " - representationDocument ${representationDocument.id}")
			} else log.debug("cancelRepresentationDocument - user '${userVS.nif}' doesn't have representative")
		}
	}
	
	//{"operation":"REPRESENTATIVE_ACCREDITATIONS_REQUEST","representativeNif":"...",
	//"representativeName":"...","selectedDate":"2013-05-20 09:50:33","email":"...","UUID":"..."}
	ResponseVS processAccreditationsRequest(MessageSMIME messageSMIMEReq, Locale locale) {
		String msg = null
		SMIMEMessage smimeMessage = messageSMIMEReq.getSmimeMessage()
		UserVS userVS = messageSMIMEReq.getUserVS();
		log.debug("processAccreditationsRequest - userVS '{userVS.nif}'")
		RepresentationDocumentVS representationDocument = null
		def messageJSON = null
		try {
			messageJSON = JSON.parse(smimeMessage.getSignedContent())
			String requestValidatedNIF =  NifUtils.validate(messageJSON.representativeNif)
			Date selectedDate = DateUtils.getDateFromString(messageJSON.selectedDate)
			if(!requestValidatedNIF || !messageJSON.operation || 
				(TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST != TypeVS.valueOf(messageJSON.operation))||
				!selectedDate || !messageJSON.email || !messageJSON.UUID ){
				msg = messageSource.getMessage('representativeAccreditationRequestErrorMsg', null, locale)
				log.error "processAccreditationsRequest - ERROR DATA - ${msg} - ${messageJSON.toString()}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
					type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
			}
			UserVS representative = UserVS.findWhere(nif:requestValidatedNIF, type:UserVS.Type.REPRESENTATIVE)
			if(!representative) {
			   msg = messageSource.getMessage('representativeNifErrorMsg', [requestValidatedNIF].toArray(), locale)
			   log.error "processAccreditationsRequest - ERROR REPRESENTATIVE - ${msg}"
			   return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
				   type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
		   }
			runAsync {
					ResponseVS backupGenResponseVS = getAccreditationsBackup(
						representative, selectedDate ,locale)
					if(ResponseVS.SC_OK == backupGenResponseVS?.statusCode) {
						File archivoCopias = backupGenResponseVS.file
						BackupRequestVS solicitudCopia = new BackupRequestVS(
							filePath:archivoCopias.getAbsolutePath(),
							type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
							representative:representative,
							messageSMIME:messageSMIMEReq, email:messageJSON.email)
						BackupRequestVS.withTransaction {
							if (!solicitudCopia.save()) {
								solicitudCopia.errors.each {
									log.error("processAccreditationsRequest - ERROR solicitudCopia - ${it}")}
							}
						}
						log.debug("processAccreditationsRequest - saved BackupRequestVS '${solicitudCopia.id}'");
						mailSenderService.sendRepresentativeAccreditations(
							solicitudCopia, messageJSON.selectedDate, locale)
					} else log.error("processAccreditationsRequest - ERROR creating backup");
			}
			msg = messageSource.getMessage('backupRequestOKMsg', [messageJSON.email].toArray(), locale)
			new ResponseVS(statusCode:ResponseVS.SC_OK,	message:msg, type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage('representativeAccreditationRequestErrorMsg', null, locale)
			return new ResponseVS(message:msg, statusCode:ResponseVS.SC_ERROR,
				type:TypeVS.REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR)
		}
	}

}