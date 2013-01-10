package org.sistemavotacion.controlacceso

import org.sistemavotacion.exception.*
import org.sistemavotacion.controlacceso.modelo.*;
import grails.converters.JSON

class Error500Controller {

	def index = {}
	
    def procesar = {   
		response.status = 500
		//response.setContentType("text/plain")
		String mensaje
        Respuesta respuesta = flash.respuesta
		//if(request.exception) 
		//	log.error(request.exception.getMessage(), request.exception)
        if (!respuesta){
			log.error "# --- Se están recibiendo errores sin respuesta asociada #"
			mensaje = message(code: 'error.PeticionIncorrecta')
        } else {
			log.debug "-- Tipo de Respuesta: ${respuesta?.tipo?.toString()}"
			mensaje = respuesta.mensaje
		} 
        if (request.exception && !mensaje) mensaje = request.exception?.getMessage()
        render mensaje
        return false
    }
        
}
