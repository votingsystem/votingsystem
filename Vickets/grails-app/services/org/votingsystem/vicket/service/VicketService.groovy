package org.votingsystem.vicket.service

import grails.converters.JSON
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.x509.extension.X509ExtensionUtil
import org.springframework.context.i18n.LocaleContextHolder
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.StringUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.model.Vicket
import org.votingsystem.vicket.model.VicketBatch
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.vicket.util.IbanVSUtil

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

    public ResponseVS processTransactionVS(MessageSMIME messageSMIMEReq, VicketBatch batchRequest, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        X509Certificate vicketX509Cert = messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        String msg;
        ResponseVS resultResponseVS;
        IbanVSUtil.validate(toUser);

        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = smimeMessageReq.getFrom().toString()
        String subject = messageSource.getMessage('vicketReceiptSubject', null, locale)

        String hashCertVS = null;
        byte[] vicketExtensionValue = vicketX509Cert.getExtensionValue(ContextVS.VICKET_OID);
        if(vicketExtensionValue != null) {
            DERTaggedObject vicketCertDataDER = (DERTaggedObject) X509ExtensionUtil.fromExtensionValue(vicketExtensionValue);
            def vicketCertData = JSON.parse(((DERUTF8String) vicketCertDataDER.getObject()).toString());
            hashCertVS = vicketCertData.hashCertVS
        }
        Vicket vicket = Vicket.findWhere(serialNumber:vicketX509Cert.serialNumber.longValue(),
                hashCertVS:hashCertVS)
        if(!vicket) throw new ExceptionVS(messageSource.getMessage("vicketNotFoundErrorMsg", null, locale))
        if(Vicket.State.OK == vicket.state) {
            vicket.setMessageSMIME(messageSMIMEReq)
            vicket.state = Vicket.State.EXPENDED
            vicket.save()
            messageSMIMEReq.setType(TypeVS.VICKET);
            messageSMIMEReq.save()
            SMIMEMessage smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                    smimeMessageReq, subject)
            MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                    content:smimeMessageResp.getBytes()).save()

            TransactionVS transaction = new TransactionVS(amount: vicket.amount, messageSMIME:messageSMIMEReq,
                    subject:messageSource.getMessage('sendVicketTransactionSubject', null, locale),
                    state:TransactionVS.State.OK, currency:vicket.currencyCode, type:TransactionVS.Type.VICKET_SEND).save()

            Map dataMap = [vicketReceipt:messageSMIMEResp, vicket:vicket]
            messageSMIMEReq.setType(TypeVS.VICKET)
            resultResponseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET, data:dataMap)
        } else if (Vicket.State.EXPENDED == vicket.state) {
            log.error("processTransactionVS - model '${vicket.id}' state ${vicket.state}")
            Map dataMap = [message:messageSource.getMessage("vicketExpendedErrorMsg", null, locale),
                           messageSMIME:new String(Base64.encode(vicket.messageSMIME.content))]
            resultResponseVS = new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST_REPEATED,
                    type:TypeVS.ERROR, messageBytes: "${dataMap as JSON}".getBytes(), data:dataMap,
                    contentType:ContentTypeVS.JSON, reason:dataMap.message,
                    metaInf: MetaInfMsg.getErrorMsg(CLASS_NAME, methodName, "vicketExpendedError"))
        }
        if(batchRequest) messageSMIMEReq.batchRequest = batchRequest
        messageSMIMEReq.save()
        return resultResponseVS
    }

    public ResponseVS processVicketRequest(VicketBatch vicketBatchRequest) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS fromUserVS = vicketBatchRequest.messageSMIME.getSmimeMessage().signerVS
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(Calendar.getInstance())
        //Check cash available for user
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements =
                walletVSService.getAccountMovementsForTransaction( fromUserVS.IBAN, vicketBatchRequest.getTag(),
                vicketBatchRequest.getRequestAmount(), vicketBatchRequest.getCurrencyCode())
        if(ResponseVS.SC_OK != accountFromMovements.getStatusCode()) {
            log.error "${methodName} - ${accountFromMovements.getMessage()}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR,
                    reason:accountFromMovements.getMessage(), message:accountFromMovements.getMessage(),
                    metaInf: MetaInfMsg.getErrorMsg(CLASS_NAME, methodName, "lowBalance"))
        }
        vicketBatchRequest = csrService.signVicketBatchRequest(vicketBatchRequest)
        TransactionVS userTransaction = new TransactionVS(amount:vicketBatchRequest.requestAmount,
                state:TransactionVS.State.OK, currency:vicketBatchRequest.currencyCode,
                subject: messageSource.getMessage('vicketRequest', null, LocaleContextHolder.locale),
                messageSMIME: vicketBatchRequest.messageSMIME, fromUserVS: fromUserVS,
                type:TransactionVS.Type.VICKET_REQUEST).save()

        Map transactionMap = transactionVSService.getTransactionMap(userTransaction)
        Map resultMap = [transactionList:[transactionMap], issuedVickets:vicketBatchRequest.getIssuedVicketListPEM()]
        return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON,
                type:TypeVS.VICKET_REQUEST, messageBytes:"${resultMap as JSON}".getBytes());
    }

}