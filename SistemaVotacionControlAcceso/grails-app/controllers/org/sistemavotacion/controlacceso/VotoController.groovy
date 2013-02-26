package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.*
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import grails.converters.JSON

class VotoController {

	def votoService
	def grailsApplication
	
	def index() { }
	
    def procesar() { 
		MultipartFile multipartFile = ((MultipartHttpServletRequest) request)?.getFile(
			grailsApplication.config.SistemaVotacion.nombreEntidadFirmada);
		SMIMEMessageWrapper smimeMessageReq = SMIMEMessageWrapper.build(
						new ByteArrayInputStream(multipartFile?.getBytes()), 
						null, SMIMEMessageWrapper.Tipo.VOTO);
		Respuesta respuesta = votoService.validarFirmas(smimeMessageReq, request.getLocale())
		if (200 == respuesta.codigoEstado) {
			response.status = 200
			response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
			response.setContentType("text/plain")
			response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
			response.outputStream.flush()
			return false
		}
		response.status = 400
		render respuesta.mensaje
		return false
	}
	
}
