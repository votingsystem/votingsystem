package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*;

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
    def index () {
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
        render message(code: 'mensajeSMIME.eventoNoEncontrado', args:[params.id])
        return false
    }
	
}