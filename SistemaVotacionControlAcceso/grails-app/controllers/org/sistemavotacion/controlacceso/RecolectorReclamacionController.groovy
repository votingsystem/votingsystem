package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.FileUtils;
import grails.converters.JSON

/**
 * @infoController Recogida de reclamaciones
 * @descController Servicios relacionados con la recogida de reclamaciones.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
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
		MensajeSMIME mensajeSMIMEReq = flash.mensajeSMIMEReq
		if(!mensajeSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
        try {
            Respuesta respuesta = reclamacionService.guardar(
				mensajeSMIMEReq, request.getLocale())
			if (Respuesta.SC_OK == respuesta?.codigoEstado) {
				response.contentType = "${grailsApplication.config.pkcs7SignedContentType}"
			}	
			flash.respuesta = respuesta
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
			flash.respuesta = new Respuesta(Respuesta.SC_ERROR_PETICION, 
				mensaje:message(code:'signClaimErrorMessage'), 
				tipo:Tipo.FIRMA_EVENTO_RECLAMACION_ERROR)
        }
	}
	

}