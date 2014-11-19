package org.votingsystem.accesscontrol.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS


/**
 * @infoController Recogida de reclamaciones
 * @descController Servicios relacionados con la recogida de reclamaciones.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class EventVSClaimCollectorController {

	def eventVSClaimSignatureCollectorService
        
	/**
	 * Servicio que valida reclamaciones recibidas en documentos SMIME
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/eventVSClaimCollector]
	 * @requestContentType [application/pkcs7-signature] Obligatorio.
	 *                     documento SMIME firmado con la reclamación.
	 * @responseContentType [application/pkcs7-signature]. Recibo firmado por el sistema.
	 * @return  Recibo que consiste en el documento recibido con la firma añadida del servidor.
	 */
	def index() {
		MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS: ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS:eventVSClaimSignatureCollectorService.save(messageSMIME)]
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}