package org.votingsystem.accesscontrol.controller

import org.bouncycastle.util.encoders.Base64
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.FileUtils
import org.votingsystem.util.HttpHelper

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
    def keyStoreService
	
	/**
	 * @httpMethod [GET]
	 * @return La cadena de certificación en formato PEM del servidor
	 */
	def certChain () {
        File certChainPEMFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                messageBytes:certChainPEMFile.getBytes())]
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
			String hashCertVSBase64 = new String(hexConverter.unmarshal(params.hashHex))
			CertificateVS certificate;
			CertificateVS.withTransaction{certificate = CertificateVS.findWhere(hashCertVSBase64:hashCertVSBase64)}
			if (certificate) {
				ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
                return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                    messageBytes: CertUtil.getPEMEncoded (certX509))]
			}
		}
        return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'certByHEXNotFound', args:[params.hashHex]))]
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
            return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code:'userNotFounError',args:[params.userId]))]
		} else {
            CertificateVS certificate
            CertificateVS.withTransaction{certificate = CertificateVS.findWhere(
                    userVS:userVS, state:CertificateVS.State.OK)}
            if (certificate) {
                ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
                X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
                return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                        messageBytes: CertUtil.getPEMEncoded (certX509))]
            } else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code:'userWithoutCert',args:[params.userId]))]
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
            return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                    messageBytes: CertUtil.getPEMEncoded (certX509))]
		} else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'eventVSNotFound', args:[params.eventVS_Id]))]
	}

	/**
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/trustedCerts]
	 * @return Los certificados en formato PEM de las Autoridades Certificadoras en las que
	 *         confía la aplicación.
	 */
	def trustedCerts () {
		Set<X509Certificate> trustedCerts = signatureVSService.getTrustedCerts()
        return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                messageBytes: CertUtil.getPEMEncoded (trustedCerts))]
	}

    /**
     * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Servicio que crea almacenes de claves de pruebas 'JKS'
     * @httpMethod [GET]
     * @serviceURL [/certificateVS/createKeystore]
     * @return Los certificados en formato PEM de las Autoridades Certificadoras en las que
     *         confía la aplicación.
     */
    def createKeystore() {
        if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "serviceDevelopmentModeMsg"))]
        }
        log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
        String password = (params.password)? params.password:"ABCDE"
        String givenName = (params.givenName)? params.givenName:"UserNameTestKeysStore"
        String surname = (params.surname)? params.surname:"UserSurnameTestKeysStore"
        String nif = (params.nif)?params.nif:"03455543T"
        ResponseVS responseVS = keyStoreService.generateUserTestKeysStore(givenName, surname, nif, password)
        if(ResponseVS.SC_OK == responseVS.statusCode) {
            byte[] resultBytes = KeyStoreUtil.getBytes(responseVS.data, password.toCharArray())
            File destFile = new File("${System.getProperty('user.home')}/${params.givenName}_${nif}.jks")
            destFile.createNewFile()
            FileUtils.copyStreamToFile(new ByteArrayInputStream(resultBytes), destFile);
            byte[] base64ResultBytes = Base64.encode(resultBytes)
            response.outputStream <<  base64ResultBytes
            response.outputStream.flush()
            return false

        } else return [responseVS:responseVS]
    }

}