package org.votingsystem.controlcenter.controller

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.*;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import org.votingsystem.model.ContextVS;
import java.util.Map;
import java.util.HashMap;
import javax.mail.internet.MimeMessage
import org.votingsystem.controlcenter.model.*
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
    def httpService
	def encryptionService
	
	
	/**
	 * Servicio que recoge los votos enviados por los usuarios.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/voto]
	 * @contentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio. El archivo de voto firmado por el
	 *        <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de Voto.</a>
	 * @return  <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-Voto">El recibo del voto.</a>
	 */
	def index() {
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		ResponseVS respuesta = votoService.validateVote(
			messageSMIMEReq, request.getLocale())
		if (ResponseVS.SC_OK == respuesta.statusCode) {
			X509Certificate certificadoVoto = respuesta.certificado
			params.receiverCert = certificadoVoto
			if(messageSMIMEReq.getUsuario())
				response.addHeader("representativeNIF", messageSMIMEReq.getUsuario().nif)
			response.setContentType(ContextVS.SIGNED_AND_ENCRYPTED_CONTENT_TYPE)
		}
		String voteURL = "${createLink(controller:'messageSMIME', absolute:'true')}/${respuesta?.messageSMIME?.id}" 
		response.setHeader('voteURL', voteURL)		
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
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}

		render votoMap as JSON
		return false
	}
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). 
	 * Servicio que devuelve los votos con errores de una votación
	 * @httpMethod [GET]
	 * @serviceURL [/errors/event/${id}]
	 * @param [id] Obligatorio. Identificador del evento en la base de datos
	 * del Control de Acceso
	 * @responseContentType [application/zip]
	 * @return Documento ZIP con los errores de una votación
	 */
	def errors() {
		if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
			VotingSystemApplicationContex.instance.environment)) {
			def msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		EventoVotacion event = EventoVotacion.getAt(params.long('id'))
		if(!event) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'evento.eventoNotFound', args:[params.id])
			return false
		}
		def errorMessages
		MessageSMIME.withTransaction {
			errorMessages = MessageSMIME.findAllByEventoAndTypeAndType(event, TypeVS.ERROR, TypeVS.VOTO_CON_ERRORES)
		}

		render errorMessages.size()
		return false
	}
	
	/**
	 * Servicio que devuelve la información de un voto a partir del  
	 * hash asociado al mismo
	 * @httpMethod [GET]
	 * @serviceURL [/voto/hashHex/$hashHex] 
	 * @param [hashHex] Obligatorio. Hash en hexadecimal asociado al voto. 
	 * @responseContentType [application/json]
	 * @return Documento JSON con la información del voto solicitado.
	 */
	def hashCertificadoVotoHex() { 
		if (params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertificadoVotoBase64 = new String(
				hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertificadoVotoBase64: ${hashCertificadoVotoBase64}"
			Certificado certificado
			Certificado.withTransaction {
				certificado = Certificado.findWhere(hashCertificadoVotoBase64:hashCertificadoVotoBase64)
			}
			if(!certificado) {
				response.status = ResponseVS.SC_NOT_FOUND
				render message(code: 'certificado.certificadoHexNotFound',
					args:[params.hashHex])
				return false
			}
			Voto voto
			def votoMap
			Voto.withTransaction {
				voto = Voto.findWhere(certificado:certificado)
				votoMap = votoService.getVotoMap(voto)
			}
			if(!voto) {
				response.status = ResponseVS.SC_NOT_FOUND
				render message(code: 'voto.votoConCertNotFound',
					args:[params.hashHex])
				return false
			}
			 
			if(Voto.Estado.ANULADO.equals(voto.estado)) {
				AnuladorVoto anuladorVoto
				AnuladorVoto.withTransaction {
					anuladorVoto = AnuladorVoto.findWhere(voto:voto)
				}
				votoMap.anuladorURL="${grailsApplication.config.grails.serverURL}/messageSMIME/${anuladorVoto?.messageSMIME?.id}"
			}
			response.status = ResponseVS.SC_OK
			response.setContentType("application/json")
			render votoMap as JSON
			return false
		}
		response.status = ResponseVS.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
		return false
	}

	
	/*
	 def testAsync () {
		 log.debug "Arranco controlador"
		 def aCtx = startAsync()
		 aCtx.setTimeout(ResponseVS.SC_ERROR0);
		 //aCtx.complete()
		 render "Todo ok"
	 }
	 
	 def post () {
	 	 MimeMessage smimeMessageReq = params.smimeMessageReq
		 Respuesta respuesta = votoService.validarFirmaUsuario(
			 smimeMessageReq, request.getLocale())
		 if (ResponseVS.SC_OK== respuesta.statusCode) {
			 def ctx = startAsync()
			 ctx.setTimeout(10000);
			 
			 EventoVotacion eventoVotacion = respuesta.eventVS
			 def future = callAsync {
				  return votoService.sendVoteToControlAccess(
				  smimeMessage, eventoVotacion, request.getLocale())
			 }
			 respuesta = future.get()
			 if (ResponseVS.SC_OK == respuesta?.statusCode) {
				 ctx.response.status = ResponseVS.SC_OK
				 ctx.response.setContentType(ContextVS.SIGNED_AND_ENCRYPTED_CONTENT_TYPE)
				 ctx.response.contentLength = respuesta.voto.messageSMIME.contenido.length
				 ctx.response.outputStream <<  respuesta.voto.messageSMIME.contenido
				 ctx.response.outputStream.flush()
			 } 
			 ctx.complete();
		 } else if (ResponseVS.SC_ERROR_VOTO_REPETIDO == respuesta.statusCode){
			 response.status = ResponseVS.SC_ERROR_VOTO_REPETIDO
			 response.contentLength = respuesta.voto.messageSMIME.contenido.length
			 response.outputStream <<  respuesta.voto.messageSMIME.contenido
			 response.outputStream.flush()
			 return false
		 }
	 }*/
	
}