package org.votingsystem.ticket.controller

import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS

class TransactionController {

    def userVSService

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
        boolean isUserAdmin = userVSService.isUserAdmin(messageSMIMEReq.getUserVS().getNif())

    }

}
