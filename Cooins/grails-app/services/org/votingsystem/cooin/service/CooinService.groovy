package org.votingsystem.cooin.service

import grails.converters.JSON
import org.votingsystem.groovy.util.TransactionVSUtils
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.cooin.model.*

import java.security.cert.X509Certificate

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class CooinService {

    def messageSource
    def transactionVSService
    def grailsApplication
    def signatureVSService
    def userVSService
    def csrService
    def walletVSService
    def timeStampService
    def systemService

    public ResponseVS cancelCooin(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
        UserVS signer = messageSMIMEReq.userVS
        def requestJSON = JSON.parse(smimeMessageReq.getSignedContent())
        if(TypeVS.COOIN_CANCEL != TypeVS.valueOf(requestJSON.operation))
                throw new ExceptionVS(messageSource.getMessage("operationMismatchErrorMsg",
                [TypeVS.COOIN_CANCEL.toString(),requestJSON.operation ].toArray(), locale))
        def hashCertVSBase64 = CMSUtils.getHashBase64(requestJSON.originHashCertVS, ContextVS.VOTING_DATA_DIGEST)
        if(!hashCertVSBase64.equals(requestJSON.hashCertVSBase64))
            throw new ExceptionVS(messageSource.getMessage("originHashErrorMsg", null, locale))
        Cooin cooin = Cooin.findWhere(hashCertVS: requestJSON.hashCertVSBase64,
                serialNumber:Long.valueOf(requestJSON.cooinCertSerialNumber))
        if(Cooin.State.OK == cooin.getState()) {
            String fromUser = grailsApplication.config.vs.serverName
            String toUser = smimeMessageReq.getFrom().toString()
            String subject = messageSource.getMessage('cancelCooinReceiptSubject', null, locale)
            cooin.setState(Cooin.State.CANCELLED)
            SMIMEMessage receipt = signatureVSService.getSMIMEMultiSigned(fromUser, toUser,
                    smimeMessageReq, subject)
            messageSMIMEReq.setSMIME(receipt)
            cooin.cancelMessage = messageSMIMEReq
            cooin.save()
            TransactionVS transaction = new TransactionVS(amount: cooin.amount, messageSMIME:messageSMIMEReq,
                    subject:messageSource.getMessage('cancelCooinTransactionSubject', null, locale),
                    fromUserVS:signer, toUserVS:signer, state:TransactionVS.State.OK,
                    currency:cooin.currency, type:TransactionVS.Type.COOIN_CANCELLATION, validTo:cooin.validTo).save()
            log.debug("cancelCooin - model: ${cooin.id} - transactionVS: ${transaction.id}");
            return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON_SIGNED,
                    messageBytes: cooin.cancelMessage.content, type:TypeVS.COOIN_CANCEL)
        } else {
            log.error("$methodName - ERROR - request for cancel cooin: ${cooin.id} - with state: ${cooin.state}");
            byte[] messageBytes
            ContentTypeVS contentType = ContentTypeVS.TEXT
            int statusCode = ResponseVS.SC_ERROR_REQUEST
            if(Cooin.State.CANCELLED == cooin.getState()) {
                contentType = ContentTypeVS.JSON_SIGNED
                messageBytes = cooin.cancelMessage.content
            } else if(Cooin.State.EXPENDED == cooin.getState()) {
                contentType = ContentTypeVS.JSON_SIGNED
                messageBytes = cooin.messageSMIME.content
            }
            if(Cooin.State.LAPSED == cooin.getState()) {
                contentType = ContentTypeVS.TEXT
                messageBytes = messageSource.getMessage("cooinLapsedErrorMsg",
                        [cooin.serialNumber].toArray(), locale).getBytes()
            }
            return new ResponseVS(type:TypeVS.ERROR, messageBytes: messageBytes, contentType: contentType,
                    metaInf:MetaInfMsg.getErrorMsg(this.getClass().getSimpleName(), methodName,
                    "CooinState_" + cooin.getState().toString()), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    public ResponseVS cancelTransactionVS(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
        //messageSMIMEReq?.getSMIME()?.getSigner()?.certificate
        log.debug(smimeMessageReq.getSignedContent())
        String fromUser = grailsApplication.config.vs.serverName
        String toUser = smimeMessageReq.getFrom().toString()
        String subject = messageSource.getMessage('cooinReceiptSubject', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned(fromUser, toUser,
                smimeMessageReq, subject)
        messageSMIMEReq.setSMIME(smimeMessageResp)
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, type:TypeVS.COOIN_CANCEL, data:messageSMIMEReq,
                contentType: ContentTypeVS.JSON_SIGNED)
    }

    public ResponseVS processCooinTransaction(CooinTransactionBatch cooinBatch) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        List<Cooin> validatedCooinList = new ArrayList<Cooin>()
        DateUtils.TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        UserVS toUserVS;
        for(Cooin cooin : cooinBatch.getCooinList()) {
            try {
                toUserVS = UserVS.findWhere(IBAN:cooin.getToUserIBAN())
                if(!toUserVS) throw new ExceptionVS("Error - Cooin with hash '${cooin?.hashCertVS}' has wrong receptor IBAN '" +
                        cooin.getToUserIBAN() + "'", MetaInfMsg.getErrorMsg(methodName, 'toUserVSERROR'))
                cooin.setToUserVS(toUserVS)
                validatedCooinList.add(validateCooin(cooin));
            } catch(Exception ex) {
                String msg = "Error validating Cooin with id '${cooin?.id}' and hash '${cooin?.hashCertVS}'";
                if(ex instanceof ExceptionVS) throw ex
                else throw new ExceptionVS("Error validating Cooin with id '${cooin?.id}' and hash '${cooin?.hashCertVS}'",
                        MetaInfMsg.getErrorMsg(methodName, 'cooinExpended'), ex)
            }
        }
        List responseList = []
        Map currencyMap = [:]
        List<TransactionVS> transactionVSList = []
        for(Cooin cooin: validatedCooinList) {
            Date validTo = null
            if(cooin.isTimeLimited == true) validTo = timePeriod.getDateTo()
            SMIMEMessage receipt = signatureVSService.getSMIMEMultiSigned(systemService.getSystemUser().getName(),
                    cooin.getHashCertVS(), cooin.getSMIME(), cooin.getSubject())
            MessageSMIME messageSMIME = new MessageSMIME(smimeMessage:receipt, type:TypeVS.COOIN_SEND).save()
            TransactionVS transactionVS = new TransactionVS(amount: cooin.amount, messageSMIME:messageSMIME,
                    toUserIBAN:cooin.getToUserIBAN(), state:TransactionVS.State.OK, validTo: validTo,
                    subject:cooin.getSubject(), toUserVS: cooin.getToUserVS(), type:TransactionVS.Type.COOIN_SEND,
                    currencyCode: cooin.getCurrencyCode(), tag:cooin.getTag()).save()
            transactionVSList.add(transactionVS)
            cooin.setState(Cooin.State.EXPENDED).setTransactionVS(transactionVS).save()
            responseList.add([(cooin.getHashCertVS()):Base64.getEncoder().encodeToString(receipt.getBytes())])
        }
        Map resultMap = [statusCode:ResponseVS.SC_OK, message: messageSource.getMessage('cooinSendResultMsg',
                [toUserVS.name, TransactionVSUtils.getBalancesMapMsg(messageSource.getMessage('forLbl', null, locale),
                        TransactionVS.getBalances(transactionVSList, TransactionVS.Source.FROM))].toArray(),
                locale), receiptList:responseList]
        return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data: resultMap)
    }

    public Cooin validateCooin(Cooin cooin) throws ExceptionVS {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessage = cooin.getSMIME()
        Cooin cooinDB =  Cooin.findWhere(serialNumber:cooin.getX509AnonymousCert().serialNumber.longValue(),
                hashCertVS:cooin.getHashCertVS())
        if(!cooinDB) throw new ExceptionVS(messageSource.getMessage('hashCertVSCooinInvalidErrorMsg',
                [cooin.getHashCertVS()].toArray(), locale),
                MetaInfMsg.getErrorMsg(methodName, 'cooinDBMissing'))
        cooin = cooinDB.checkRequestWithDB(cooin)
        if(cooin.state == Cooin.State.EXPENDED) {
            throw new ExceptionVS(messageSource.getMessage('cooinExpendedErrorMsg',
                    [cooin.getHashCertVS()].toArray(), locale),
                    MetaInfMsg.getErrorMsg(methodName, 'cooinExpended'))
        } else if(cooin.state == Cooin.State.OK) {
            UserVS userVS = smimeMessage.getSigner(); //anonymous signer
            CertExtensionCheckerVS extensionChecker
            timeStampService.validateToken(userVS.getTimeStampToken())
            CertUtils.CertValidatorResultVS certValidatorResult = CertUtils.verifyCertificate(signatureVSService.getCooinAnchors(),
                    false, [userVS.getCertificate()])
            X509Certificate certCaResult = certValidatorResult.result.trustAnchor.trustedCert;
            extensionChecker = certValidatorResult.checker
            //if (extensionChecker.isAnonymousSigner()) { }
        } else  throw new ExceptionVS(messageSource.getMessage('cooinStateErrorMsg',
                [cooin.id, cooin.state.toString()].toArray(), locale),
                MetaInfMsg.getErrorMsg(methodName, 'cooinStateError'))
        cooin.setAuthorityCertificateVS(signatureVSService.getServerCertificateVS())
        return cooin
    }

    public ResponseVS processCooinRequest(CooinRequestBatch cooinBatch) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS fromUserVS = cooinBatch.messageSMIME.userVS
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(Calendar.getInstance())
        //Check cash available for user
        Map<CooinAccount, BigDecimal> accountFromMovements = walletVSService.getAccountMovementsForTransaction(
                fromUserVS.IBAN, cooinBatch.getTagVS(), cooinBatch.getRequestAmount(), cooinBatch.getCurrencyCode())
        cooinBatch = csrService.signCooinBatchRequest(cooinBatch)
        TransactionVS userTransaction = cooinBatch.getTransactionVS(messageSource.getMessage(
                'cooinRequestLbl', null, locale), accountFromMovements).save()
        String message = messageSource.getMessage('withdrawalMsg', [cooinBatch.getRequestAmount().toString(),
                cooinBatch.getCurrencyCode()].toArray(), locale) + " " + systemService.getTagMessage(cooinBatch.getTag())

        Map resultMap = [statusCode: ResponseVS.SC_OK, message:message, issuedCooins:cooinBatch.getIssuedCooinListPEM()]
        return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data:resultMap,
                type:TypeVS.COOIN_REQUEST);
    }

}