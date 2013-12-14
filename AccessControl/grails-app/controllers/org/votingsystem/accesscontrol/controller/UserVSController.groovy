package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.RepresentationDocumentVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.NifUtils
import org.votingsystem.util.StringUtils

import java.security.cert.X509Certificate

class UserVSController {
	
	def subscriptionVSService
        
        def index() {}

	/**
	 *
	 * Servicio que sirve para comprobar el representante de un usuario
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/user/$nif/representative]
	 * @param [nif] NIF del usuario que se desea consultar.
	 * @responseContentType [application/json]
	 * @return documento JSON con información básica del representante asociado
	 *         al usuario cuyo nif se pada como parámetro nif
	 */
	def representative() {
		String nif = NifUtils.validate(params.nif)
		if(nif) {
            UserVS userVS = UserVS.findByNif(nif)
            if(userVS) {
                String msg = null
                if(UserVS.Type.REPRESENTATIVE == userVS.type) {
                    return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                            message(code: 'userIsRepresentativeMsg', args:[nif]))]
                } else {
                    if(!userVS.representative) {
                        if(UserVS.Type.USER_WITH_CANCELLED_REPRESENTATIVE == userVS.type) {
                            msg = message(code: 'userRepresentativeUnsubscribedMsg', args:[nif])
                        } else msg = message(code: 'nifWithoutRepresentative', args:[nif])
                        return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND, msg)]
                    } else {
                        UserVS representative = userVS.representative
                        String name = "${representative.name} ${representative.firstName}"
                        def resultMap = [representativeId: representative.id, representativeName:name,
                                representativeNIF:representative.nif]
                        render resultMap as JSON
                    }
                }
            } else {
                return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'userVSNotFoundByNIF', args:[nif]))]
            }
		} else return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: 'nifWithErrors', args:[params.nif]))]
	}
	
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
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
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
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
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
					repDocFromUser.dateCanceled = DateUtils.getTodayDate()
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
	
}