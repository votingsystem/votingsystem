package org.votingsystem.controlcenter.controller

import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.ApplicationContextHolder

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
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
        response.outputStream << signatureVSService.getServerCertChain().getBytes()
        response.outputStream.flush()
        return
	}
	
	/**
	 * Servicio de consulta de certificados de voto.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/voteVS/$hashHex]
	 *
	 * @param	[hashHex] Obligatorio. Hash en hexadecimal asociado al
	 *          certificado del voto consultado.
	 * @return El certificado de voto en formato PEM.
	 */
	def voteVS () {
		if (params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertVSBase64 = new String(hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertVSBase64: ${hashCertVSBase64}"
			def certificate
			CertificateVS.withTransaction {certificate = CertificateVS.findWhere(hashCertVSBase64:hashCertVSBase64)}
			if (certificate) {
				response.status = ResponseVS.SC_OK
				ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
                return [responseVS : new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                        messageBytes: CertUtil.getPEMEncoded (certX509))]
			}
		}
        return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'certByHEXNotFound', args:[params.hashHex]))]
	}
	
	/**
	 * Servicio de consulta de certificados de usuario.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/userVS/$userId]
	 * @param [userId] Obligatorio. El identificador en la base de datos del usuario.
	 * @return El certificado en formato PEM.
	 */
	def userVS () {
		UserVS userVS = UserVS.get(params.long('userId'))
		if(!userVS) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'userNotFounError', args:[params.userId])
			return false
		}
		def certificate
		CertificateVS.withTransaction {certificate=CertificateVS.findWhere(userVS:userVS, state:CertificateVS.State.OK)}
        if (certificate) {
            ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
            X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
            return [responseVS : new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                    messageBytes: CertUtil.getPEMEncoded (certX509))]
        }
        return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code:'userWithoutCert',args:[params.userId]))]
	}
	
	/**
	 * Servicio de consulta de los certificados con los que se firman los 
	 * certificados de los voto en una votación.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/eventCA]
     * @param [eventAccessControlURL] Opcional. La url en el Control de Acceso  del evento 
     *        cuyos votesVS fueron emitidos por el certificado consultado.
	 * @return Devuelve la cadena de certificación, en formato PEM, con la que se generan los 
	 * 			certificados de los votesVS.
	 */
	def eventCA () {
		EventVS eventVSElection
		if (params.eventAccessControlURL){
			EventVS.withTransaction { eventVSElection = EventVS.findWhere(url:params.eventAccessControlURL) }
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
					response.setContentType(ContentTypeVS.TEXT.getName())
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

}