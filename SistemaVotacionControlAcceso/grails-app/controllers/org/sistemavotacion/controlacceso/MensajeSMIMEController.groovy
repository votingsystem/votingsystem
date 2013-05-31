package org.sistemavotacion.controlacceso

import org.servidordnie.persistencia.modelo.*
import org.sistemavotacion.controlacceso.modelo.*;

/**
 * @infoController Mensajes firmados
 * @descController Servicios relacionados con los mensajes firmados manejados por la
 *                 aplicación.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class MensajeSMIMEController {
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/mensajeSMIME/$id] 
	 * @param [id]	Obligatorio. Identificador del mensaje en la base de datos
	 * @return El mensaje solicitado.
	 */
	def index() { 
        def mensajeSMIME
		MensajeSMIME.withTransaction{
			mensajeSMIME = MensajeSMIME.get(params.long('id'))
		}
        if (mensajeSMIME) {
            response.status = Respuesta.SC_OK
            response.contentLength = mensajeSMIME.contenido.length
            response.setContentType("text/plain")
            response.outputStream <<  mensajeSMIME.contenido
            response.outputStream.flush()
            return false
        }
        response.status = Respuesta.SC_NOT_FOUND
        render message(code: 'mensajeSMIME.eventoNoEncontrado', args:[params.id])
        return false
	}
	
	
	/**
	 * Servicio que devuelve el recibo con el que respondió el servidor al un mensaje
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/mensajeSMIME/recibo/$requestMessageId] 
	 * @param [requestMessageId] Obligatorio. Identificador del mensaje origen del recibo 
	 *                         en la base de datos
	 * @return El recibo asociado al mensaje pasado como parámetro.
	 */
	def recibo() {
		def mensajeSMIMEPadre = MensajeSMIME.get(params.long('requestMessageId'))
		if (mensajeSMIMEPadre) {
			def mensajeSMIME = MensajeSMIME.findWhere(smimePadre:mensajeSMIMEPadre,
				tipo: Tipo.RECIBO)
			if (mensajeSMIME) {
				response.status = Respuesta.SC_OK
				response.contentLength = mensajeSMIME.contenido.length
				response.setContentType("text/plain")
				response.outputStream <<  mensajeSMIME.contenido
				response.outputStream.flush()
				return false
			}
		}
		response.status = Respuesta.SC_NOT_FOUND
		render message(code: 'mensajeSMIME.eventoNoEncontrado', args:[params.smimePadreId])
		return false
	}
	
}