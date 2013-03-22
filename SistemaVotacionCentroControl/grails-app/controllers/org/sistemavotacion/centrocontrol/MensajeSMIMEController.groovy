package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*;

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
	def index() { 
		redirect action: "restDoc"
	}
	
	/**
	 * @httpMethod GET
	 * @param id	Obligatorio. Identificador del mensaje en la base de datos
	 * @return El mensaje solicitado.
	 */
    def obtener () {
        if (params.long('id')) {
            def mensajeSMIME;
			MensajeSMIME.withTransaction {
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
        render (view:"index")
        return false
    }
	
}