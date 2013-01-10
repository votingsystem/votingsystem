package org.sistemavotacion.centrocontrol

import javax.mail.internet.MimeMessage
import org.sistemavotacion.centrocontrol.modelo.*;

class AnuladorVotoController {
	
	def votoService

	def index = { }
	
    def guardar = {
		Respuesta respuesta = votoService.validarAnulacion(
			params.smimeMessageReq, request.getLocale())
		log.debug (respuesta.codigoEstado + " - mensaje: ${respuesta.mensaje}")
		response.status = respuesta.codigoEstado
		render respuesta.codigoEstado
		return false
    }
	
}
