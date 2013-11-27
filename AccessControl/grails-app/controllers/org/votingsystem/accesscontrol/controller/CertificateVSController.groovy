package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.CertificateVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.UserVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.CertUtil

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.cert.X509Certificate
/**
* @infoController Servicio de Certificados
* @descController Servicios relacionados con los certificates manejados por la aplicación
* 
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class CertificateVSController {
	
	def grailsApplication
	def signatureVSService
	
	/**
	 * @httpMethod [GET]
	 * @return La cadena de certificación en formato PEM del servidor
	 */
	def certChain () {
		try {
			File certChain = grailsApplication.mainContext.getResource(
				grailsApplication.config.VotingSystem.certChainPath).getFile();
			response.outputStream << certChain.getBytes() // Performing a binary stream copy
			response.outputStream.flush()
			return false
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = ResponseVS.SC_ERROR
			render ex.getMessage()
			return false
		}
	}

	/**
	 * Servicio de consulta de certificates de voteVS.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/voteVS/hashHex/$hashHex]
	 * @param	[hashHex] Obligatorio. Hash en hexadecimal asociado al
	 *          certificateVS del voteVS consultado.
	 * @return El certificateVS en formato PEM.
	 */
	def voteVS () {
		if (params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertVoteBase64 = new String(hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertVoteBase64: ${hashCertVoteBase64}"
			CertificateVS certificate;
			CertificateVS.withTransaction{
				certificate = CertificateVS.findWhere(hashCertVoteBase64:hashCertVoteBase64)
			}
			if (certificate) {
				ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				byte[] pemCert = CertUtil.getPEMEncoded (certX509)
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
		}
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: 'certByHEXNotFound', args:[params.hashHex])
		return false
	}
	
	/**
	 * Servicio de consulta de certificates de userVS.
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
		CertificateVS certificate
		CertificateVS.withTransaction {
			certificate = CertificateVS.findWhere(
				userVS:userVS, state:CertificateVS.State.OK)
		}
		log.debug("certificateVS: ${certificate.id}")
		if (certificate) {
			ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
			X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
			byte[] pemCert = CertUtil.getPEMEncoded (certX509)
			response.contentLength = pemCert.length
			response.outputStream <<  pemCert
			response.outputStream.flush()
			return false
		}
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: 'userWithoutCert', args:[params.userId])
		return false
	}
	
	/**
	 * Servicio de consulta de los certificados emisores de certificados
	 * de voteVS para una votación.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/eventCA/$eventVS_Id]
	 * @param [eventVS_Id] Obligatorio. El identificador en la base de datos de la votación que se desea consultar.
	 * @return Devuelve la cadena de certificación, en formato PEM, con la que se generan los
	 * 			certificados de los votos.
	 */
	def eventCA () {
		EventVSElection eventVSElection;
		EventVSElection.withTransaction {
			eventVSElection = EventVSElection.get(params.long('eventVS_Id'))
		}
		log.debug "certificateCA._EventVS - eventVSElection: '${eventVSElection.id}'"
		if (eventVSElection) {
			CertificateVS certificateCA
			EventVSElection.withTransaction {
				certificateCA = CertificateVS.findWhere(eventVSElection:eventVSElection,
                        type:CertificateVS.Type.VOTEVS_ROOT)
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(certificateCA.content)
			X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
			byte[] pemCert = CertUtil.getPEMEncoded (certX509)
			response.contentLength = pemCert.length
			response.outputStream <<  pemCert
			response.outputStream.flush()
			return false
		}
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: 'eventVSNotFound', args:[params.eventVS_Id])
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

	/**
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/trustedCerts]
	 * @return Los certificados en formato PEM de las Autoridades Certificadoras en las que
	 *         confía la aplicación.
	 */
	def trustedCerts () {
		Set<X509Certificate> trustedCerts = signatureVSService.getTrustedCerts()
		log.debug("number trustedCerts: ${trustedCerts.size()}")
		for(X509Certificate cert : trustedCerts) {
			byte[] pemCert = CertUtil.getPEMEncoded (cert)
			response.outputStream <<  pemCert
		}
		response.outputStream.flush()
		return false
	}
	
	def deleteTestCerts() {
		ResponseVS responseVS = signatureVSService.deleteTestCerts()
		if(responseVS.statusCode == ResponseVS.SC_OK) {
			render "OK"
		} else render "ERROR"
	}
}