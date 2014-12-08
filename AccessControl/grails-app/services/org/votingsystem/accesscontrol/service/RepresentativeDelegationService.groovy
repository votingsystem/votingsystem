package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONObject
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.throwable.ValidationExceptionVS

import static org.springframework.context.i18n.LocaleContextHolder.*
import org.votingsystem.model.*
import org.votingsystem.model.RepresentationState
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.NifUtils

import java.security.cert.X509Certificate

//@Transactional
class RepresentativeDelegationService {
	
	enum State {WITHOUT_ACCESS_REQUEST, WITH_ACCESS_REQUEST, WITH_VOTE}
	
	def subscriptionVSService
	def messageSource
	def grailsApplication
	def mailSenderService
	def signatureVSService
	def eventVSService
    def csrService
	LinkGenerator grailsLinkGenerator

	//{"operation":"REPRESENTATIVE_SELECTION","representativeNif":"...","representativeName":"...","UUID":"..."}
	public synchronized ResponseVS saveDelegation(MessageSMIME messageSMIMEReq) {
		//def future = callAsync {}
		//return future.get(30, TimeUnit.SECONDS)
		SMIMEMessage smimeMessage = messageSMIMEReq.getSMIME()
		UserVS userVS = messageSMIMEReq.getUserVS()
        ResponseVS responseVS = checkUserDelegationStatus(userVS)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        JSONObject messageJSON = JSON.parse(smimeMessage.getSignedContent())
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
        cancelPublicDelegation(messageSMIMEReq);
        userVS.representative = representative
        RepresentationDocumentVS representationDocument = new RepresentationDocumentVS(activationSMIME:messageSMIMEReq,
                userVS:userVS, representative:representative, state:RepresentationDocumentVS.State.OK).save()
        String msg = messageSource.getMessage('representativeAssociatedMsg',
                [messageJSON.representativeName, userVS.nif].toArray(), locale)
        String toUser = userVS.getNif()
        String subject = messageSource.getMessage('representativeSelectValidationSubject', null, locale)
        messageSMIMEReq.setSMIME(signatureVSService.getSMIMEMultiSigned(toUser, smimeMessage, subject))
        log.debug("saveDelegation - user '${userVS.nif}' - representationDocument.id: '${representationDocument.id}'")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, data:messageSMIMEReq,
                type:TypeVS.REPRESENTATIVE_SELECTION)
	}

    @Transactional
    private void cancelPublicDelegation(MessageSMIME messageSMIMEReq) {
        RepresentationDocumentVS representationDocument = RepresentationDocumentVS.findWhere(
                userVS:messageSMIMEReq.getUserVS(), state:RepresentationDocumentVS.State.OK)
        if(representationDocument) {
            representationDocument.dateCanceled = messageSMIMEReq.getUserVS().getTimeStampToken().
                    getTimeStampInfo().getGenTime();
            representationDocument.setState(RepresentationDocumentVS.State.CANCELLED).setCancellationSMIME(
                    messageSMIMEReq).save()
            log.debug("cancelPublicDelegation - cancelled representationDocument ${representationDocument.id} " +
                    "by user '${messageSMIMEReq.getUserVS().nif}'")
        } else log.debug("cancelPublicDelegation - user '${messageSMIMEReq.getUserVS().nif}' has no public delegations")
    }

    private ResponseVS checkUserDelegationStatus(UserVS userVS) {
        String msg = null
        if(UserVS.Type.REPRESENTATIVE == userVS.type) {
            msg = messageSource.getMessage('userIsRepresentativeErrorMsg', [userVS.nif].toArray(), locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        int statusCode = ResponseVS.SC_OK
        String userDelegationURL = null
        AnonymousDelegation anonymousDelegation = getAnonymousDelegation(userVS)
        if(anonymousDelegation) {
            statusCode = ResponseVS.SC_ERROR_REQUEST_REPEATED
            msg = messageSource.getMessage('userWithPreviousDelegationErrorMsg' ,[userVS.nif,
                    anonymousDelegation.getDateTo().format("dd/MMM/yyyy' 'HH:mm")].toArray(), locale)
            userDelegationURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${anonymousDelegation.delegationSMIME.id}"
        }
        Map responseDataMap = [message:msg, URL:userDelegationURL, statusCode:ResponseVS.SC_ERROR_REQUEST_REPEATED]
        return new ResponseVS(statusCode:statusCode,data:responseDataMap, message: msg, contentType:ContentTypeVS.JSON);
    }

    @Transactional public AnonymousDelegation getAnonymousDelegation(UserVS userVS) {
        AnonymousDelegation anonymousDelegation = AnonymousDelegation.findWhere(userVS:userVS,
                status:AnonymousDelegation.Status.OK)
        if(anonymousDelegation && Calendar.getInstance().getTime().after(anonymousDelegation.getDateTo())) {
            anonymousDelegation.setStatus(AnonymousDelegation.Status.FINISHED)
            anonymousDelegation.save()
            return null
        }
        return anonymousDelegation
    }

    @Transactional public Map checkRepresentationState(String nifToCheck) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        nifToCheck = NifUtils.validate(nifToCheck)
        UserVS userVS  = UserVS.findWhere(nif:nifToCheck)
        if(!userVS) throw new ExceptionVS(messageSource.getMessage('userVSNotFoundByNIF', [nifToCheck].toArray(), locale))
        if(userVS.representative) {
            RepresentationDocumentVS representationDocument = RepresentationDocumentVS.findWhere(
                    userVS:userVS, state:RepresentationDocumentVS.State.OK)
            return [state:RepresentationState.WITH_PUBLIC_REPRESENTATION.toString(),
                    lastCheckedDate:DateUtils.getDateStr(Calendar.getInstance().getTime()),
                    base64ContentDigest:representationDocument.activationSMIME.base64ContentDigest,
                    representative:((RepresentativeService)grailsApplication.mainContext.getBean(
                    "representativeService")).getRepresentativeDetailedMap(userVS.representative)]
        }
        if(UserVS.Type.REPRESENTATIVE == userVS.type) {
            return [state:RepresentationState.REPRESENTATIVE.toString(),
                    lastCheckedDate:DateUtils.getDateStr(Calendar.getInstance().getTime()),
                    representative: ((RepresentativeService)grailsApplication.mainContext.getBean(
                            "representativeService")).getRepresentativeDetailedMap(userVS)]
        }
        AnonymousDelegation anonymousDelegation = getAnonymousDelegation(userVS)
        if(anonymousDelegation) {
            return [state:RepresentationState.WITH_ANONYMOUS_REPRESENTATION.toString(),
                    lastCheckedDate:DateUtils.getDateStr(Calendar.getInstance().getTime()),
                    base64ContentDigest:anonymousDelegation.delegationSMIME.base64ContentDigest,
                    dateFrom: DateUtils.getDateStr(anonymousDelegation.getDateFrom()),
                    dateTo: DateUtils.getDateStr(anonymousDelegation.getDateTo())]
        }
        return [state:org.votingsystem.model.RepresentationState.WITHOUT_REPRESENTATION.toString(),
                lastCheckedDate:DateUtils.getDateStr(Calendar.getInstance().getTime()),
                base64ContentDigest:""]
    }

    @Transactional
    ResponseVS validateAnonymousRequest(MessageSMIME messageSMIMEReq, byte[] csrRequest) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
        UserVS userVS = messageSMIMEReq.getUserVS()
        ResponseVS responseVS = checkUserDelegationStatus(userVS)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS.setMetaInf(MetaInfMsg.getErrorMsg(methodName,
                "delegationStatusError")).setReason(responseVS.message)
        AnonymousDelegationRequest request = new AnonymousDelegationRequest(
                smimeMessageReq.getSignedContent()).validateRequest()
        cancelPublicDelegation(messageSMIMEReq)
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned(userVS.getNif(), smimeMessageReq, null)
        messageSMIMEReq.setType(request.operation).setSMIME(smimeMessageResp)
        responseVS = csrService.signAnonymousDelegationCert(csrRequest)
        if(ResponseVS.SC_OK == responseVS.statusCode) {
            userVS.representative = null
            new AnonymousDelegation(status:AnonymousDelegation.Status.OK, delegationSMIME:messageSMIMEReq,
                    userVS:userVS, dateFrom:request.dateFrom, dateTo:request.dateTo).save();
        }
        return responseVS
    }

    @Transactional
    public ResponseVS saveAnonymousDelegation(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessage = messageSMIMEReq.getSMIME()
        X509Certificate x509UserCert =  messageSMIMEReq.getAnonymousSigner().getCertificate()
        JSONObject certExtensionData = CertUtils.getCertExtensionData(x509UserCert,
                ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_OID)
        CertificateVS certificateVS = CertificateVS.findWhere(serialNumber:x509UserCert.serialNumber.longValue(),
                type: CertificateVS.Type.ANONYMOUS_REPRESENTATIVE_DELEGATION, state: CertificateVS.State.OK,
                hashCertVSBase64:certExtensionData.hashCertVS)
        if(!certificateVS) throw new ExceptionVS(messageSource.getMessage('certificateVSUnknownErrorMsg', null, locale))
        AnonymousDelegationRequest request = new AnonymousDelegationRequest(
                smimeMessage.getSignedContent()).validateDelegation()
        String toUser = certificateVS.hashCertVSBase64
        String subject = messageSource.getMessage('representativeSelectValidationSubject', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned(toUser, smimeMessage, subject)
        messageSMIMEReq.setSMIME(smimeMessageResp)
        certificateVS.setState(CertificateVS.State.USED).setMessageSMIME(messageSMIMEReq).save()
        RepresentationDocumentVS representationDocument = new RepresentationDocumentVS(representative:
                request.representative, activationSMIME:messageSMIMEReq, state:RepresentationDocumentVS.State.OK).save();
        String msg = messageSource.getMessage('anonymousRepresentativeAssociatedMsg',
                [request.representativeName].toArray(), locale)
        log.debug "$methodName - representationDocument: ${representationDocument.id}"
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, messageSMIME: messageSMIMEReq,
                type:TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION, contentType: ContentTypeVS.JSON_SIGNED)
    }

    @Transactional
    public ResponseVS cancelAnonymousDelegation(MessageSMIME messageSMIMEReq) {
        SMIMEMessage smimeMessage = messageSMIMEReq.getSMIME()
        UserVS userVS = messageSMIMEReq.getUserVS()
        AnonymousDelegationRequest request = new AnonymousDelegationRequest(
                smimeMessage.getSignedContent()).validateAnonymousDelegationCancellation(userVS)
        request.anonymousDelegation.setStatus(AnonymousDelegation.Status.CANCELLED).setCancellationSMIME(
                messageSMIMEReq).setDateCancelled(Calendar.getInstance().getTime()).save()
        request.representationDocumentVS.setCancellationSMIME(messageSMIMEReq).save()
        request.anonymousDelegation.setStatus(AnonymousDelegation.Status.CANCELLED).setCancellationSMIME(
                messageSMIMEReq).save()
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned(userVS.getNif(), smimeMessage, null)
        messageSMIMEReq.setSMIME(smimeMessageResp)
        return new ResponseVS(statusCode:ResponseVS.SC_OK, messageSMIME: messageSMIMEReq, userVS:userVS,
                type:request.operation, contentType: ContentTypeVS.JSON_SIGNED)
    }

	private void cancelRepresentationDocument(MessageSMIME messageSMIMEReq, UserVS userVS) {
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
	
	//{"operation":"REPRESENTATIVE_ACCREDITATIONS_REQUEST","representativeNif":"...",
	//"representativeName":"...","selectedDate":"2013-05-20 09:50:33","email":"...","UUID":"..."}
	ResponseVS processAccreditationsRequest(MessageSMIME messageSMIMEReq) {
		String msg = null
		SMIMEMessage smimeMessage = messageSMIMEReq.getSMIME()
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
                //to avoid circular references
                ResponseVS backupGenResponseVS = ((RepresentativeService)grailsApplication.mainContext.getBean(
                        "representativeService")).getAccreditationsBackup(representative, selectedDate)
                if(ResponseVS.SC_OK == backupGenResponseVS?.statusCode) {
                    File backupFile = backupGenResponseVS.file
                    BackupRequestVS backupRequest = new BackupRequestVS(filePath:backupFile.getAbsolutePath(),
                        type:TypeVS.REPRESENTATIVE_VOTING_HISTORY_REQUEST, representative:representative,
                        messageSMIME:messageSMIMEReq, email:messageJSON.email)
                    BackupRequestVS.withTransaction {
                        if (!backupRequest.save()) {
                            backupRequest.errors.each {
                                log.error("processAccreditationsRequest - ERROR backupRequest - ${it}")}
                        }
                    }
                    log.debug("processAccreditationsRequest - saved BackupRequestVS '${backupRequest.id}'");
                    mailSenderService.sendRepresentativeAccreditations(backupRequest, messageJSON.selectedDate)
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

    private class AnonymousDelegationRequest {
        String representativeNif, representativeName, accessControlURL, hashCertVSBase64, originHashCertVSBase64;
        Integer weeksOperationActive;
        Date dateFrom, dateTo;
        TypeVS operation;
        JSONObject messageJSON;
        UserVS representative
        AnonymousDelegation anonymousDelegation
        CertificateVS certificateVS
        RepresentationDocumentVS representationDocumentVS

        public AnonymousDelegationRequest(String signedContent) throws ExceptionVS {
            messageJSON = JSON.parse(signedContent)
            operation = TypeVS.valueOf(messageJSON.operation)
            accessControlURL = messageJSON.accessControlURL;
            weeksOperationActive = Integer.valueOf(messageJSON.weeksOperationActive)
            if(!weeksOperationActive) throw new ValidationExceptionVS(this.getClass(), "missing param 'weeksOperationActive'")
            dateFrom = DateUtils.getDateFromString(messageJSON.dateFrom)
            if(!dateFrom) throw new ValidationExceptionVS(this.getClass(), "missing param 'dateFrom'")
            dateTo = DateUtils.getDateFromString(messageJSON.dateTo)
            if(!dateTo) throw new ValidationExceptionVS(this.getClass(), "missing param 'dateTo'")
            if(dateFrom.after(dateTo)) throw new ValidationExceptionVS(this.getClass(), "'dateFrom' after 'dateTo'")
            Date dateFromCheck = DateUtils.getMonday(DateUtils.addDays(7)).getTime();//Next week Monday
            if(dateFrom.compareTo(dateFromCheck) != 0) throw new ValidationExceptionVS(this.getClass(),
                    "dateFrom expected '${dateFromCheck}' received '${dateFrom}'")
            Date dateToCheck = DateUtils.addDays(dateFromCheck, weeksOperationActive * 7).getTime();
            if(dateTo.compareTo(dateToCheck) != 0) throw new ValidationExceptionVS(this.getClass(),
                    "dateTo expected '${dateToCheck}' received '${dateTo}'")
        }

        public AnonymousDelegationRequest validateDelegationData() throws ExceptionVS {
            representativeNif = NifUtils.validate(messageJSON.representativeNif);
            if(!representativeNif) throw new ValidationExceptionVS(this.getClass(), "missing param 'representativeNif'")
            representativeName = messageJSON.representativeName
            if(!representativeName) throw new ValidationExceptionVS(this.getClass(), "missing param 'representativeName'")
            representative = UserVS.findWhere(nif:representativeNif, type:UserVS.Type.REPRESENTATIVE)
            if(!representative) throw new ValidationExceptionVS(this.getClass(),
                    messageSource.getMessage('representativeNifErrorMsg', [representativeNif].toArray(), locale))
            return this;
        }

        public AnonymousDelegationRequest validateRequest() throws ExceptionVS {
            if(TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST != operation) throw new ValidationExceptionVS(this.getClass(),
                    "expected operation 'ANONYMOUS_REPRESENTATIVE_REQUEST' but found '" + operation.toString() + "'")
            return this;
        }
        public AnonymousDelegationRequest validateDelegation() throws ExceptionVS {
            if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION != operation) throw new ValidationExceptionVS(this.getClass(),
                    "expected operation 'ANONYMOUS_REPRESENTATIVE_SELECTION' but found '" + operation.toString() + "'")
            validateDelegationData()
            return this;
        }

        public AnonymousDelegationRequest validateAnonymousDelegationCancellation(UserVS userVS) {
            if(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED != operation) throw new ValidationExceptionVS(this.getClass(),
                    "expected operation 'ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED' but found '" + operation.toString() + "'")
            validateDelegationData()
            anonymousDelegation = getAnonymousDelegation(userVS)
            if(!anonymousDelegation)  throw new ValidationExceptionVS(this.getClass(), message:messageSource.
                    getMessage('userWithoutAnonymousDelegationErrorMsg', [userVS.nif].toArray(), locale))
            hashCertVSBase64 =  messageJSON.hashCertVSBase64
            if(!hashCertVSBase64) throw new ValidationExceptionVS(this.getClass(), "missing param 'hashCertVSBase64'")
            originHashCertVSBase64 = messageJSON.originHashCertVSBase64
            if(!originHashCertVSBase64) throw new ValidationExceptionVS(this.getClass(), "missing param 'originHashCertVSBase64'")
            if(!hashCertVSBase64.equals(CMSUtils.getHashBase64(originHashCertVSBase64,ContextVS.VOTING_DATA_DIGEST)))
                    throw new ValidationExceptionVS(this.getClass(), "provided origin doesn't match hash origin")
            certificateVS = CertificateVS.findWhere(state:CertificateVS.State.USED, hashCertVSBase64:hashCertVSBase64)
            if(!certificateVS) throw new ValidationExceptionVS(this.getClass(), "data doesn't macth param CertificateVS")
            representationDocumentVS = RepresentationDocumentVS.findWhere(activationSMIME:certificateVS.messageSMIME)
            if(!representationDocumentVS) throw new ValidationExceptionVS(this.getClass(),
                    "data doesn't macth param RepresentationDocumentVS")
            return this;
        }
    }

}