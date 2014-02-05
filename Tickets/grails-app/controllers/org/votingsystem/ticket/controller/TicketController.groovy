package org.votingsystem.ticket.controller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.TransactionVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper

class TicketController {

    def ticketService
    def csrService
    def userVSService
    def transactionVSService

    def request() {}

    def cancel() {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = ticketService.cancelTicket(messageSMIMEReq, request.getLocale())
        return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
    }


    def cancelBatch () {
        JSONObject jsonRequest = JSON.parse(new String(params.requestBytes))
        JSONArray ticketCancellationArray = jsonRequest.getJSONArray("ticketCancellationList")
        ticketCancellationArray.each {
            log.debug("ticketCancellationArray -it: ${it}")
        }
    }

    /**
     * Servicio que valida las solicitudes de tickets de los usuarios.
     *
     * @httpMethod [POST]
     * @serviceURL [/ticket/request]
     * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] La solicitud de certificado de delegación.
     * @param [csr] Obligatorio. La solicitud de certificado de delegación anónima.
     * @return La solicitud de certificado de delegación anónima firmada.
     */
    def processRequestFileMap() {
        MessageSMIME messageSMIMEReq = params[ContextVS.TICKET_REQUEST_DATA_FILE_NAME]
        request.messageSMIMEReq = messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = transactionVSService.processTicketRequest(messageSMIMEReq,
                params[ContextVS.CSR_FILE_NAME], request.getLocale())
        if(!responseVS.contentType) responseVS.setContentType(ContentTypeVS.ENCRYPTED);
        return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
    }
}
