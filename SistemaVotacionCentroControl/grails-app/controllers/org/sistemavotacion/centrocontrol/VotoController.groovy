package org.sistemavotacion.centrocontrol

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.sistemavotacion.smime.*;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.HashMap;
import javax.mail.internet.MimeMessage
import org.sistemavotacion.centrocontrol.modelo.*
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
			X509Certificate certificadoVoto = respuesta.certificado
			params.receiverCert = certificadoVoto
			if(mensajeSMIMEReq.getUsuario())
				response.addHeader("representativeNIF", mensajeSMIMEReq.getUsuario().nif)
			response.setContentType("${grailsApplication.config.pkcs7SignedContentType};" +
				"${grailsApplication.config.pkcs7EncryptedContentType}")
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
				response.status = Respuesta.SC_NOT_FOUND
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
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'voto.votoConCertNotFound',
					args:[params.hashHex])
				return false
			}
			 
			if(Voto.Estado.ANULADO.equals(voto.estado)) {
				AnuladorVoto anuladorVoto
				AnuladorVoto.withTransaction {
					anuladorVoto = AnuladorVoto.findWhere(voto:voto)
				}
				votoMap.anuladorURL="${grailsApplication.config.grails.serverURL}/mensajeSMIME/${anuladorVoto?.mensajeSMIME?.id}"
			}
			response.status = Respuesta.SC_OK
			response.setContentType("application/json")
			render votoMap as JSON
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
		return false
	}

	
	/*
	 def testAsync () {
		 log.debug "Arranco controlador"
		 def aCtx = startAsync()
		 aCtx.setTimeout(Respuesta.SC_ERROR_EJECUCION0);
		 //aCtx.complete()
		 render "Todo ok"
	 }
	 
	 def post () {
	 	 MimeMessage smimeMessageReq = params.smimeMessageReq
		 Respuesta respuesta = votoService.validarFirmaUsuario(
			 smimeMessageReq, request.getLocale())
		 if (Respuesta.SC_OK== respuesta.codigoEstado) {
			 def ctx = startAsync()
			 ctx.setTimeout(10000);
			 
			 EventoVotacion eventoVotacion = respuesta.evento
			 def future = callAsync {
				  return votoService.sendVoteToControlAccess(
				  smimeMessage, eventoVotacion, request.getLocale())
			 }
			 respuesta = future.get()
			 if (Respuesta.SC_OK == respuesta?.codigoEstado) {
				 ctx.response.status = Respuesta.SC_OK
				 ctx.response.setContentType("${grailsApplication.config.pkcs7SignedContentType};" +
					"${grailsApplication.config.pkcs7EncryptedContentType}")
				 ctx.response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
				 ctx.response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
				 ctx.response.outputStream.flush()
			 } 
			 ctx.complete();
		 } else if (Respuesta.SC_ERROR_VOTO_REPETIDO == respuesta.codigoEstado){
			 response.status = Respuesta.SC_ERROR_VOTO_REPETIDO
			 response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
			 response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
			 response.outputStream.flush()
			 return false
		 }
	 }*/
	
}