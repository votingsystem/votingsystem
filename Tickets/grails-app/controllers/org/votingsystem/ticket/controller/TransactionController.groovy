package org.votingsystem.ticket.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS

class TransactionController {

    def userVSService
    def transactionVSService

    /**
     * Servicio que recibe los asignaciones de los usuarios en documentos SMIME
     *
     * @httpMethod [POST]
     * @serviceURL [/transaction/deposit]
     * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio.
     *                     documento SMIME firmado con un ticket emitido por el sistema.
     * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
     * @return  Recibo que consiste en el documento recibido con la firma añadida del servidor.
     */
    def deposit() {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ContentTypeVS contentTypeVS = ContentTypeVS.getByName(request?.contentType)
        ResponseVS responseVS = null
        if(ContentTypeVS.TICKET == contentTypeVS) {
            responseVS = transactionVSService.processTicketDeposit(messageSMIMEReq, request.locale)
        } else responseVS = transactionVSService.processDeposit(messageSMIMEReq, request.locale)
        return [responseVS:responseVS]
    }

}
