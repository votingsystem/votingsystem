package org.votingsystem.vicket.service

import grails.converters.JSON
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.x509.extension.X509ExtensionUtil
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.CurrencyVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.vicket.Vicket
import org.votingsystem.model.vicket.VicketBatchRequest
import org.votingsystem.model.vicket.TransactionVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.StringUtils

import java.math.RoundingMode
import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class VicketService {

    def messageSource
    def transactionVSService
    def grailsApplication
    def signatureVSService
    def userVSService

	public ResponseVS processRequest(MessageSMIME messageSMIMEReq, Locale locale) {
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

            CurrencyVS requestCurrency = CurrencyVS.valueOf(dataRequestJSON.currency)

            Calendar mondayLapse = DateUtils.getMonday(Calendar.getInstance())
            String dirPath = DateUtils.getDirPath(mondayLapse.getTime())
            Map userInfoMap = transactionVSService.getUserInfoMap(signer, mondayLapse)

            Map currencyMap = userInfoMap.get(dirPath).get(requestCurrency.toString())
            if(!currencyMap) throw new ExceptionVS(messageSource.getMessage("currencyMissingErrorMsg",
                    [requestCurrency.toString()].toArray(), locale));

            BigDecimal currencyAvailable = ((BigDecimal)currencyMap.totalInputs).add(
                    ((BigDecimal)currencyMap.totalOutputs).negate())

            BigDecimal totalAmount = new BigDecimal(dataRequestJSON.totalAmount)
            if(currencyAvailable.compareTo(totalAmount) < 0) throw new ExceptionVS(
                    messageSource.getMessage("vicketRequestAvailableErrorMsg",
                    [totalAmount, currencyAvailable,requestCurrency.toString()].toArray(), locale));

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
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        try {
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
                log.debug("cancelVicket - vicket: ${vicket.id} - transactionVS: ${transaction.id}");
                return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                        messageBytes: vicket.cancelMessage.content, type:TypeVS.VICKET_CANCEL)
            } else {
                log.error("cancelVicket - ERROR - request for cancel vicket: ${vicket.id} - with state: ${vicket.state}");
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
                        type:TypeVS.VICKET_CANCEL_ERROR)
            }
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, messageBytes: ex.getMessage().getBytes("UTF-8"),
                    contentType: ContentTypeVS.ENCRYPTED, type:TypeVS.VICKET_CANCEL_ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.VICKET_CANCEL_ERROR, contentType:
                    ContentTypeVS.ENCRYPTED, messageBytes: messageSource.getMessage(
                    'vicketRequestDataError', null, locale).getBytes("UTF-8"))
        }
    }


    public ResponseVS processVicketDeposit(MessageSMIME messageSMIMEReq, VicketBatchRequest batchRequest, Locale locale) {
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        X509Certificate vicketX509Cert = messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        String msg;
        ResponseVS resultResponseVS;
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
                log.error("processVicketDeposit - vicket '${vicket.id}' state ${vicket.state}")
                Map dataMap = [message:messageSource.getMessage("tickedExpendedErrorMsg", null, locale),
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

}