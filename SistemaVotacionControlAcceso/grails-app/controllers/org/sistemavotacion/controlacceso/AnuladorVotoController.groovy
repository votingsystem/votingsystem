package org.sistemavotacion.controlacceso

import javax.mail.internet.MimeMessage
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.sistemavotacion.controlacceso.modelo.*;

/**
 * @infoController Anulación de votos
 * @descController Servicios relacionados con la anulación de votos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class AnuladorVotoController {
	
	def votoService

	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/anuladorVoto'.
	 */
	def index() { 
		redirect action: "restDoc"
	}
	/*
	def guardarAdjuntandoValidacionAsync () {
		Respuesta respuesta = votoService.validarAnulacion(params.smimeMessageReq)
		log.debug (respuesta.codigoEstado + " - mensaje: ${respuesta.mensaje}")
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
				ctx.response.contentLength = anuladorVoto.mensajeSMIME.contenido.length
				ctx.response.setContentType("text/plain")
				ctx.response.outputStream <<  anuladorVoto.mensajeSMIME.contenido
				ctx.response.outputStream.flush()
			} else {
				String codigoEstado = respuesta? respuesta.codigoEstado:500
				forward controller: "error${codigoEstado}", action: "procesar"
				return false
			}
			ctx.complete();
		} else {
			String codigoEstado = respuesta? respuesta.codigoEstado:500
			forward controller: "error${codigoEstado}", action: "procesar"
			return false
		}
	}*/
	
	
	/**
	 * Servicio que anula votos.
	 *
	 * @httpMethod GET
	 * @param hashCertVoteBase64 El hash en Base64 del certificado del voto anulado.
	 * @return La anulación firmada por el usuario.
	 */
	def obtener () {
		if(params.hashCertVoteHEX) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertificadoVotoBase64 = new String(
				hexConverter.unmarshal(params.hashCertVoteHEX))
			log.debug "hashCertificadoVotoBase64: '${hashCertificadoVotoBase64}'"
			AnuladorVoto anuladorVoto = null
			AnuladorVoto.withTransaction{
				anuladorVoto = AnuladorVoto.findWhere(
					hashCertificadoVotoBase64:hashCertificadoVotoBase64)
			}
			if (anuladorVoto) {
				response.status = Respuesta.SC_OK
                response.contentLength = anuladorVoto.mensajeSMIME.contenido.length
                response.setContentType("text/plain")
                response.outputStream <<  anuladorVoto.mensajeSMIME.contenido
                response.outputStream.flush()
				return false
			}
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'cancelVoteNotFoundMsg',
				args:[params.hashCertVoteBase64])
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * Servicio que anula votos.
	 * 
	 * @httpMethod POST
	 * @param archivoFirmado El <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">anulador de voto</a>.
	 * @return Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.
	 */
    def guardarAdjuntandoValidacion () {
		Respuesta respuesta = votoService.validarAnulacion(
			params.smimeMessageReq, request.getLocale())
		log.debug (respuesta.codigoEstado + " - mensaje: ${respuesta.mensaje}")
        if (Respuesta.SC_OK == respuesta.codigoEstado) {
			AnuladorVoto anuladorVoto = respuesta.anuladorVoto
			flash.receiverCert = respuesta.certificado
            Respuesta controlCenterResponse = votoService.sendVoteCancelationToControlCenter(
				anuladorVoto, request.getLocale())
            if (Respuesta.SC_OK == controlCenterResponse?.codigoEstado) {
				flash.respuesta = new Respuesta(codigoEstado:200,
					mensajeSMIMEValidado:anuladorVoto.mensajeSMIME)
				log.debug (" - receiverCert.getSubject: " + flash.receiverCert.getSubjectDN())
				flash.isEncryptedResponse = true
				return false
            } else {
				votoService.cancelVoteCancelation(anuladorVoto)
			}				
        }
		flash.respuesta = respuesta
		return false
    }
	
}
