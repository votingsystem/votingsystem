package org.votingsystem.ticket.service

import grails.converters.JSON
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.x509.extension.X509ExtensionUtil
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.CurrencyVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.TicketVS
import org.votingsystem.model.ticket.TicketVSBatchRequest
import org.votingsystem.model.ticket.TransactionVS
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
class TicketService {

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
            String ticketServerURL = StringUtils.checkURL(dataRequestJSON.serverURL)
            String serverURL = grailsApplication.config.grails.serverURL
            if(!serverURL.equals(ticketServerURL)) throw new ExceptionVS(messageSource.getMessage("serverMismatchErrorMsg",
                    [serverURL, ticketServerURL].toArray(), locale));

            TypeVS operation = TypeVS.valueOf(dataRequestJSON.operation)
            if(TypeVS.TICKET_REQUEST != operation) throw new ExceptionVS(messageSource.getMessage(
                    "operationMismatchErrorMsg", [TypeVS.TICKET_REQUEST.toString(), operation.toString()].toArray(),
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
                    messageSource.getMessage("ticketRequestAvailableErrorMsg",
                    [totalAmount, currencyAvailable,requestCurrency.toString()].toArray(), locale));

            Integer numTotalTickets = 0
            def ticketsArray = dataRequestJSON.tickets
            BigDecimal ticketsAmount = new BigDecimal(0)
            ticketsArray.each {
                Integer numTickets = it.numTickets
                Integer ticketsValue = it.ticketValue
                numTotalTickets = numTotalTickets + it.numTickets
                ticketsAmount = ticketsAmount.add(new BigDecimal(numTickets * ticketsValue))
                log.debug("batch of '${numTickets}' tickets of '${ticketsValue}' euros")
            }
            log.debug("numTotalTickets: ${numTotalTickets} - ticketsAmount: ${ticketsAmount}")
            if(totalAmount.compareTo(ticketsAmount) != 0) throw new ExceptionVS(messageSource.getMessage(
                    "ticketRequestAmountErrorMsg", [totalAmount, ticketsAmount].toArray(), locale));

            Map resultMap = [amount:totalAmount, currency:requestCurrency, userInfoMap:userInfoMap]
            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:resultMap, type:TypeVS.TICKET_REQUEST,)
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(),
                    type:TypeVS.TICKET_REQUEST_ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.TICKET_REQUEST_ERROR,
                    message:messageSource.getMessage('ticketRequestDataError', null, locale))
        }
    }

    public ResponseVS cancelTicket(MessageSMIME messageSMIMEReq, Locale locale) {
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        try {
            def requestJSON = JSON.parse(smimeMessageReq.getSignedContent())
            if(TypeVS.TICKET_CANCEL != TypeVS.valueOf(requestJSON.operation))
                throw new ExceptionVS(messageSource.getMessage("operationMismatchErrorMsg",
                        [TypeVS.TICKET_CANCEL.toString(),requestJSON.operation ].toArray(), locale))
            def hashCertVSBase64 = CMSUtils.getHashBase64(requestJSON.originHashCertVS, ContextVS.VOTING_DATA_DIGEST)
            if(!hashCertVSBase64.equals(requestJSON.hashCertVSBase64))
                throw new ExceptionVS(messageSource.getMessage("originHashErrorMsg", null, locale))
            TicketVS ticket = TicketVS.findWhere(hashCertVS: requestJSON.hashCertVSBase64,
                    serialNumber:Long.valueOf(requestJSON.ticketCertSerialNumber))
            if(TicketVS.State.OK == ticket.getState()) {
                String fromUser = grailsApplication.config.VotingSystem.serverName
                String toUser = smimeMessageReq.getFrom().toString()
                String subject = messageSource.getMessage('cancelTicketReceiptSubject', null, locale)
                ticket.setState(TicketVS.State.CANCELLED)
                SMIMEMessageWrapper receipt = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                        smimeMessageReq, subject)
                MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                        content:receipt.getBytes()).save()
                ticket.cancelMessage = messageSMIMEReq
                ticket.save()
                TransactionVS transaction = new TransactionVS(amount: ticket.amount, messageSMIME:messageSMIMEReq,
                        subject:messageSource.getMessage('cancelTicketTransactionSubject', null, locale),
                        fromUserVS:signer, toUserVS:signer, state:TransactionVS.State.OK,
                        currency:ticket.currency, type:TransactionVS.Type.TICKET_CANCELLATION, validTo:ticket.validTo).save()
                log.debug("cancelTicket - ticket: ${ticket.id} - transactionVS: ${transaction.id}");
                return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                        messageBytes: ticket.cancelMessage.content, type:TypeVS.TICKET_CANCEL)
            } else {
                log.error("cancelTicket - ERROR - request for cancel ticket: ${ticket.id} - with state: ${ticket.state}");
                byte[] messageBytes
                ContentTypeVS contentType = ContentTypeVS.ENCRYPTED
                int statusCode = ResponseVS.SC_ERROR_REQUEST
                //ResponseVS.
                if(TicketVS.State.CANCELLED == ticket.getState()) {
                    contentType = ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED
                    messageBytes = ticket.cancelMessage.content
                } else if(TicketVS.State.EXPENDED == ticket.getState()) {
                    contentType = ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED
                    messageBytes = ticket.messageSMIME.content
                }
                if(TicketVS.State.LAPSED == ticket.getState()) {
                    contentType = ContentTypeVS.ENCRYPTED
                    messageBytes = messageSource.getMessage("ticketLapsedErrorMsg",
                            [ticket.serialNumber].toArray(), locale).getBytes()
                }
                return new ResponseVS(statusCode:statusCode, messageBytes: messageBytes, contentType: contentType,
                        type:TypeVS.TICKET_CANCEL_ERROR)
            }
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, messageBytes: ex.getMessage().getBytes("UTF-8"),
                    contentType: ContentTypeVS.ENCRYPTED, type:TypeVS.TICKET_CANCEL_ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.TICKET_CANCEL_ERROR, contentType:
                    ContentTypeVS.ENCRYPTED, messageBytes: messageSource.getMessage(
                    'ticketRequestDataError', null, locale).getBytes("UTF-8"))
        }
    }


    public ResponseVS processTicketDeposit(MessageSMIME messageSMIMEReq, TicketVSBatchRequest batchRequest, Locale locale) {
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        X509Certificate ticketX509Cert = messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        String msg;
        ResponseVS resultResponseVS;
        try {
            String fromUser = grailsApplication.config.VotingSystem.serverName
            String toUser = smimeMessageReq.getFrom().toString()
            String subject = messageSource.getMessage('ticketReceiptSubject', null, locale)

            String hashCertVS = null;
            byte[] ticketExtensionValue = ticketX509Cert.getExtensionValue(ContextVS.TICKET_OID);
            if(ticketExtensionValue != null) {
                DERTaggedObject ticketCertDataDER = (DERTaggedObject) X509ExtensionUtil.fromExtensionValue(ticketExtensionValue);
                def ticketCertData = JSON.parse(((DERUTF8String) ticketCertDataDER.getObject()).toString());
                hashCertVS = ticketCertData.hashCertVS
            }
            TicketVS ticket = TicketVS.findWhere(serialNumber:ticketX509Cert.serialNumber.longValue(),
                    hashCertVS:hashCertVS)
            if(!ticket) throw new ExceptionVS(messageSource.getMessage("ticketNotFoundErrorMsg", null, locale))
            if(TicketVS.State.OK == ticket.state) {
                ticket.setMessageSMIME(messageSMIMEReq)
                ticket.state = TicketVS.State.EXPENDED
                ticket.save()
                messageSMIMEReq.setType(TypeVS.TICKET);
                messageSMIMEReq.save()
                SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                        smimeMessageReq, subject)
                MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                        content:smimeMessageResp.getBytes()).save()
                Map dataMap = [ticketReceipt:messageSMIMEResp, ticket:ticket]
                resultResponseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.TICKET, data:dataMap)
            } else if (TicketVS.State.EXPENDED == ticket.state) {
                log.error("processTicketDeposit - ticket '${ticket.id}' state ${ticket.state}")
                Map dataMap = [message:messageSource.getMessage("tickedExpendedErrorMsg", null, locale),
                        messageSMIME:new String(ticket.messageSMIME.content, "UTF-8")]
                resultResponseVS = new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST_REPEATED,
                        type:TypeVS.TICKET_DEPOSIT_ERROR, messageBytes: "${dataMap as JSON}".getBytes(), data:dataMap,
                        contentType:ContentTypeVS.JSON_ENCRYPTED)
            }
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            resultResponseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(),
                    type:TypeVS.TICKET_DEPOSIT_ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            msg = messageSource.getMessage('depositDataError', null, locale)
            resultResponseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.TICKET_DEPOSIT_ERROR)
        } finally {
            if(ResponseVS.SC_OK != resultResponseVS.getStatusCode()) {
                messageSMIMEReq.setType(TypeVS.TICKET_DEPOSIT_ERROR)
                if(ResponseVS.SC_ERROR_REQUEST_REPEATED == resultResponseVS.getStatusCode()) {
                    messageSMIMEReq.setReason("TICKET EXPENDED")
                } else  messageSMIMEReq.setReason(resultResponseVS.getMessage())
            } else {
                messageSMIMEReq.setType(TypeVS.TICKET)
            }
            if(batchRequest) messageSMIMEReq.batchRequest = batchRequest
            messageSMIMEReq.save()
            return resultResponseVS
        }
    }

}