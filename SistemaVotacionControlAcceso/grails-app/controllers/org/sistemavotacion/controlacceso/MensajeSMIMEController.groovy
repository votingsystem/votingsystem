package org.sistemavotacion.controlacceso

import org.servidordnie.persistencia.modelo.*
import org.sistemavotacion.controlacceso.modelo.*;

/**
 * @infoController Mensajes firmados
 * @descController Servicios relacionados con los mensajes firmados manejados por la
 *                 aplicación.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class MensajeSMIMEController {
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/mensajeSMIME'
	 */
	def index() { }
	
	/**
	 * @httpMethod GET
	 * @param id	Obligatorio. Identificador del mensaje en la base de datos
	 * @return El mensaje solicitado.
	 */
    def obtener () {
        if (params.long('id')) {
            def mensajeSMIME
			MensajeSMIME.withTransaction{
				mensajeSMIME = MensajeSMIME.get(params.id)
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
            render message(code: 'mensajeSMIME.eventoNoEncontrado', args:[params.ids])
            return false
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
	
	/**
	 * Servicio que devuelve el recibo con el que respondió el servidor al publicar un manifiesto.
	 *
	 * @httpMethod GET
	 * @param id	Obligatorio. Identificador del mensaje de publicación en la base de datos
	 * @return El recibo.
	 */
	def obtenerReciboFirma () {
		if (params.long('id')) {
			def mensajeSMIMEPadre = MensajeSMIME.get(params.id)
			if (mensajeSMIMEPadre) {
				def mensajeSMIME = MensajeSMIME.findWhere(smimePadre:mensajeSMIMEPadre,
					tipo: Tipo.FIRMA_VALIDADA)
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
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * Servicio que devuelve el recibo con el que respondió el servidor al publicar una reclamación.
	 * 
	 * @httpMethod GET
	 * @param id	Obligatorio. Identificador del mensaje de publicación en la base de datos
	 * @return El recibo.
	 */
	def obtenerReciboReclamacion () {
		if (params.long('id')) {
			def mensajeSMIMEPadre = MensajeSMIME.get(params.id)
			if (mensajeSMIMEPadre) {
				def mensajeSMIME = MensajeSMIME.findWhere(smimePadre:mensajeSMIMEPadre,
					tipo: Tipo.FIRMA_EVENTO_RECLAMACION_VALIDADA)
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
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
}