package org.votingsystem.ticket.controller

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
            return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, contentType:ContentTypeVS.TEXT_STREAM,
                    messageBytes:messageSMIME.content)]
        } else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'messageSMIMENotFound', args:[params.id]))]
	}
	
	
	/**
	 * Servicio que devuelve el recibo con el que respondió el servidor al un message
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/messageSMIME/receipt/$requestMessageId]
	 * @param [requestMessageId] Obligatorio. Identificador del message origen del recibo 
	 *                         en la base de datos
	 * @return El recibo asociado al message pasado como parámetro.
	 */
	def receipt() {
		def messageSMIMEOri = MessageSMIME.get(params.long('requestMessageId'))
		if (messageSMIMEOri) {
			def messageSMIME = MessageSMIME.findWhere(smimeParent:messageSMIMEOri, type: TypeVS.RECEIPT)
			if (messageSMIME) {
                return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.TEXT_STREAM,
                        messageBytes: messageSMIME.content)]
			}
		}
        return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'messageSMIMENotFound', args:[params.smimeParentId]))]
	}
	
}