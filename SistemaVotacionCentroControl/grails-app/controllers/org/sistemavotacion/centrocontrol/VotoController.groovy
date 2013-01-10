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
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
* */
class VotoController {

    def votoService
    def httpService
	
	def index = { }
	
	/*
    def testAsync = {
		log.debug "Arranco controlador"
		def aCtx = startAsync()
		aCtx.setTimeout(5000);
		//aCtx.complete()		
		render "Todo ok"
	}
	
    def guardarAdjuntandoValidacion = {
    	String codigoEstado
		params.smimeMessageReq.initVoto() 
        Respuesta respuesta = votoService.validarFirmaUsuario(
        	params.smimeMessageReq, request.getLocale())
        if (200 == respuesta.codigoEstado) {
			def ctx = startAsync()
			ctx.setTimeout(10000);
			MimeMessage smimeMessage = params.smimeMessageReq
			EventoVotacion eventoVotacion = respuesta.evento
			def future = callAsync {
				 return votoService.enviarVoto_A_ControlAcceso(smimeMessage, eventoVotacion)
			}
            respuesta = future.get()
            if (200  == respuesta?.codigoEstado) {
				ctx.response.status = 200
				ctx.response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
				ctx.response.setContentType("text/plain")
				ctx.response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
				ctx.response.outputStream.flush()
            } else {
				codigoEstado = respuesta? respuesta.codigoEstado:500
				forward controller: "error${codigoEstado}", action: "procesar"
				return false
            }				
			ctx.complete();
        } else if (409 == respuesta.codigoEstado){
            response.status = 409
			response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
			response.setContentType("text/plain")
			response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
			response.outputStream.flush()
            return false
        } else {
			codigoEstado = respuesta? respuesta.codigoEstado:500
			forward controller: "error${codigoEstado}", action: "procesar"
            return false
        }
    }*/
	def guardarAdjuntandoValidacion = {
		params.smimeMessageReq.initVoto()
		Respuesta respuesta = votoService.validarFirmaUsuario(
			params.smimeMessageReq, request.getLocale())
		if (200 == respuesta.codigoEstado) {
			MimeMessage smimeMessage = params.smimeMessageReq
			EventoVotacion eventoVotacion = respuesta.evento
			respuesta = votoService.enviarVoto_A_ControlAcceso(smimeMessage, eventoVotacion)
			if (200  == respuesta?.codigoEstado) {
				response.status = 200
				response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
				response.setContentType("text/plain")
				response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
				response.outputStream.flush()
			} else {
				log.debug "----- Error enviando voto a Control de Acceso - Código estado:'${respuesta?.codigoEstado}'"
				respuesta.mensaje =  "Error enviando voto a Control de Acceso - ${respuesta.mensaje}"
				flash.respuesta = respuesta
				String codigoEstado = respuesta? respuesta.codigoEstado:500
				forward controller: "error${codigoEstado}", action: "procesar"
				return false
			}
		} else if (409 == respuesta.codigoEstado){
			response.status = 409
			response.contentLength = respuesta.voto.mensajeSMIME.contenido.length
			response.setContentType("text/plain")
			response.outputStream <<  respuesta.voto.mensajeSMIME.contenido
			response.outputStream.flush()
			return false
		} else {
			log.debug "----- respuesta.codigo: ${respuesta.codigoEstado} - mensaje:'${respuesta?.mensaje}'"
			response.status = respuesta?.codigoEstado
			render respuesta?.mensaje
			return false
		}
	}
	
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
				response.status = 404
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
				response.status = 404
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
			response.status = 200
			response.setContentType("application/json")
			render votoMap as JSON
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
		
		
		
	}
}