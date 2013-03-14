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
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* */
class CertificadoController {

	def firmaService

	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/certificado'.
	 */
	def index () { }

	/**
	 * @httpMethod GET
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
	 * @param	hashCertificadoVotoHex Obligatorio. Hash en hexadecimal asociado al
	 *          certificado de voto que se desea consultar.
	 * @httpMethod GET
	 * @return El certificado en formato PEM.
	 */
	def certificadoDeVoto () {
		if (params.hashCertificadoVotoHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertificadoVotoBase64 = new String(
				hexConverter.unmarshal(params.hashCertificadoVotoHex))
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
				args:[params.hashCertificadoVotoHex])
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
	 * @param	usuarioId Obligatorio. El identificador en la base de datos del usuario.
	 * @httpMethod GET
	 * @return El certificado en formato PEM.
	 */
	def certificadoUsuario () {
		def usuarioId
		if (params.int('usuarioId')) {
			Usuario usuario = Usuario.get(params.int('usuarioId'))
			if(!usuario) {
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: 'error.UsuarioNoEncontrado', args:[params.usuarioId])
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
				args:[params.usuarioId])
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:
			["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * Servicio de consulta de los certificados emisores de certificados
	 * de voto para una votación.
	 * 
	 * @httpMethod GET
	 * @param idEvento el identificador de la votación que se desea consultar.
	 * @param controlAccesoId el identificador en la base de datos del control de acceso en el 
	 * 		  que se publicó la votación.
	 * @return Devuelve la cadena de certificación, en formato PEM, con la que se generan los 
	 * 			certificados de los votos.
	 */
	def certificadoCA_DeEvento () {
		if (params.long('idEvento') && params.long('controlAccesoId')){
			log.debug "certificadoCA_DeEvento - idEvento: '${params.idEvento}' - controlAccesoId: '${params.controlAccesoId}'"
			ControlAcceso controlAcceso = ControlAcceso.get(params.controlAccesoId)
			if(!controlAcceso) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'controlAccesoNotFound', args:[params.controlAccesoId])
				return false 
			}
			EventoVotacion eventoVotacion
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.get(params.idEvento)
			}
			if(!eventoVotacion) {
				response.status = Respuesta.SC_NOT_FOUND
				render  message(code: 'eventoVotacion.eventoNotFound', args:[params.idEvento])
				return false
			}
			if (eventoVotacion && controlAcceso) {
				Certificado certificadoCA
				Certificado.withTransaction {
					certificadoCA = Certificado.findWhere(eventoVotacion:eventoVotacion,
						actorConIP:controlAcceso, esRaiz:true)
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
		response.status = Respuesta.SC_ERROR_PETICION
		render (view:"index")
		return false
	}
}