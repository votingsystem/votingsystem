package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*;
import grails.converters.JSON

class Error400Controller {

	def index = {}
	
    def procesar = { 
        response.status = 400
		//response.setContentType("text/plain")
		String mensaje
        Respuesta respuesta = flash.respuesta
        if (respuesta){
            log.debug "-- Tipo de Respuesta: ${respuesta.tipo.toString()}"
            switch (respuesta.tipo) {
                case Tipo.FIRMA_EVENTO_CON_ERRORES:
                    mensaje = message(code:"eventoVotacion.signatureError")
                    break;
                case Tipo.PETICION_SIN_ARCHIVO:
                    mensaje = message(code:"eventoVotacion.peticionSinEvento")
                    break;
                case Tipo.EVENTO_CON_ERRORES: case Tipo.PETICION_CON_ERRORES:
                    mensaje = message(code:"eventoVotacion.documentError", args:[respuesta.mensaje])
                    break;
                default:
                    log.debug "### Tipo de respuesta sin caso asociado ###"
					mensaje = respuesta.mensaje?respuesta.mensaje:respuesta.tipo.toString()
                    break;
            }
        } else {
            log.error "# Se están recibiendo errores sin respuesta asociada #"
            mensaje = message(code: 'error.PeticionIncorrecta')
        }
		log.debug "-- mensaje: ${mensaje}"
		render mensaje
		return false
    }
        
}
