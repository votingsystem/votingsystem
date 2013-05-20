package org.sistemavotacion.controlacceso

import grails.converters.JSON
import java.security.MessageDigest
import org.sistemavotacion.util.StringUtils;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.sistemavotacion.controlacceso.modelo.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.bouncycastle.util.encoders.Base64;

class UserController {

	/**
	 *
	 * Servicio que sirve para comprobar el representante de un usuario
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/user/$nif/representative]
	 * @param [nif] NIF del usuario que se desea consultar.
	 * @responseContentType [application/json]
	 * @return Documento JSON con información básica del representante asociado
	 *         al usuario cuyo nif se pada como parámetro nif
	 */
	def representative() {
		String nif = StringUtils.validarNIF(params.nif)
		if(!nif) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.errorNif', args:[params.nif])
			return false
		}
		Usuario usuario = Usuario.findByNif(nif)
		if(!usuario) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'usuario.nifNoEncontrado', args:[nif])
			return false
		}
		String msg = null
		if(Usuario.Type.REPRESENTATIVE == usuario.type) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'userIsRepresentativeMsg', args:[nif])
			return false
		}
		if(!usuario.representative) {
			response.status = Respuesta.SC_NOT_FOUND
			if(Usuario.Type.USER_WITH_CANCELLED_REPRESENTATIVE == usuario.type) {
				msg = message(code: 'userRepresentativeUnsubscribedMsg', args:[nif])
			} else msg = message(code: 'nifWithoutRepresentative', args:[nif])
			render msg
			return false
		} else {
			Usuario representative = usuario.representative
			String name = "${representative.nombre} ${representative.primerApellido}"
			def resultMap = [representativeId: representative.id, representativeName:name,
				representativeNIF:representative.nif]
			render resultMap as JSON
			return false
		}
	}
}
