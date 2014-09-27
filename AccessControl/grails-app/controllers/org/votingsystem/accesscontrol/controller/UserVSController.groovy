package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.RepresentationDocumentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.ApplicationContextHolder

import java.security.cert.X509Certificate

class UserVSController {
	
	def subscriptionVSService
        
    def index() {}

	
	/**
	 * Servicio que sirve para añadir usuarios de pruebas.
	 * SOLO DISPONIBLES EN ENTORNOS DE DESARROLLO.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/user]
	 * @param [userCert] Certificado de usuario en formato PEM
	 * 
	 * @requestContentType [application/x-x509-ca-cert]
	 * 
	 */
	def save() {
		if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		String pemCert = "${request.getInputStream()}"
		Collection<X509Certificate> userCertCollection = CertUtil.fromPEMToX509CertCollection(pemCert.getBytes())
		X509Certificate userCert = userCertCollection?.toArray()[0]
		if(userCert) {
			UserVS userVS = UserVS.getUserVS(userCert);
			ResponseVS responseVS = subscriptionVSService.checkUser(userVS, request.locale)
			responseVS.userVS.type = UserVS.Type.USER
			responseVS.userVS.representative = null
			responseVS.userVS.representativeMessage = null
			responseVS.userVS.representativeRegisterDate = null
			responseVS.userVS.metaInf = null
			UserVS.withTransaction { responseVS.userVS.save() }
            return [responseVS : responseVS]
		} else return [responseVS : new ResponseVS(ResponseVS.SC_ERROR, message(code:"nullCertificateErrorMsg"))]
	}
	
	
	/**
	 *
	 * Servicio que sirve para prepaparar la base de usuarios
	 * antes de lanzar simulaciones.
	 * SOLO DISPONIBLES EN ENTORNOS DE DESARROLLO.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/user/prepareUserBaseData]
	 *
	 */
	def prepareUserBaseData() {
		if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		def usersVS = UserVS.findAll()
        usersVS.each { user ->
			user.type = UserVS.Type.USER
			user.representative = null
			user.representativeMessage = null
			user.representativeRegisterDate = null
			user.metaInf = null
			def repDocsFromUser
			RepresentationDocumentVS.withTransaction {
				repDocsFromUser = RepresentationDocumentVS.findAllWhere(userVS:user)
				repDocsFromUser.each { repDocFromUser ->
					repDocFromUser.state = RepresentationDocumentVS.State.CANCELLED
					repDocFromUser.dateCanceled = Calendar.getInstance().getTime()
					repDocFromUser.save()
				}
			}
			String userId = String.format('%05d', user.id)
			render "prepareUserBaseData - user: ${userId} of ${usersVS.size()} - ${repDocsFromUser.size()} representations<br/>"
			log.info("prepareUserBaseData - user: ${userId} of ${usersVS.size()} - ${repDocsFromUser.size()} representations");
		}
		response.status = ResponseVS.SC_OK
		render "OK"
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