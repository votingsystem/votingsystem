package org.sistemavotacion.controlacceso

import javax.mail.internet.MimeMessage
import org.sistemavotacion.controlacceso.modelo.*;

class AnuladorVotoController {
	
	def votoService

	def index() { }
	/*
	def guardarAdjuntandoValidacionAsync = {
		Respuesta respuesta = votoService.validarAnulacion(params.smimeMessageReq)
		log.debug (respuesta.codigoEstado + " - mensaje: ${respuesta.mensaje}")
		if (200 == respuesta.codigoEstado) {
			def ctx = startAsync()
			ctx.setTimeout(10000);
			AnuladorVoto anuladorVoto = respuesta.anuladorVoto
			def future = callAsync {
				 return votoService.enviarAnulacion_A_CentroControl(anuladorVoto)
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
	
	
    def guardarAdjuntandoValidacion = {
		Respuesta respuesta = votoService.validarAnulacion(params.smimeMessageReq, request.getLocale())
		log.debug (respuesta.codigoEstado + " - mensaje: ${respuesta.mensaje}")
        if (Respuesta.SC_OK == respuesta.codigoEstado) {
			AnuladorVoto anuladorVoto = respuesta.anuladorVoto
            respuesta = votoService.enviarAnulacion_A_CentroControl(anuladorVoto)
            if (200  == respuesta?.codigoEstado) {
				flash.respuesta = new Respuesta(codigoEstado:200,
					mensajeSMIMEValidado:anuladorVoto.mensajeSMIME)
				return false
            }				
        }
		flash.respuesta = respuesta
		return false
    }
	
}
