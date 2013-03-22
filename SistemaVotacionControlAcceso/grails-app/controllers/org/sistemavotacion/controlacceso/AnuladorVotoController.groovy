package org.sistemavotacion.controlacceso

import javax.mail.internet.MimeMessage
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
	
	
	/**
	 * Servicio que anula votos.
	 * 
	 * @httpMethod POST
	 * @param archivoFirmado El <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">anulador de voto</a>.
	 * @return Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.
	 */
    def guardarAdjuntandoValidacion () {
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
