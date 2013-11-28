package org.votingsystem.controlcenter.controller

import org.votingsystem.model.CertificateVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.ApplicationContextHolder

import javax.xml.bind.annotation.adapters.HexBinaryAdapter

import org.votingsystem.model.ResponseVS

import java.security.cert.X509Certificate
/**
* @infoController Servicio de Certificados
* @descController Servicios relacionados con los certificados manejados por la aplicación
* 
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* */
class CertificateVSController {

	def signatureVSService

	/**
	 * @httpMethod [GET]
	 * @return La cadena de certificación del servidor
	 */
	def certChain () {
		try {
			response.outputStream << signatureVSService.getServerCertChain().getBytes()
			response.outputStream.flush()
			return
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = ResponseVS.SC_ERROR
			render ex.getMessage()
			return false 
		}
	}
	
	/**
	 * Servicio de consulta de certificados de voteVS.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/voteVS/$hashHex]
	 *
	 * @param	[hashHex] Obligatorio. Hash en hexadecimal asociado al
	 *          certificateVS del voteVS consultado.
	 * @return El certificateVS de voteVS en formato PEM.
	 */
	def voteVS () {
		if (params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertVoteBase64 = new String(
				hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertVoteBase64: ${hashCertVoteBase64}"
			def certificate
			CertificateVS.withTransaction {
				certificate = CertificateVS.findWhere(hashCertVoteBase64:
					hashCertVoteBase64)
			}
			if (certificate) {
				response.status = ResponseVS.SC_OK
				ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				byte[] pemCert = CertUtil.getPEMEncoded (certX509)
				response.setContentType(ContentTypeVS.TEXT)
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'certByHEXNotFound',
				args:[params.hashHex])
			return false
		}
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'requestWithErrorsHTML', args:
			["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * Servicio de consulta de certificados de userVS.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/userVS/$userId]
	 * @param [userId] Obligatorio. El identificador en la base de datos del userVS.
	 * @return El certificateVS en formato PEM.
	 */
	def userVS () {
		UserVS userVS = UserVS.get(params.long('userId'))
		if(!userVS) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'userNotFounError', args:[params.userId])
			return false
		}
		def certificate
		CertificateVS.withTransaction {
			certificate = CertificateVS.findWhere(userVS:userVS,
				state:CertificateVS.State.OK)
		}
		if (certificate) {
			response.status = ResponseVS.SC_OK
			ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
			X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
			byte[] pemCert = CertUtil.getPEMEncoded (certX509)
			response.setContentType(ContentTypeVS.TEXT)
			response.contentLength = pemCert.length
			response.outputStream <<  pemCert
			response.outputStream.flush()
			return false
		}
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: 'userWithoutCert',
			args:[params.userId])
		return false
	}
	
	/**
	 * Servicio de consulta de los certificados con los que se firman los 
	 * certificados de los voteVS en una votación.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/eventCA]
     * @param [eventAccessControlURL] Opcional. La url en el Control de Acceso  del evento 
     *        cuyos votesVS fueron emitidos por el certificateVS consultado.
	 * @return Devuelve la cadena de certificación, en formato PEM, con la que se generan los 
	 * 			certificados de los votesVS.
	 */
	def eventCA () {
		EventVS eventVSElection
		if (params.eventAccessControlURL){
			EventVS.withTransaction {
				eventVSElection = EventVS.findWhere(url:params.eventAccessControlURL)
			}
			if (eventVSElection) {
				CertificateVS certificateCA
				CertificateVS.withTransaction {
					certificateCA = CertificateVS.findWhere(eventVSElection:eventVSElection,
						actorVS:eventVSElection.accessControl, isRoot:true)
				}
				if(certificateCA) {
					response.status = ResponseVS.SC_OK
					ByteArrayInputStream bais = new ByteArrayInputStream(certificateCA.content)
					X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
					byte[] pemCert = CertUtil.getPEMEncoded (certX509)
					response.setContentType(ContentTypeVS.TEXT)
					response.contentLength = pemCert.length
					response.outputStream <<  pemCert
					response.outputStream.flush()
					return false
				}
			}
		}
		response.status = ResponseVS.SC_NOT_FOUND
		render  message(code: 'eventVSNotFoundByURL', args:[params.eventAccessControlURL])
		return false
	}
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Servicio que añade Autoridades de Confianza.<br/>
	 * Sirve para poder validar los certificados enviados en las simulaciones.
	 *
	 * @httpMethod [POST]
	 * @param pemCertificate certificateVS en formato PEM de la Autoridad de Confianza que se desea añadir.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def addCertificateAuthority () {
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
			def msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		signatureVSService.deleteTestCerts()
		ResponseVS responseVS = signatureVSService.addCertificateAuthority(
			"${request.getInputStream()}".getBytes(), request.getLocale())
		log.debug("addCertificateAuthority - status: ${responseVS.statusCode} - msg: ${responseVS.message}")
		response.status = responseVS.statusCode
		render responseVS.message
		return false
	}
}