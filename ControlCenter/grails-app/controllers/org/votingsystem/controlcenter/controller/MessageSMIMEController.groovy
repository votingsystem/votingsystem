package org.votingsystem.controlcenter.controller

import org.votingsystem.controlcenter.model.*;
import org.votingsystem.model.ResponseVS;

/**
 * @infoController Messages firmados
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
    def index () {
        def messageSMIME;
		MessageSMIME.withTransaction {
			messageSMIME = MessageSMIME.get(params.id)
		}
        if (messageSMIME) {
            response.status = ResponseVS.SC_OK
            response.contentLength = messageSMIME.contenido.length
            response.setContentType("text/plain")
            response.outputStream <<  messageSMIME.contenido
            response.outputStream.flush()
            return false
        }
        response.status = ResponseVS.SC_NOT_FOUND
        render message(code: 'messageSMIME.eventoNoEncontrado', args:[params.id])
        return false
    }
	
}