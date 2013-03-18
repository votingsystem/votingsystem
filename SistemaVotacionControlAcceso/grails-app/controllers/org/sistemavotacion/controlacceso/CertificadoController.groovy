package org.sistemavotacion.controlacceso

import java.io.IOException;
import java.io.InputStream;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.controlacceso.modelo.*
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.cert.X509Certificate;
import java.util.Set;
import grails.util.*
/**
* @infoController Servicio de Certificados
* @descController Servicios relacionados con los certificados manejados por la aplicación
* 
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class CertificadoController {
	
	def grailsApplication
	def firmaService

	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/certificado'.
	 */
	def index () { }
	
	/**
	 * @httpMethod GET
	 * @return La cadena de certificación en formato PEM del servidor
	 */
	def cadenaCertificacion () {
		try {
			File cadenaCertificacion = grailsApplication.mainContext.getResource(
				grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion).getFile();
			response.status = Respuesta.SC_OK
			response.outputStream << cadenaCertificacion.getBytes() // Performing a binary stream copy
			response.outputStream.flush()
			return false
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
			Certificado certificado;
			Certificado.withTransaction{
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
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
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
		if (params.long('usuarioId')) {
			Usuario usuario = Usuario.get(params.long('usuarioId'))
			if(!usuario) {
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: 'error.UsuarioNoEncontrado', args:[params.usuarioId])
				return false
			}
			Certificado certificado
			Certificado.withTransaction {
				certificado = Certificado.findWhere(
					usuario:usuario, estado:Certificado.Estado.OK)
			}
			log.debug("certificado: ${certificado.id}")
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
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * Servicio de consulta de los certificados emisores de certificados
	 * de voto para una votación.
	 *
	 * @httpMethod GET
	 * @param idEvento el identificador de la votación que se desea consultar.
	 * @return Devuelve la cadena de certificación, en formato PEM, con la que se generan los
	 * 			certificados de los votos.
	 */
	def certificadoCA_DeEvento () {
		if (params.int('idEvento')) {
			EventoVotacion eventoVotacion;
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.get(params.int('idEvento'))
			}
			log.debug "certificadoCA_DeEvento - eventoVotacion: '${eventoVotacion.id}'"
			if (eventoVotacion) {
				Certificado certificadoCA
				EventoVotacion.withTransaction { 
					certificadoCA = Certificado.findWhere(
						eventoVotacion:eventoVotacion, tipo:Certificado.Tipo.RAIZ_VOTOS)
				}
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
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'eventNotFound', args:[params.idEvento])
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). Servicio que añade Autoridades de Confianza.<br/>
	 * Sirve para poder validar los certificados enviados en las simulaciones.
	 * 
	 * @httpMethod POST
	 * @param pemCertificate certificado en formato PEM de la Autoridad de Confianza que se desea añadir.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def addCertificateAuthority () {
		//===== 
		/*if(!Environment.TEST.equals(Environment.current)) {
			def msg = message(code: "msg.servicioEntornoTest")
			log.debug msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}*/
		//=====
		log.debug "Environment --- TEST ---"
		Respuesta respuesta = firmaService.addCertificateAuthority(
			params.pemCertificate?.getBytes(), request.getLocale())
		log.debug("addCertificateAuthority - codigo estado: ${respuesta.codigoEstado} - mensaje: ${respuesta.mensaje}")
		response.status = respuesta.codigoEstado
		render respuesta.mensaje
		return false
	}

	/**
	 * @httpMethod GET
	 * @return Los certificados en formato PEM de las Autoridades Certificadoras en las que
	 *         confía la aplicación.
	 */
	def trustedCerts () {
		Set<X509Certificate> trustedCerts = firmaService.getTrustedCerts()
		log.debug("number trustedCerts: ${trustedCerts.size()}")
		response.setContentType("text/plain")
		for(X509Certificate cert : trustedCerts) {
			byte[] pemCert = CertUtil.fromX509CertToPEM (cert)
			response.outputStream <<  pemCert
		}
		response.outputStream.flush()
		return false
	}
}