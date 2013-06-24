package org.sistemavotacion.controlacceso

import java.io.IOException;
import java.io.InputStream;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.controlacceso.modelo.*
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.cert.X509Certificate;
import java.util.Set;
import grails.util.*
import org.sistemavotacion.utils.*

/**
* @infoController Servicio de Certificados
* @descController Servicios relacionados con los certificados manejados por la aplicación
* 
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class CertificadoController {
	
	def grailsApplication
	def firmaService
	
	/**
	 * @httpMethod [GET]
	 * @return La cadena de certificación en formato PEM del servidor
	 */
	def cadenaCertificacion () {
		try {
			File cadenaCertificacion = grailsApplication.mainContext.getResource(
				grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion).getFile();
			response.outputStream << cadenaCertificacion.getBytes() // Performing a binary stream copy
			response.outputStream.flush()
			return false
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR
			render ex.getMessage()
			return false
		}
	}

	/**
	 * Servicio de consulta de certificados de voto.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificado/voto/hashHex/$hashHex] 
	 * @param	[hashHex] Obligatorio. Hash en hexadecimal asociado al
	 *          certificado del voto consultado.
	 * @return El certificado en formato PEM.
	 */
	def voto () {
		if (params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertificadoVotoBase64 = new String(
				hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertificadoVotoBase64: ${hashCertificadoVotoBase64}"
			Certificado certificado;
			Certificado.withTransaction{
				certificado = Certificado.findWhere(hashCertificadoVotoBase64:
					hashCertificadoVotoBase64)
			}
			if (certificado) {
				ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
		}
		response.status = Respuesta.SC_NOT_FOUND
		render message(code: 'certificado.certificadoHexNotFound',
			args:[params.hashHex])
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
		Certificado certificado
		Certificado.withTransaction {
			certificado = Certificado.findWhere(
				usuario:usuario, estado:Certificado.Estado.OK)
		}
		log.debug("certificado: ${certificado.id}")
		if (certificado) {
			ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
			X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
			byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
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
	 * Servicio de consulta de los certificados emisores de certificados
	 * de voto para una votación.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificado/eventCA/$idEvento] 
	 * @param [idEvento] Obligatorio. El identificador en la base de datos de la votación que se desea consultar.
	 * @return Devuelve la cadena de certificación, en formato PEM, con la que se generan los
	 * 			certificados de los votos.
	 */
	def eventCA () {
		EventoVotacion eventoVotacion;
		EventoVotacion.withTransaction {
			eventoVotacion = EventoVotacion.get(params.long('idEvento'))
		}
		log.debug "certificadoCA_DeEvento - eventoVotacion: '${eventoVotacion.id}'"
		if (eventoVotacion) {
			Certificado certificadoCA
			EventoVotacion.withTransaction { 
				certificadoCA = Certificado.findWhere(
					eventoVotacion:eventoVotacion, tipo:Certificado.Tipo.RAIZ_VOTOS)
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(certificadoCA.contenido)
			X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
			byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
			response.contentLength = pemCert.length
			response.outputStream <<  pemCert
			response.outputStream.flush()
			return false
		}
		response.status = Respuesta.SC_NOT_FOUND
		render message(code: 'eventNotFound', args:[params.idEvento])
		return false
	}

	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Servicio que añade Autoridades de Confianza.<br/>
	 * Sirve para poder validar los certificados enviados en las simulaciones.
	 * 
	 * @httpMethod [POST]
	 * @param pemCertificate certificado en formato PEM de la Autoridad de Confianza que se desea añadir.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def addCertificateAuthority () {
		if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
			VotingSystemApplicationContex.instance.environment)) {
			def msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		firmaService.deleteTestCerts()
		Respuesta respuesta = firmaService.addCertificateAuthority(
			"${request.getInputStream()}".getBytes(), request.getLocale())
		log.debug("addCertificateAuthority - status: ${respuesta.codigoEstado} - msg: ${respuesta.mensaje}")
		response.status = respuesta.codigoEstado
		render respuesta.mensaje
		return false
	}

	/**
	 * @httpMethod [GET]
	 * @serviceURL [/certificado/trustedCerts] 
	 * @return Los certificados en formato PEM de las Autoridades Certificadoras en las que
	 *         confía la aplicación.
	 */
	def trustedCerts () {
		Set<X509Certificate> trustedCerts = firmaService.getTrustedCerts()
		log.debug("number trustedCerts: ${trustedCerts.size()}")
		for(X509Certificate cert : trustedCerts) {
			byte[] pemCert = CertUtil.fromX509CertToPEM (cert)
			response.outputStream <<  pemCert
		}
		response.outputStream.flush()
		return false
	}
	
	def deleteTestCerts() {
		Respuesta respuesta = firmaService.deleteTestCerts()
		if(respuesta.codigoEstado == Respuesta.SC_OK) {
			render "OK"
		} else render "ERROR"
	}
}