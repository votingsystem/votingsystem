package org.votingsystem.accesscontrol.controller

import grails.converters.JSON

import javax.mail.internet.MimeMessage
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

/**
 * @infoController Anulación de votos
 * @descController Servicios relacionados con la anulación de votos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class AnuladorVotoController {
	
	def votoService
	
	
	/**
	 * Servicio de consulta de votos anulados.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/anuladorVoto/$hashHex]
	 * @param [hashHex] El hash en Hexadecimal del certificado del voto anulado.
	 * @return La anulación de voto firmada por el usuario.
	 */
	def index () {
		if(params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertificadoVotoBase64 = new String(
				hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertificadoVotoBase64: '${hashCertificadoVotoBase64}'"
			AnuladorVoto anuladorVoto = null
			AnuladorVoto.withTransaction{
				anuladorVoto = AnuladorVoto.findWhere(
					hashCertificadoVotoBase64:hashCertificadoVotoBase64)
			}
			if(!anuladorVoto) {
				response.status = ResponseVS.SC_NOT_FOUND
				render message(code: 'voteNotFound', args:[params.id])
			} else {
				Map anuladorvotoMap = votoService.getAnuladorVotoMap(anuladorVoto)
				render anuladorvotoMap as JSON
			}
		} else {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'error.PeticionIncorrectaHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
		}
		return false
	}
	
	/**
	 * Servicio que anula votos.
	 * 
     * @httpMethod [POST]
	 * @serviceURL [/anuladorVoto]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Documento correspondiente al 
	 *              <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">anulador de voto</a>
	 * 				firmado y cifrado	 
     * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] 
	 * @return Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor. La respuesta viaja cifrada.
	 */
    def post () {
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		params.receiverCert = messageSMIMEReq.getUsuario().getCertificate()
		ResponseVS respuesta = votoService.processCancel(messageSMIMEReq, request.getLocale())
		if (ResponseVS.SC_OK == respuesta.statusCode) {
			response.setContentType(ContentTypeVS.SIGNED_AND_ENCRYPTED)		
        }
		params.respuesta = respuesta
    }

	/**
	 * Servicio que devuelve la información de la anulación de un voto a partir del
	 * identifiacador del voto en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/anuladorVoto/voto/${id}]
	 * @param [id] Obligatorio. Identificador del voto en la base de datos
	 * @responseContentType [application/json]
	 * @return Documento JSON con la información del voto solicitado.
	 */
	def get() {
		Voto voto
		Map  anuladorvotoMap
		Voto.withTransaction {
			voto = Voto.get(params.long('id'))
		}
		if(!voto) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}
		AnuladorVoto anulador
		AnuladorVoto.withTransaction {
			anulador = AnuladorVoto.findWhere(voto:voto)
		}
		if(!anulador) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
		} else {
			anuladorvotoMap = votoService.getAnuladorVotoMap(anulador)
			render anuladorvotoMap as JSON
		}
		return false
	}
}
