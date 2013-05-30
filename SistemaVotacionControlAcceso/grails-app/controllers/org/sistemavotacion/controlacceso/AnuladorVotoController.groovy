package org.sistemavotacion.controlacceso

import grails.converters.JSON
import javax.mail.internet.MimeMessage
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.sistemavotacion.controlacceso.modelo.*;

/**
 * @infoController Anulación de votos ======
 * @descController Servicios relacionados con la anulación de votos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
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
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'voteNotFound', args:[params.id])
			} else {
				Map anuladorvotoMap = votoService.getAnuladorVotoMap(anuladorVoto)
				render anuladorvotoMap as JSON
			}
		} else {
			response.status = Respuesta.SC_ERROR_PETICION
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
		MensajeSMIME mensajeSMIMEReq = params.mensajeSMIMEReq
		if(!mensajeSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		params.receiverCert = mensajeSMIMEReq.getUsuario().getCertificate()
		Respuesta respuesta = votoService.processCancel(mensajeSMIMEReq, request.getLocale())
		if (Respuesta.SC_OK == respuesta.codigoEstado) {
			response.setContentType("${grailsApplication.config.pkcs7SignedContentType};" + 
				"${grailsApplication.config.pkcs7EncryptedContentType}")		
        }
		params.respuesta = respuesta
    }
	
	
	/*
	def postAsync () {
		Respuesta respuesta = votoService.validarAnulacion(params.mensajeSMIMEReq)
		if (200 == respuesta.codigoEstado) {
			def ctx = startAsync()
			ctx.setTimeout(10000);
			AnuladorVoto anuladorVoto = respuesta.anuladorVoto
			def future = callAsync {
				 return votoService.sendVoteCancelationToControlCenter(anuladorVoto)
			}
			respuesta = future.get()
			if (200  == respuesta?.codigoEstado) {
				ctx.response.status = 200
				ctx.response.setContentType("text/plain")
				ctx.response.contentLength = anuladorVoto.mensajeSMIME.contenido.length
				ctx.response.outputStream <<  anuladorVoto.mensajeSMIME.contenido
				ctx.response.outputStream.flush()
			}
			ctx.complete();
		}
		params.respuesta = respuesta
	}*/
	
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
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}
		AnuladorVoto anulador
		AnuladorVoto.withTransaction {
			anulador = AnuladorVoto.findWhere(voto:voto)
		}
		if(!anulador) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
		} else {
			anuladorvotoMap = votoService.getAnuladorVotoMap(anulador)
			render anuladorvotoMap as JSON
		}
		return false
	}
}
