package org.votingsystem.vicket.service

import grails.converters.JSON
import org.springframework.context.i18n.LocaleContextHolder
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.vicket.model.*
import org.votingsystem.vicket.util.LoggerVS
import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class VicketService {

    private static final CLASS_NAME = VicketService.class.getSimpleName()

    def messageSource
    def transactionVSService
    def grailsApplication
    def signatureVSService
    def userVSService
    def csrService
    def walletVSService
    def timeStampService
    def systemService

    public ResponseVS cancelVicket(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        def requestJSON = JSON.parse(smimeMessageReq.getSignedContent())
        if(TypeVS.VICKET_CANCEL != TypeVS.valueOf(requestJSON.operation))
                throw new ExceptionVS(messageSource.getMessage("operationMismatchErrorMsg",
                [TypeVS.VICKET_CANCEL.toString(),requestJSON.operation ].toArray(), locale))
        def hashCertVSBase64 = CMSUtils.getHashBase64(requestJSON.originHashCertVS, ContextVS.VOTING_DATA_DIGEST)
        if(!hashCertVSBase64.equals(requestJSON.hashCertVSBase64))
            throw new ExceptionVS(messageSource.getMessage("originHashErrorMsg", null, locale))
        Vicket vicket = Vicket.findWhere(hashCertVS: requestJSON.hashCertVSBase64,
                serialNumber:Long.valueOf(requestJSON.vicketCertSerialNumber))
        if(Vicket.State.OK == vicket.getState()) {
            String fromUser = grailsApplication.config.VotingSystem.serverName
            String toUser = smimeMessageReq.getFrom().toString()
            String subject = messageSource.getMessage('cancelVicketReceiptSubject', null, locale)
            vicket.setState(Vicket.State.CANCELLED)
            SMIMEMessage receipt = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                    smimeMessageReq, subject)
            MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                    content:receipt.getBytes()).save()
            vicket.cancelMessage = messageSMIMEReq
            vicket.save()
            TransactionVS transaction = new TransactionVS(amount: vicket.amount, messageSMIME:messageSMIMEReq,
                    subject:messageSource.getMessage('cancelVicketTransactionSubject', null, locale),
                    fromUserVS:signer, toUserVS:signer, state:TransactionVS.State.OK,
                    currency:vicket.currency, type:TransactionVS.Type.VICKET_CANCELLATION, validTo:vicket.validTo).save()
            log.debug("cancelVicket - model: ${vicket.id} - transactionVS: ${transaction.id}");
            return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON_SIGNED,
                    messageBytes: vicket.cancelMessage.content, type:TypeVS.VICKET_CANCEL)
        } else {
            log.error("$methodName - ERROR - request for cancel vicket: ${vicket.id} - with state: ${vicket.state}");
            byte[] messageBytes
            ContentTypeVS contentType = ContentTypeVS.TEXT
            int statusCode = ResponseVS.SC_ERROR_REQUEST
            if(Vicket.State.CANCELLED == vicket.getState()) {
                contentType = ContentTypeVS.JSON_SIGNED
                messageBytes = vicket.cancelMessage.content
            } else if(Vicket.State.EXPENDED == vicket.getState()) {
                contentType = ContentTypeVS.JSON_SIGNED
                messageBytes = vicket.messageSMIME.content
            }
            if(Vicket.State.LAPSED == vicket.getState()) {
                contentType = ContentTypeVS.TEXT
                messageBytes = messageSource.getMessage("vicketLapsedErrorMsg",
                        [vicket.serialNumber].toArray(), locale).getBytes()
            }
            return new ResponseVS(statusCode:statusCode, messageBytes: messageBytes, contentType: contentType,
                    type:TypeVS.ERROR)
            return new ResponseVS(type:TypeVS.ERROR, messageBytes: messageBytes, contentType: contentType,
                    metaInf:MetaInfMsg.getErrorMsg(CLASS_NAME, methodName, "VicketState_" + vicket.getState().toString()),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    public ResponseVS cancelTransactionVS(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        //messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        log.debug(smimeMessageReq.getSignedContent())
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = smimeMessageReq.getFrom().toString()
        String subject = messageSource.getMessage('vicketReceiptSubject', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                smimeMessageReq, subject)
        MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                content:smimeMessageResp.getBytes()).save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, type:TypeVS.VICKET_CANCEL, data:messageSMIMEResp,
                contentType: ContentTypeVS.JSON_SIGNED)
    }

    public ResponseVS processVicketTransaction(VicketTransactionBatch vicketBatch) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        List<Vicket> validatedVicketList = new ArrayList<Vicket>()
        DateUtils.TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        for(Vicket vicket : vicketBatch.getVicketList()) {
            try {
                UserVS toUserVS = UserVS.findWhere(IBAN:vicket.getToUserIBAN())
                if(!toUserVS) throw new ExceptionVS("Error - Vicket with id '${vicket?.id}' has wrong receptor IBAN '" +
                        vicket.getToUserIBAN() + "'", MetaInfMsg.getErrorMsg(methodName, 'toUserVSERROR'))
                vicket.setToUserVS(toUserVS)
                validatedVicketList.add(validateVicket(vicket));
            } catch(Exception ex) {
                String msg = "Error validating Vicket with id '${vicket?.id}' and hash '${vicket?.hashCertVS}'";
                if(ex instanceof ExceptionVS) throw ex
                else throw new ExceptionVS("Error validating Vicket with id '${vicket?.id}' and hash '${vicket?.hashCertVS}'",
                        MetaInfMsg.getErrorMsg(methodName, 'vicketExpended'), ex)
            }
        }
        List responseList = []
        for(Vicket vicket: validatedVicketList) {
            MessageSMIME messageSMIME = new MessageSMIME(smimeMessage:vicket.getSMIMEMessage(), type:TypeVS.VICKET).save()
            Date validTo = null
            if(vicket.isTimeLimited == true) validTo = timePeriod.getDateTo()
            TransactionVS transactionVS = new TransactionVS(amount: vicket.amount, messageSMIME:messageSMIME,
                    state:TransactionVS.State.OK, validTo: validTo, subject:vicket.getSubject(),
                    type:TransactionVS.Type.VICKET_SEND, currencyCode: vicket.getCurrencyCode(), tag:vicket.getTag()).save()
            vicket.setState(Vicket.State.EXPENDED).setTransactionVS(transactionVS).save()
            SMIMEMessage receipt = signatureVSService.getMultiSignedMimeMessage(systemService.getSystemUser().getName(),
                    vicket.getHashCertVS(), vicket.getSMIMEMessage(), vicket.getSubject())
            MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIME,
                    smimeMessage:receipt).save()
            responseList.add([(vicket.getHashCertVS()):Base64.getEncoder().encodeToString(receipt.getBytes())])
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data: responseList)
    }

    public Vicket validateVicket(Vicket vicket) throws ExceptionVS {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessage = vicket.getSMIMEMessage()
        Vicket vicketDB =  Vicket.findWhere(serialNumber:vicket.getX509AnonymousCert().serialNumber.longValue(),
                hashCertVS:vicket.getHashCertVS())
        if(!vicketDB) throw new ExceptionVS(messageSource.getMessage('hashCertVSVicketInvalidErrorMsg',
                [vicket.getHashCertVS()].toArray(), LocaleContextHolder.locale),
                MetaInfMsg.getErrorMsg(methodName, 'vicketDBMissing'))
        vicket = vicketDB.checkRequestWithDB(vicket)
        if(vicket.state == Vicket.State.EXPENDED) {
            throw new ExceptionVS(messageSource.getMessage('vicketExpendedErrorMsg',
                    [vicket.getHashCertVS()].toArray(), LocaleContextHolder.locale),
                    MetaInfMsg.getErrorMsg(methodName, 'vicketExpended'))
        } else if(vicket.state == Vicket.State.OK) {
            Set<UserVS> signersVS = smimeMessage.getSigners();
            if (signersVS.isEmpty()) throw new ExceptionVS(messageSource.getMessage('documentWithoutSignersErrorMsg',
                    null, LocaleContextHolder.locale), MetaInfMsg.getErrorMsg(methodName, 'vicketExpended'))
            UserVS userVS = smimeMessage.getSigner(); //anonymous signer
            CertExtensionCheckerVS extensionChecker
            if (userVS.getTimeStampToken() != null) {
                ResponseVS timestampValidationResp = timeStampService.validateToken(
                        userVS.getTimeStampToken(), LocaleContextHolder.locale)
                log.debug("$methodName - timestampValidationResp - " +
                        "statusCode:${timestampValidationResp.statusCode} - message:${timestampValidationResp.message}")
                if (ResponseVS.SC_OK != timestampValidationResp.statusCode) {
                    throw new ExceptionVS(timestampValidationResp.getMessage(),
                            MetaInfMsg.getErrorMsg(methodName, 'timestampError'))
                }
            } else throw new ExceptionVS(messageSource.getMessage('documentWithoutTimeStampErrorMsg', null,
                    LocaleContextHolder.locale), MetaInfMsg.getErrorMsg(methodName, 'timestampMissing'))
            ResponseVS validationResponse = CertUtil.verifyCertificate(signatureVSService.getVicketAnchors(),
                    false, [userVS.getCertificate()])
            X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
            extensionChecker = validationResponse.data.extensionChecker
            //if (extensionChecker.isAnonymousSigner()) { }
        } else  throw new ExceptionVS(messageSource.getMessage('vicketStateErrorMsg',
                [vicket.id, vicket.state.toString()].toArray(), LocaleContextHolder.locale),
                MetaInfMsg.getErrorMsg(methodName, 'vicketStateError'))
        vicket.setAuthorityCertificateVS(signatureVSService.getServerCertificateVS())
        return vicket
    }

    public ResponseVS processVicketRequest(VicketRequestBatch vicketBatch) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS fromUserVS = vicketBatch.messageSMIME.userVS
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(Calendar.getInstance())
        //Check cash available for user
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements =
                walletVSService.getAccountMovementsForTransaction( fromUserVS.IBAN, vicketBatch.getTag(),
                vicketBatch.getRequestAmount(), vicketBatch.getCurrencyCode())
        if(ResponseVS.SC_OK != accountFromMovements.getStatusCode()) {
            log.error "${methodName} - ${accountFromMovements.getMessage()}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR,
                    reason:accountFromMovements.getMessage(), message:accountFromMovements.getMessage(),
                    metaInf: MetaInfMsg.getErrorMsg(CLASS_NAME, methodName, "lowBalance"))
        }
        vicketBatch = csrService.signVicketBatchRequest(vicketBatch)
        TransactionVS userTransaction = vicketBatch.getTransactionVS(messageSource.getMessage(
                'vicketRequest', null, LocaleContextHolder.locale), accountFromMovements.data).save()
        LoggerVS.logVicketRequest(userTransaction.id, fromUserVS.nif, userTransaction.currencyCode, userTransaction.amount,
                userTransaction.tag, userTransaction.dateCreated)
        Map transactionMap = transactionVSService.getTransactionMap(userTransaction)
        Map resultMap = [transactionList:[transactionMap], issuedVickets:vicketBatch.getIssuedVicketListPEM()]
        return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data:resultMap,
                type:TypeVS.VICKET_REQUEST);
    }

}