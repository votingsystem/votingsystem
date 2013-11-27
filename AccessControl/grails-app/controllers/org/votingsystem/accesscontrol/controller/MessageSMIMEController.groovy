package org.votingsystem.accesscontrol.controller

import org.servidordnie.persistencia.modelo.*
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
/**
 * @infoController Mensajes firmados
 * @descController Servicios relacionados con los messages firmados manejados por la
 *                 aplicación.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class MessageSMIMEController {
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/messageSMIME/$id] 
	 * @param [id]	Obligatorio. Identificador del message en la base de datos
	 * @return El message solicitado.
	 */
	def index() { 
        def messageSMIME
		MessageSMIME.withTransaction{
			messageSMIME = MessageSMIME.get(params.long('id'))
		}
        if (messageSMIME) {
            response.status = ResponseVS.SC_OK
            response.contentLength = messageSMIME.content.length
            response.setContentType(ContentTypeVS.TEXT)
            response.outputStream <<  messageSMIME.content
            response.outputStream.flush()
            return false
        }
        response.status = ResponseVS.SC_NOT_FOUND
        render message(code: 'eventVSNotFound', args:[params.id])
        return false
	}
	
	
	/**
	 * Servicio que devuelve el recibo con el que respondió el servidor al un message
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/messageSMIME/recibo/$requestMessageId] 
	 * @param [requestMessageId] Obligatorio. Identificador del message origen del recibo 
	 *                         en la base de datos
	 * @return El recibo asociado al message pasado como parámetro.
	 */
	def recibo() {
		def messageSMIMEPadre = MessageSMIME.get(params.long('requestMessageId'))
		if (messageSMIMEPadre) {
			def messageSMIME = MessageSMIME.findWhere(smimeParent:messageSMIMEPadre,
				type: TypeVS.RECEIPT)
			if (messageSMIME) {
				response.status = ResponseVS.SC_OK
				response.contentLength = messageSMIME.content.length
				response.setContentType(ContentTypeVS.TEXT)
				response.outputStream <<  messageSMIME.content
				response.outputStream.flush()
				return false
			}
		}
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: 'eventVSNotFound', args:[params.smimeParentId])
		return false
	}
	
}