package org.sistemavotacion.centrocontrol

import java.io.IOException;
import java.io.InputStream;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.centrocontrol.modelo.*
import org.springframework.context.ApplicationContext;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.cert.X509Certificate;
/**
* @infoController Servicio de Certificados
* @descController Servicios relacionados con los certificados manejados por la aplicación
* 
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* */
class CertificadoController {

	def firmaService

	/**
	 * @httpMethod [GET]
	 * @return La cadena de certificación del servidor
	 */
	def cadenaCertificacion () {
		try {
			response.outputStream << firmaService.getCadenaCertificacion().getBytes()
			response.outputStream.flush()
			return
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_EJECUCION
			render ex.getMessage()
			return false 
		}
	}
	
	/**
	 * Servicio de consulta de certificados de voto.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/certificado/voto/$hashHex] 
	 *
	 * @param	[hashHex] Obligatorio. Hash en hexadecimal asociado al
	 *          certificado del voto consultado.
	 * @return El certificado de voto en formato PEM.
	 */
	def voto () {
		if (params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertificadoVotoBase64 = new String(
				hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertificadoVotoBase64: ${hashCertificadoVotoBase64}"
			def certificado
			Certificado.withTransaction {
				certificado = Certificado.findWhere(hashCertificadoVotoBase64:
					hashCertificadoVotoBase64)
			}
			if (certificado) {
				response.status = Respuesta.SC_OK
				ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
				response.setContentType("text/plain")
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'certificado.certificadoHexNotFound',
				args:[params.hashHex])
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:
			["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * Servicio de consulta de certificados de usuario.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificado/usuario/$userId] 
	 * @param [userId] Obligatorio. El identificador en la base de datos del usuario.	 
	 * @return El certificado en formato PEM.
	 */
	def usuario () {
		Usuario usuario = Usuario.get(params.long('userId'))
		if(!usuario) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.UsuarioNoEncontrado', args:[params.userId])
			return false
		}
		def certificado
		Certificado.withTransaction {
			certificado = Certificado.findWhere(usuario:usuario, 
				estado:Certificado.Estado.OK)
		}
		if (certificado) {
			response.status = Respuesta.SC_OK
			ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
			X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
			byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
			response.setContentType("text/plain")
			response.contentLength = pemCert.length
			response.outputStream <<  pemCert
			response.outputStream.flush()
			return false
		}
		response.status = Respuesta.SC_NOT_FOUND
		render message(code: 'error.UsuarioSinCertificado',
			args:[params.userId])
		return false
	}
	
	/**
	 * Servicio de consulta de los certificados con los que se firman los 
	 * certificados de los voto en una votación.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/certificado/eventCA] 
     * @param [eventAccessControlURL] Opcional. La url en el Control de Acceso  del evento 
     *        cuyos votos fueron emitidos por el certificado consultado.
	 * @return Devuelve la cadena de certificación, en formato PEM, con la que se generan los 
	 * 			certificados de los votos.
	 */
	def eventCA () {
		EventoVotacion eventoVotacion
		if (params.eventAccessControlURL){
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.findWhere(url:params.eventAccessControlURL)
			}
			if (eventoVotacion) {
				Certificado certificadoCA
				Certificado.withTransaction {
					certificadoCA = Certificado.findWhere(eventoVotacion:eventoVotacion,
						actorConIP:eventoVotacion.controlAcceso, esRaiz:true)
				}
				if(certificadoCA) {
					response.status = Respuesta.SC_OK
					ByteArrayInputStream bais = new ByteArrayInputStream(certificadoCA.contenido)
					X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
					byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
					response.setContentType("text/plain")
					response.contentLength = pemCert.length
					response.outputStream <<  pemCert
					response.outputStream.flush()
					return false
				}
			}
		}
		response.status = Respuesta.SC_NOT_FOUND
		render  message(code: 'eventoByURLNotFound', args:[params.eventAccessControlURL])
		return false
	}
}