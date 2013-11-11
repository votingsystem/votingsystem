package org.votingsystem.accesscontrol.controller

import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.model.ContextVS
import grails.converters.JSON

/**
 * @infoController Recogida de reclamaciones
 * @descController Servicios relacionados con la recogida de reclamaciones.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class RecolectorReclamacionController {

	def reclamacionService
        
	/**
	 * Servicio que valida reclamaciones recibidas en documentos SMIME
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/recolectorReclamacion]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio. 
	 *                     Documento SMIME firmado con la reclamación.
	 * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
	 * @return  Recibo que consiste en el documento recibido con la firma añadida del servidor.
	 */
	def index() { 
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
        try {
            ResponseVS respuesta = reclamacionService.guardar(
				messageSMIMEReq, request.getLocale())
			if (ResponseVS.SC_OK == respuesta?.statusCode) {
				response.contentType = ContextVS.SI
			}	
			params.respuesta = respuesta
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
			params.respuesta = new ResponseVS(ResponseVS.SC_ERROR_PETICION, 
				message:message(code:'signClaimErrorMessage'), 
				type:TypeVS.FIRMA_EVENTO_RECLAMACION_ERROR)
        }
	}
	

}