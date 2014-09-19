package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.vicket.util.ApplicationContextHolder
import org.votingsystem.util.HttpHelper

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.cert.X509Certificate

/**
* @infoController Servicio de Certificados
* @descController Servicios relacionados con los certificados manejados por la aplicación
* 
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class CertificateVSController {
	
	def grailsApplication
	def signatureVSService
    def certificateVSService


	/**
	 * Servicio de consulta de certificates de vickets.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/model/hashHex/$hashHex]
	 * @param	[hashHex] Obligatorio. Hash en hexadecimal asociado al
	 *          certificado del voto consultado.
	 * @return El certificado en formato PEM.
	 */
	def vicket () {
		if (params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertVSBase64 = new String(hexConverter.unmarshal(params.hashHex))
			CertificateVS certificate;
			CertificateVS.withTransaction{certificate = CertificateVS.findWhere(hashCertVSBase64:hashCertVSBase64)}
			if (certificate) {
				X509Certificate certX509 = CertUtil.loadCertificate (certificate.content)
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
                X509Certificate certX509 = CertUtil.loadCertificate (certificate.content)

                log.debug("====== certX509.hashCode(): ${certX509.hashCode()}")

                return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                        messageBytes: CertUtil.getPEMEncoded (certX509))]
            } else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code:'userWithoutCert',args:[params.userId]))]
        }
	}

	/**
     * (Disponible sólo para administradores de sistema)
     *
     * Servicio que añade Autoridades de Confianza.<br/>
	 * 
	 * @httpMethod [POST]
	 * @param pemCertificate certificado en formato PEM de la Autoridad de Confianza que se desea añadir.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def addCertificateAuthority () {
        /*if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "serviceDevelopmentModeMsg"))]
        }
        log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
        return [responseVS:signatureVSService.addCertificateAuthority(
            "${request.getInputStream()}".getBytes(), request.getLocale())]*/
        if("POST".equals(request.method)) {
            MessageSMIME messageSMIMEReq = request.messageSMIMEReq
            if(!messageSMIMEReq) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
            }
            ResponseVS responseVS = certificateVSService.addCertificateAuthority(messageSMIMEReq, request.getLocale())
            return [responseVS:responseVS]
        }
        render(view:'newCertificateAuthority')
	}

    /**
     * Servicio disponible sólo para administradores de sistema
     *
     * @httpMethod [POST]
     * @serviceURL [/certificateVS/editCert]
     * @requestContentType [application/x-pkcs7-signature] Obligatorio. El archivo con los datos
     *                  datos del nuevo estado del certificado
     * @responseContentType [application/x-pkcs7-signature]
     */
    def editCert() {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = certificateVSService.editCert(messageSMIMEReq, request.getLocale())
        return [responseVS:responseVS]
    }

	/**
	 * @httpMethod [GET]
	 * @serviceURL [/certificateVS/certs]
     * @param [type] (opcional) El tipo (CertificateVS.Type) de certificado. Por defecto CertificateVS.Type.USER
     * @param [state] (opcional) El estado (CertificateVS.State) de certificado. Por defecto CertificateVS.State.OK
	 * @return Los certificados en formato PEM que cumplen con los criterios de la búsqueda
	 */
	def certs () {
        CertificateVS.Type type = CertificateVS.Type.USER
        CertificateVS.State state = CertificateVS.State.OK
        try {type = CertificateVS.Type.valueOf(params.type)} catch(Exception ex) {type = CertificateVS.Type.USER }
        try {state = CertificateVS.State.valueOf(params.state)} catch(Exception ex) { }
        if(request.contentType?.contains("pem") || request.contentType?.contains("json") || 'pem'.equals(params.format)) {
            List<CertificateVS> certList = null
            CertificateVS.withTransaction {
                Date dateFrom = null
                Date dateTo = null

                certList = CertificateVS.createCriteria().list(max: params.max, offset: params.offset) {
                    and {
                        eq("type", type)
                        eq("state", state)
                    }
                    if(params.searchText) {
                        or {
                            ilike('userVS.name', "%${params.searchText}%")
                            ilike('userVS.nif', "%${params.searchText}%")
                            ilike('userVS.firstName', "%${params.searchText}%")
                            ilike('userVS.lastName', "%${params.searchText}%")
                            ilike('userVS.description', "%${params.searchText}%")
                        }
                    }
                }
            }
            def resultList = []
            if(request.contentType?.contains("pem") || 'pem'.equals(params.format)) {
                certList.each {certItem ->
                    resultList.add(certItem.getX509Cert())
                }
                return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                        messageBytes: CertUtil.getPEMEncoded (resultList))]
            } else {
                certList.each {certItem ->
                    resultList.add(certificateVSService.getCertificateVSDataMap(certItem))
                }
                def resultMap = [certList:resultList, type:type.toString(), state:state.toString(),
                                 queryRecordCount: certList.totalCount, numTotalCerts:certList.totalCount]
                render resultMap as JSON
            }
            return false
        }
        render(view:'certs', model: [certsMap:[type:type.toString(), state:state.toString()]])
	}

    /**
     * Método que sirve para obtener un certificado de la lista de confianza del sitio
     * @httpMethod [GET]
     * @serviceURL [/certificateVS/cert/$numSerie]
     * @param [serialNumber] Obligatorio. El número de serie del certificado que se desea obtener
     * @return El certificado en formato PEM.
     */
    def cert () {
        if(params.long('serialNumber')) {
            CertificateVS certificate
            CertificateVS.withTransaction {
                certificate = CertificateVS.findWhere(serialNumber:params.long('serialNumber'))
            }
            if(certificate) {
                X509Certificate x509Cert = certificate.getX509Cert()
                if(request.contentType?.contains("pem") || 'pem'.equals(params.format)) {
                    response.setHeader("Content-Disposition", "inline; filename='trustedCert_${params.serialNumber}'")
                    if(x509Cert) return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                            messageBytes: CertUtil.getPEMEncoded (x509Cert))]
                } else {
                    def certMap = certificateVSService.getCertificateVSDataMap(certificate)
                    if(request.contentType?.contains("json")) {
                        render certMap as JSON
                    } else render(view:'cert', model: [certMap:certMap])
                }
            }
        }
        return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }

}