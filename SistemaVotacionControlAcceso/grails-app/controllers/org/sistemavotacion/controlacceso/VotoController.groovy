package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.*
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import grails.converters.JSON
import java.security.cert.X509Certificate;
/**
 * @infoController Servicio de Votos
 * @descController Servicio que procesa los votos recibidos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class VotoController {

	def votoService
	def grailsApplication
	def encryptionService
        
    def index() {}
	
	/**
	 * Servicio que recoge los votos enviados por los Centrol de Control
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/voto]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio. El archivo voto firmado por el
	 *        <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de Voto.</a>
	 *        y el certificado del Centro de Control.
	 * @responseContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] 
	 * @return  <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-Voto">El recibo del voto.</a>
	 */
    def save() { 
		MensajeSMIME mensajeSMIMEReq = params.mensajeSMIMEReq
		if(!mensajeSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg 
			return false
		}
	    Respuesta respuesta = votoService.validateVote(
			mensajeSMIMEReq, request.getLocale())
		if (Respuesta.SC_OK == respuesta.codigoEstado) {
			X509Certificate controlCenterCert = respuesta.certificado
			params.receiverCert = controlCenterCert
			response.setContentType("application/x-pkcs7-signature")
		}
		params.respuesta = respuesta
	}
	
	/**
	 * Servicio que devuelve la información de un voto a partir del identificador
	 * del mismo en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/voto/${id}]
	 * @param [id] Obligatorio. Identificador del voto en la base de datos
	 * @responseContentType [application/json]
	 * @return Documento JSON con la información del voto solicitado.
	 */
	def get() {
		Voto voto
		Map  votoMap
		Voto.withTransaction {
			voto = Voto.get(params.long('id'))
			if(voto) votoMap = votoService.getVotoMap(voto)
		}
		if(!voto) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}

		render votoMap as JSON
		return false
	}
	
}
