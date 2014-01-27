package org.votingsystem.ticket.controller

import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.HttpHelper

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.cert.X509Certificate

/**
* @infoController Servicio de Certificados
* @descController Servicios relacionados con los certificados manejados por la aplicación
* 
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class CertificateVSController {
	
	def grailsApplication
	def signatureVSService


	/**
	 * Servicio de consulta de certificates de tickets.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/ticket/hashHex/$hashHex]
	 * @param	[hashHex] Obligatorio. Hash en hexadecimal asociado al
	 *          certificado del voto consultado.
	 * @return El certificado en formato PEM.
	 */
	def ticket () {
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
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Servicio que añade Autoridades de Confianza.<br/>
	 * Sirve para poder validar los certificados enviados en las simulaciones.
	 * 
	 * @httpMethod [POST]
	 * @param pemCertificate certificado en formato PEM de la Autoridad de Confianza que se desea añadir.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def addCertificateAuthority () {
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		return [responseVS:signatureVSService.addCertificateAuthority(
			"${request.getInputStream()}".getBytes(), request.getLocale())]
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
     * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Servicio que borra los certificados generados en una
     * simualcion
     * @httpMethod [GET]
     * @serviceURL [/certificateVS/deleteTestCerts]
     * @return Los certificados en formato PEM de las Autoridades Certificadoras en las que
     *         confía la aplicación.
     */
	def deleteTestCerts() {
        if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "serviceDevelopmentModeMsg"))]
        }
        log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		return [responseVS:signatureVSService.deleteTestCerts()]
	}

    /**
     * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Servicio que da de alta el certificado del servidor
     * en el servidor que recibe como parámetro.
     * @httpMethod [GET]
     * @serviceURL [/certificateVS/sendCA]
     * @param [serverURL] Obligatorio. la URL base del servidor en el que se desea dar de alta el certificado.
     */
    def sendCA() {
        if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "serviceDevelopmentModeMsg"))]
        }
        log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
        if(!params.serverURL) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                message(code: "missingParamErrorMsg", args:["serverURL"]))]
        else {
            X509Certificate serverCert = signatureVSService.getServerCert()
            byte[] rootCACertPEMBytes = CertUtil.getPEMEncoded (serverCert);
            //String serviceURL = ActorVS.getRootCAServiceURL(params.serverURL)
            String serviceURL = "${params.serverURL}/certificateVS/addCertificateAuthority"
            return [responseVS:HttpHelper.getInstance().sendData(rootCACertPEMBytes, ContentTypeVS.X509_CA,
                    serviceURL)]
        }

    }


}