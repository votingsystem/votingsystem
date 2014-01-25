package org.votingsystem.ticket.controller

import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.TransactionVS

class TicketController {

    def ticketService
    def csrService
    def userVSService

    def request() {}


    def cancel() {}

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
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = ticketService.processRequest(messageSMIMEReq, request.getLocale())
        if (ResponseVS.SC_OK == responseVS.statusCode) {
            byte[] csrRequest = params[ContextVS.CSR_FILE_NAME]
            ResponseVS ticketGenBatchResponse = csrService.signTicketBatchRequest(csrRequest,
                    responseVS.data.amount, responseVS.data.currency,request.getLocale())
            if (ResponseVS.SC_OK == ticketGenBatchResponse.statusCode) {
                ticketGenBatchResponse.setContentType(ContentTypeVS.JSON_ENCRYPTED)
                UserVS userVS = messageSMIMEReq.userVS
                TransactionVS userTransaction = new TransactionVS(amount:responseVS.data.amount,
                        fromUserVS: userVS, currency:responseVS.data.currency,
                        type:TransactionVS.Type.USER_OUTPUT).save()
                return [responseVS:ticketGenBatchResponse,
                        receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
            } else return [responseVS:ticketGenBatchResponse]
        } else return [responseVS:responseVS]
    }
}