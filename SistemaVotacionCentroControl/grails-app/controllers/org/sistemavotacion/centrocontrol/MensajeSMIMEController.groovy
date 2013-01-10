package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class MensajeSMIMEController {
	
	def index = {}
	
    def obtener = {
        if (params.long('id')) {
            def mensajeSMIME;
			MensajeSMIME.withTransaction {
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
        render (view:"index")
        return false
    }
	
}