package org.votingsystem.vicket.service

import grails.converters.JSON
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.x509.extension.X509ExtensionUtil
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.StringUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.Vicket
import org.votingsystem.vicket.model.VicketBatchRequest
import org.votingsystem.util.MetaInfMsg

import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class VicketService {

    def messageSource
    def transactionVSService
    def grailsApplication
    def signatureVSService
    def userVSService

	public ResponseVS processRequest(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        def dataRequestJSON
        try {
            dataRequestJSON = JSON.parse(smimeMessageReq.getSignedContent())
            String vicketServerURL = StringUtils.checkURL(dataRequestJSON.serverURL)
            String serverURL = grailsApplication.config.grails.serverURL
            if(!serverURL.equals(vicketServerURL)) throw new ExceptionVS(messageSource.getMessage("serverMismatchErrorMsg",
                    [serverURL, vicketServerURL].toArray(), locale));

            TypeVS operation = TypeVS.valueOf(dataRequestJSON.operation)
            if(TypeVS.VICKET_REQUEST != operation) throw new ExceptionVS(messageSource.getMessage(
                    "operationMismatchErrorMsg", [TypeVS.VICKET_REQUEST.toString(), operation.toString()].toArray(),
                    locale));

            Currency requestCurrency = Currency.getInstance(dataRequestJSON.currency)

            Calendar mondayLapse = DateUtils.getMonday(Calendar.getInstance())
            String dirPath = DateUtils.getDirPath(mondayLapse.getTime())
            Map userInfoMap = transactionVSService.getUserInfoMap(signer, mondayLapse)

            Map currencyMap = userInfoMap.get(dirPath).get(requestCurrency.getCurrencyCode())
            if(!currencyMap) throw new ExceptionVS(messageSource.getMessage("currencyMissingErrorMsg",
                    [requestCurrency.getCurrencyCode()].toArray(), locale));

            BigDecimal currencyAvailable = ((BigDecimal)currencyMap.totalInputs).add(
                    ((BigDecimal)currencyMap.totalOutputs).negate())

            BigDecimal totalAmount = new BigDecimal(dataRequestJSON.totalAmount)
            if(currencyAvailable.compareTo(totalAmount) < 0) throw new ExceptionVS(
                    messageSource.getMessage("vicketRequestAvailableErrorMsg",
                    [totalAmount, currencyAvailable,requestCurrency.getCurrencyCode()].toArray(), locale));

            Integer numTotalVickets = 0
            def vicketsArray = dataRequestJSON.vickets
            BigDecimal vicketsAmount = new BigDecimal(0)
            vicketsArray.each {
                Integer numVickets = it.numVickets
                Integer vicketsValue = it.vicketValue
                numTotalVickets = numTotalVickets + it.numVickets
                vicketsAmount = vicketsAmount.add(new BigDecimal(numVickets * vicketsValue))
                log.debug("batch of '${numVickets}' vickets of '${vicketsValue}' euros")
            }
            log.debug("numTotalVickets: ${numTotalVickets} - vicketsAmount: ${vicketsAmount}")
            if(totalAmount.compareTo(vicketsAmount) != 0) throw new ExceptionVS(messageSource.getMessage(
                    "vicketRequestAmountErrorMsg", [totalAmount, vicketsAmount].toArray(), locale));

            Map resultMap = [amount:totalAmount, currency:requestCurrency, userInfoMap:userInfoMap]
            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:resultMap, type:TypeVS.VICKET_REQUEST,)
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(),
                    type:TypeVS.VICKET_REQUEST_ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.VICKET_REQUEST_ERROR,
                    message:messageSource.getMessage('vicketRequestDataError', null, locale))
        }
    }

    public ResponseVS cancelVicket(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
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
            SMIMEMessageWrapper receipt = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
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
            return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                    messageBytes: vicket.cancelMessage.content, type:TypeVS.VICKET_CANCEL)
        } else {
            log.error("cancelVicket - ERROR - request for cancel model: ${vicket.id} - with state: ${vicket.state}");
            byte[] messageBytes
            ContentTypeVS contentType = ContentTypeVS.ENCRYPTED
            int statusCode = ResponseVS.SC_ERROR_REQUEST
            //ResponseVS.
            if(Vicket.State.CANCELLED == vicket.getState()) {
                contentType = ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED
                messageBytes = vicket.cancelMessage.content
            } else if(Vicket.State.EXPENDED == vicket.getState()) {
                contentType = ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED
                messageBytes = vicket.messageSMIME.content
            }
            if(Vicket.State.LAPSED == vicket.getState()) {
                contentType = ContentTypeVS.ENCRYPTED
                messageBytes = messageSource.getMessage("vicketLapsedErrorMsg",
                        [vicket.serialNumber].toArray(), locale).getBytes()
            }
            return new ResponseVS(statusCode:statusCode, messageBytes: messageBytes, contentType: contentType,
                    type:TypeVS.ERROR)
            return new ResponseVS(type:TypeVS.ERROR, messageBytes: messageBytes, contentType: contentType,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "VicketState_" + vicket.getState().toString()),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    public ResponseVS cancelVicketDeposit(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        //messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        log.debug(smimeMessageReq.getSignedContent())
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = smimeMessageReq.getFrom().toString()
        String subject = messageSource.getMessage('vicketReceiptSubject', null, locale)
        SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                smimeMessageReq, subject)
        MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                content:smimeMessageResp.getBytes()).save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, type:TypeVS.VICKET_CANCEL, data:messageSMIMEResp,
                contentType: ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED)
    }

    public ResponseVS processVicketDeposit(MessageSMIME messageSMIMEReq, VicketBatchRequest batchRequest, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        X509Certificate vicketX509Cert = messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        String msg;
        ResponseVS resultResponseVS;
        try {
            IbanVSUtil.validate(toUser);
        } catch(Exception ex) {
            msg = messageSource.getMessage('IBANCodeErrorMsg', [smimeMessageReq.getFrom().toString()].toArray(),
                    locale)
            log.error("${msg} - ${ex.getMessage()}", ex)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: msg,
                    metaInf:MetaInfMsg.getExceptionMsg(methodName, ex, "IBAN_code"),
                    contentType: ContentTypeVS.ENCRYPTED, type:TypeVS.VICKET_DEPOSIT_ERROR)
        }


        try {
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
                SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                        smimeMessageReq, subject)
                MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                        content:smimeMessageResp.getBytes()).save()

                TransactionVS transaction = new TransactionVS(amount: vicket.amount, messageSMIME:messageSMIMEReq,
                    subject:messageSource.getMessage('sendVicketTransactionSubject', null, locale),
                    state:TransactionVS.State.OK, currency:vicket.currency, type:TransactionVS.Type.VICKET_SEND).save()

                Map dataMap = [vicketReceipt:messageSMIMEResp, vicket:vicket]
                resultResponseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET, data:dataMap)
            } else if (Vicket.State.EXPENDED == vicket.state) {
                log.error("processVicketDeposit - model '${vicket.id}' state ${vicket.state}")
                Map dataMap = [message:messageSource.getMessage("vicketExpendedErrorMsg", null, locale),
                        messageSMIME:new String(Base64.encode(vicket.messageSMIME.content))]
                resultResponseVS = new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST_REPEATED,
                        type:TypeVS.VICKET_DEPOSIT_ERROR, messageBytes: "${dataMap as JSON}".getBytes(), data:dataMap,
                        contentType:ContentTypeVS.JSON_ENCRYPTED)
            }
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            resultResponseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(),
                    type:TypeVS.VICKET_DEPOSIT_ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            msg = messageSource.getMessage('depositDataError', null, locale)
            resultResponseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VICKET_DEPOSIT_ERROR)
        } finally {
            if(ResponseVS.SC_OK != resultResponseVS.getStatusCode()) {
                messageSMIMEReq.setType(TypeVS.VICKET_DEPOSIT_ERROR)
                if(ResponseVS.SC_ERROR_REQUEST_REPEATED == resultResponseVS.getStatusCode()) {
                    messageSMIMEReq.setReason("VICKET EXPENDED")
                } else  messageSMIMEReq.setReason(resultResponseVS.getMessage())
            } else {
                messageSMIMEReq.setType(TypeVS.VICKET)
            }
            if(batchRequest) messageSMIMEReq.batchRequest = batchRequest
            messageSMIMEReq.save()
            return resultResponseVS
        }
    }

    def csrService


    public ResponseVS processVicketRequest(MessageSMIME messageSMIMEReq, byte[] csrRequest, Locale locale) {
        log.debug("processVicketRequest");
        //To avoid circular references issues
        ResponseVS responseVS = processRequest(messageSMIMEReq, locale)
        if (ResponseVS.SC_OK == responseVS.statusCode) {
            ResponseVS vicketGenBatchResponse = csrService.signVicketBatchRequest(csrRequest,
                    responseVS.data.amount, responseVS.data.currency, locale)
            if (ResponseVS.SC_OK == vicketGenBatchResponse.statusCode) {
                UserVS userVS = messageSMIMEReq.userVS
                TransactionVS userTransaction = new TransactionVS(amount:responseVS.data.amount,
                        state:TransactionVS.State.OK, currency:responseVS.data.currency,
                        subject: messageSource.getMessage('vicketRequest', null, locale), messageSMIME: messageSMIMEReq,
                        fromUserVS: userVS, toUserVS: userVS, type:TransactionVS.Type.VICKET_REQUEST).save()

                Map transactionMap = transactionVSService.getTransactionMap(userTransaction)
                Map resultMap = [transactionList:[transactionMap], issuedVickets:vicketGenBatchResponse.data]
                return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON_ENCRYPTED,
                        type:TypeVS.VICKET_REQUEST, messageBytes:"${resultMap as JSON}".getBytes());
            } else return vicketGenBatchResponse
        } else return responseVS
    }

}