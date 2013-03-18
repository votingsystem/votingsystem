package org.sistemavotacion.centrocontrol

import javax.mail.internet.MimeMessage
import org.sistemavotacion.centrocontrol.modelo.*;

/**
 * @infoController Anulación de Votos
 * @descController Servicios que permiten anular los votos de una votación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 * */
class AnuladorVotoController {
	
	def votoService

	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/anuladorVoto'
	 */
	def index () { }
	
	/**
	 * @httpMethod POST
	 * @param archivoFirmado <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">El anulador de voto</a>
	 * @return Recibo firmado con el certificado del servidor
	 */
    def guardar () {
		Respuesta respuesta = votoService.validarAnulacion(
			params.smimeMessageReq, request.getLocale())
		log.debug (respuesta.codigoEstado + " - mensaje: ${respuesta.mensaje}")
		response.status = respuesta.codigoEstado
		render respuesta.codigoEstado
		return false
    }
	
}
