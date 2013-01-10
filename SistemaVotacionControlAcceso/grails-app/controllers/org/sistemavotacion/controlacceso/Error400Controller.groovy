package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import grails.converters.JSON

class Error400Controller {

	def index = { }
	
    def procesar = { 
        response.status = 400
		//response.setContentType("text/plain")
		String mensaje
        Respuesta respuesta = flash.respuesta
        if (respuesta){ 
            log.debug "-- Tipo de Respuesta: ${respuesta.tipo.toString()}"
            switch (respuesta.tipo) {
                case Tipo.FIRMA_EVENTO_CON_ERRORES:
                    mensaje = message(code:"evento.signatureError")
                    break;
                case Tipo.PETICION_SIN_ARCHIVO:
                    mensaje = message(code:"evento.peticionSinArchivo")
                    break;
                case Tipo.EVENTO_CON_ERRORES: case Tipo.PETICION_CON_ERRORES:
                    mensaje = message(code:"evento.documentError", args:[respuesta.mensaje])
                    break;
            }
        } else {
            log.error "# Se están recibiendo errores sin respuesta asociada #"
            mensaje = message(code: 'error.PeticionIncorrecta')
        }
		render mensaje
		return false
    }
	
}
