package org.votingsystem.ticket.service

import grails.converters.JSON
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.votingsystem.model.CurrencyVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.CurrencyVS
import org.votingsystem.model.ticket.TransactionVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.StringUtils

import java.math.RoundingMode

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class TicketService {

    def messageSource
    def transactionVSService
    def grailsApplication

	public ResponseVS processRequest(MessageSMIME messageSMIMEReq, Locale locale) {
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        String msg;
        try {
            def dataRequestJSON = JSON.parse(smimeMessageReq.getSignedContent())
            String ticketServerURL = StringUtils.checkURL(dataRequestJSON.serverURL)
            String serverURL = grailsApplication.config.grails.serverURL
            if(!serverURL.equals(ticketServerURL)) throw new ExceptionVS(messageSource.getMessage("serverMismatchErrorMsg",
                    [serverURL, ticketServerURL].toArray(), locale));

            TypeVS operation = TypeVS.valueOf(dataRequestJSON.operation)
            if(TypeVS.TICKET_REQUEST != operation) throw new ExceptionVS(messageSource.getMessage(
                    "operationMismatchErrorMsg", [TypeVS.TICKET_REQUEST.toString(), operation.toString()].toArray(),
                    locale));

            CurrencyVS requestCurrency = CurrencyVS.valueOf(dataRequestJSON.currency)

            Map userInfoMap = transactionVSService.getUserInfoMap(signer)

            BigDecimal totalAmount = new BigDecimal(dataRequestJSON.totalAmount)
            if(((BigDecimal)userInfoMap.available).compareTo(totalAmount) < 0) throw new ExceptionVS(
                    messageSource.getMessage("ticketRequestAvailableErrorMsg",
                    [totalAmount, userInfoMap.available].toArray(), locale));

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
            return new ResponseVS(statusCode:ResponseVS.SC_OK, data:resultMap, type:TypeVS.TICKET_REQUEST)
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(),
                    type:TypeVS.TICKET_REQUEST_ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.TICKET_REQUEST_ERROR,
                    message:messageSource.getMessage('ticketWithdrawalDataError', null, locale))
        }
    }

    public ResponseVS cancel(MessageSMIME messageSMIMEReq, Locale locale) {

    }

}