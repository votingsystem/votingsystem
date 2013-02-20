package org.sistemavotacion.controlacceso

import org.servidordnie.persistencia.modelo.*
import org.sistemavotacion.controlacceso.modelo.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class MensajeSMIMEController {
	
	def index() { }
	
    def obtener = {
        if (params.long('id')) {
            def mensajeSMIME
			MensajeSMIME.withTransaction{
				mensajeSMIME = MensajeSMIME.get(params.id)
			}
            if (mensajeSMIME) {
                response.status = 200
                response.contentLength = mensajeSMIME.contenido.length
                response.setContentType("text/plain")
                response.outputStream <<  mensajeSMIME.contenido
                response.outputStream.flush()
                return false
            }
            response.status = 404
            render message(code: 'mensajeSMIME.eventoNoEncontrado', args:[params.ids])
            return false
        }
        response.status = 400
        render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
	
	def obtenerReciboFirma = {
		if (params.long('id')) {
			def mensajeSMIMEPadre = MensajeSMIME.get(params.id)
			if (mensajeSMIMEPadre) {
				def mensajeSMIME = MensajeSMIME.findWhere(smimePadre:mensajeSMIMEPadre,
					tipo: Tipo.FIRMA_VALIDADA)
				if (mensajeSMIME) {
					response.status = 200
					response.contentLength = mensajeSMIME.contenido.length
					response.setContentType("text/plain")
					response.outputStream <<  mensajeSMIME.contenido
					response.outputStream.flush()
					return false
				}
			}
			response.status = 404
			render message(code: 'mensajeSMIME.eventoNoEncontrado', args:[params.smimePadreId])
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def obtenerReciboReclamacion = {
		if (params.long('id')) {
			def mensajeSMIMEPadre = MensajeSMIME.get(params.id)
			if (mensajeSMIMEPadre) {
				def mensajeSMIME = MensajeSMIME.findWhere(smimePadre:mensajeSMIMEPadre,
					tipo: Tipo.FIRMA_EVENTO_RECLAMACION_VALIDADA)
				if (mensajeSMIME) {
					response.status = 200
					response.contentLength = mensajeSMIME.contenido.length
					response.setContentType("text/plain")
					response.outputStream <<  mensajeSMIME.contenido
					response.outputStream.flush()
					return false
				}
			}
			response.status = 404
			render message(code: 'mensajeSMIME.eventoNoEncontrado', args:[params.smimePadreId])
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
}