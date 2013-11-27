package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
/**
 * @infoController Recogida de reclamaciones
 * @descController Servicios relacionados con la recogida de reclamaciones.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventVSClaimCollectorController {

	def eventVSClaimSignatureCollectorService
        
	/**
	 * Servicio que valida reclamaciones recibidas en documentos SMIME
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/eventVSClaimCollector]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio. 
	 *                     PDFDocumentVS SMIME firmado con la reclamación.
	 * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
	 * @return  Recibo que consiste en el PDFDocumentVS recibido con la signatureVS añadida del servidor.
	 */
	def index() { 
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
        try {
            ResponseVS responseVS = eventVSClaimSignatureCollectorService.save(
				messageSMIMEReq, request.getLocale())
			if (ResponseVS.SC_OK == responseVS?.statusCode) {
				response.contentType = ContentTypeVS.SIGNED
				params.receiverCert = messageSMIMEReq.getSmimeMessage().getSigner().certificate
			}	
			params.responseVS = responseVS
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
			params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
				message:message(code:'signClaimErrorMessage'), 
				type:TypeVS.CLAIM_EVENT_SIGNATURE_ERROR)
        }
	}
	

}