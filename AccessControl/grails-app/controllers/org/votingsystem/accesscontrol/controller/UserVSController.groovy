package org.votingsystem.accesscontrol.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.RepresentationDocumentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.util.CertUtils

import java.security.cert.X509Certificate

class UserVSController {
	
	def subscriptionVSService
        
    def index() {}

	
	/**
	 * (DISPONIBLES EN ENTORNOS DE DESARROLLO) Servicio que añade usuarios de pruebas.
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
		Collection<X509Certificate> userCertCollection = CertUtils.fromPEMToX509CertCollection(pemCert.getBytes())
		X509Certificate userCert = userCertCollection?.toArray()[0]
		if(userCert) {
			UserVS userVS = UserVS.getUserVS(userCert);
			ResponseVS responseVS = subscriptionVSService.checkUser(userVS)
			responseVS.userVS.type = UserVS.Type.USER
			responseVS.userVS.representative = null
			responseVS.userVS.metaInf = null
			UserVS.withTransaction { responseVS.userVS.save() }
            return [responseVS : responseVS]
		} else return [responseVS : new ResponseVS(ResponseVS.SC_ERROR, message(code:"nullCertificateErrorMsg"))]
	}
	
	
	/**
	 * (DISPONIBLES SOLO EN ENTORNOS DE DESARROLLO) Servicio que sirve para prepaparar la base de usuarios
	 * antes de lanzar simulaciones.
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
		List<UserVS> usersVS = UserVS.findAll()
        usersVS.each { user ->
			user.type = UserVS.Type.USER
			user.representative = null
			user.metaInf = null
			List<RepresentationDocumentVS> repDocsFromUser
			RepresentationDocumentVS.withTransaction {
				repDocsFromUser = RepresentationDocumentVS.findAllWhere(userVS:user)
				repDocsFromUser.each { repDocFromUser ->
					repDocFromUser.dateCanceled = Calendar.getInstance().getTime()
					repDocFromUser.setState(RepresentationDocumentVS.State.CANCELLED).save()
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
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}