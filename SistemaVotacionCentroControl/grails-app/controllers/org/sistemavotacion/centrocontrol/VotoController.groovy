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
/**
 * @infoController Servicio de Votos
 * @descController Servicio que procesa los votos recibidos.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 * */
class VotoController {

    def votoService
    def httpService
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/voto'.
	 */
	def index() { 
		redirect action: "restDoc"
	}

	/**
	 * Servicio que recoge los votos enviados por los usuarios.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado	Obligatorio. El voto firmado por el 
	 *        <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de Voto.</a>
	 * @return  <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-Voto">El recibo del voto.</a>  
	 */
	def guardarAdjuntandoValidacion () {
		params.smimeMessageReq.initVoto()
		Respuesta respuesta = votoService.validarFirmaUsuario(
			params.smimeMessageReq, request.getLocale())
		if (Respuesta.SC_OK== respuesta.codigoEstado) {
			MimeMessage smimeMessage = params.smimeMessageReq
			respuesta = votoService.enviarVoto_A_ControlAcceso(
				smimeMessage, respuesta.evento, request.getLocale())
			if (Respuesta.SC_OK == respuesta?.codigoEstado) {
				response.status = Respuesta.SC_OK
				response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
				response.setContentType("text/plain")
				response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
				response.outputStream.flush()
			} else {
				log.debug "----- Error sending vote to Access Request Service - statusCode:'${respuesta?.codigoEstado}'" + 
					"-  message: '${respuesta.mensaje}'"
				response.status = respuesta.codigoEstado
				render message(code: 'accessRequestVoteErrorMsg', args:[respuesta.mensaje])
				return false
			}
		} else if (Respuesta.SC_ERROR_VOTO_REPETIDO == respuesta.codigoEstado){
			response.status = Respuesta.SC_ERROR_VOTO_REPETIDO
			response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
			response.setContentType("text/plain")
			response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
			response.outputStream.flush()
			return false
		} else {
			log.debug "----- statusCode: ${respuesta.codigoEstado} - mensaje:'${respuesta?.mensaje}'"
			response.status = respuesta?.codigoEstado
			render respuesta?.mensaje
			return false
		}
	}
	
	/**
	 * Servicio que devuelve la información de un voto a partir del  
	 * hash asociado al mismo
	 * @httpMethod GET
	 * @param hashCertificadoVotoHex	Obligatorio. Hash en hexadecimal asociado al voto. 
	 * @return Documento JSON con la información del voto solicitado.
	 */
	def obtener() {
		if (params.hashCertificadoVotoHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertificadoVotoBase64 = new String(
				hexConverter.unmarshal(params.hashCertificadoVotoHex))
			log.debug "hashCertificadoVotoBase64: ${hashCertificadoVotoBase64}"
			Certificado certificado
			Certificado.withTransaction {
				certificado = Certificado.findWhere(hashCertificadoVotoBase64:hashCertificadoVotoBase64)
			}
			if(!certificado) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'certificado.certificadoHexNotFound',
					args:[params.hashCertificadoVotoHex])
				return false
			}
			Voto voto
			def votoMap
			Voto.withTransaction {
				voto = Voto.findWhere(certificado:certificado)
				votoMap = [id:voto.id,
					hashCertificadoVotoBase64:voto.certificado.hashCertificadoVotoBase64,
					opcionDeEventoId:voto.opcionDeEvento.opcionDeEventoId,
					eventoVotacionId:voto.eventoVotacion.eventoVotacionId,
					estado:voto.estado.toString(),
					certificadoURL:"${grailsApplication.config.grails.serverURL}/certificado/certificadoDeVoto?hashCertificadoVotoHex=${params.hashCertificadoVotoHex}",
					votoSMIMEURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${voto.mensajeSMIME.id}"]
			}
			if(!voto) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'voto.votoConCertNotFound',
					args:[params.hashCertificadoVotoHex])
				return false
			}
			 
			if(Voto.Estado.ANULADO.equals(voto.estado)) {
				AnuladorVoto anuladorVoto
				AnuladorVoto.withTransaction {
					anuladorVoto = AnuladorVoto.findWhere(voto:voto)
				}
				votoMap.anuladorURL="${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${anuladorVoto?.mensajeSMIME?.id}"
			}
			response.status = Respuesta.SC_OK
			response.setContentType("application/json")
			render votoMap as JSON
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
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
	 
	 def guardarAdjuntandoValidacion () {
		 String codigoEstado
		 params.smimeMessageReq.initVoto()
		 Respuesta respuesta = votoService.validarFirmaUsuario(
			 params.smimeMessageReq, request.getLocale())
		 if (Respuesta.SC_OK== respuesta.codigoEstado) {
			 def ctx = startAsync()
			 ctx.setTimeout(10000);
			 MimeMessage smimeMessage = params.smimeMessageReq
			 EventoVotacion eventoVotacion = respuesta.evento
			 def future = callAsync {
				  return votoService.enviarVoto_A_ControlAcceso(
				  smimeMessage, eventoVotacion, request.getLocale())
			 }
			 respuesta = future.get()
			 if (Respuesta.SC_OK == respuesta?.codigoEstado) {
				 ctx.response.status = Respuesta.SC_OK
				 ctx.response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
				 ctx.response.setContentType("text/plain")
				 ctx.response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
				 ctx.response.outputStream.flush()
			 } else {
				 codigoEstado = respuesta? respuesta.codigoEstado:Respuesta.SC_ERROR_EJECUCION
				 forward controller: "error${codigoEstado}", action: "procesar"
				 return false
			 }
			 ctx.complete();
		 } else if (Respuesta.SC_ERROR_VOTO_REPETIDO == respuesta.codigoEstado){
			 response.status = Respuesta.SC_ERROR_VOTO_REPETIDO
			 response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
			 response.setContentType("text/plain")
			 response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
			 response.outputStream.flush()
			 return false
		 } else {
			 codigoEstado = respuesta? respuesta.codigoEstado:Respuesta.SC_ERROR_EJECUCION
			 forward controller: "error${codigoEstado}", action: "procesar"
			 return false
		 }
	 }*/
	
}