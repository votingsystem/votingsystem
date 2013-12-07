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
	 *                     documento SMIME firmado con la reclamación.
	 * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
	 * @return  Recibo que consiste en el documento recibido con la signatureVS añadida del servidor.
	 */
	def index() { 
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
        if(!messageSMIMEReq) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))
            return
        }
        ResponseVS responseVS = eventVSClaimSignatureCollectorService.save(messageSMIMEReq, request.getLocale())
        if (ResponseVS.SC_OK == responseVS?.statusCode) {
            responseVS.setContentType(ContentTypeVS.SIGNED)
            params.receiverCert = messageSMIMEReq.getSmimeMessage().getSigner().certificate
        }
        params.responseVS = responseVS
	}
	

}