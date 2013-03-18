package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.*
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import grails.converters.JSON

/**
 * @infoController Servicio de Votos
 * @descController Servicio que procesa los votos recibidos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 * */
class VotoController {

	def votoService
	def grailsApplication
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/voto'.
	 */
	def index() { }
	
	/**
	 * Servicio que recoge los votos enviados por lo Centrols de Control
	 *
	 * @httpMethod POST
	 * @param archivoFirmado	Obligatorio. El voto firmado por el
	 *        <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de Voto.</a>
	 *        y el certificado del Centro de Control.
	 * @return  <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-Voto">El recibo del voto.</a>
	 */
    def procesar() { 
		MultipartFile multipartFile = ((MultipartHttpServletRequest) request)?.getFile(
			grailsApplication.config.SistemaVotacion.nombreEntidadFirmada);
		SMIMEMessageWrapper smimeMessageReq = SMIMEMessageWrapper.build(
						new ByteArrayInputStream(multipartFile?.getBytes()), 
						null, SMIMEMessageWrapper.Tipo.VOTO);
		Respuesta respuesta = votoService.validarFirmas(smimeMessageReq, request.getLocale())
		response.status = respuesta.codigoEstado
		if (Respuesta.SC_OK == respuesta.codigoEstado) {
			response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
			response.setContentType("text/plain")
			response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
			response.outputStream.flush()
			return false
		}
		render respuesta.mensaje
		return false
	}
	
}
