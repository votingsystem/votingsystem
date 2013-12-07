package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContentTypeVS
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
        File certChain = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
        response.outputStream << certChain.getBytes() // Performing a binary stream copy
        response.outputStream.flush()
        return false
	}

	/**
	 * Servicio de consulta de certificates de voto.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/voteVS/hashHex/$hashHex]
	 * @param	[hashHex] Obligatorio. Hash en hexadecimal asociado al
	 *          certificado del voto consultado.
	 * @return El certificado en formato PEM.
	 */
	def voteVS () {
		if (params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertVoteBase64 = new String(hexConverter.unmarshal(params.hashHex))
			CertificateVS certificate;
			CertificateVS.withTransaction{certificate = CertificateVS.findWhere(hashCertVoteBase64:hashCertVoteBase64)}
			if (certificate) {
				ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
                params.responseVS = new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                    messageBytes: CertUtil.getPEMEncoded (certX509))
                return
			}
		}
        params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'certByHEXNotFound', args:[params.hashHex]))
	}
	
	/**
	 * Servicio de consulta de certificates de usuario.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/userVS/$userId]
	 * @param [userId] Obligatorio. El identificador en la base de datos del usuario.
	 * @return El certificado en formato PEM.
	 */
	def userVS () {
		UserVS userVS = UserVS.get(params.long('userId'))
		if(!userVS) {
            params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code:'userNotFounError',args:[params.userId]))
		} else {
            CertificateVS certificate
            CertificateVS.withTransaction{certificate = CertificateVS.findWhere(
                    userVS:userVS, state:CertificateVS.State.OK)}
            if (certificate) {
                ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
                X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
                params.responseVS = new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                        messageBytes: CertUtil.getPEMEncoded (certX509))
                return
            }
            params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code:'userWithoutCert',args:[params.userId]))
        }
	}
	
	/**
	 * Servicio de consulta de los certificados emisores de certificados
	 * de voto para una votación.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/eventCA/$eventVS_Id]
	 * @param [eventVS_Id] Obligatorio. El identificador en la base de datos de la votación que se desea consultar.
	 * @return Devuelve la cadena de certificación, en formato PEM, con la que se generan los
	 * 			certificados de los votos.
	 */
	def eventCA () {
		EventVSElection eventVSElection;
		EventVSElection.withTransaction {eventVSElection = EventVSElection.get(params.long('eventVS_Id'))}
		log.debug "certificateCA._EventVS - eventVSElection: '${eventVSElection.id}'"
		if (eventVSElection) {
			CertificateVS certificateCA
			EventVSElection.withTransaction {
				certificateCA = CertificateVS.findWhere(eventVSElection:eventVSElection,
                        type:CertificateVS.Type.VOTEVS_ROOT)
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(certificateCA.content)
			X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
            params.responseVS = new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                    messageBytes: CertUtil.getPEMEncoded (certX509))
		} else params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'eventVSNotFound', args:[params.eventVS_Id]))
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
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))
			return
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		signatureVSService.deleteTestCerts()
		params.responseVS = signatureVSService.addCertificateAuthority(
			"${request.getInputStream()}".getBytes(), request.getLocale())
	}

	/**
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/trustedCerts]
	 * @return Los certificados en formato PEM de las Autoridades Certificadoras en las que
	 *         confía la aplicación.
	 */
	def trustedCerts () {
		Set<X509Certificate> trustedCerts = signatureVSService.getTrustedCerts()
        params.responseVS = new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                messageBytes: CertUtil.getPEMEncoded (trustedCerts))
	}
	
	def deleteTestCerts() {
		params.responseVS = signatureVSService.deleteTestCerts()
	}

}