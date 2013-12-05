package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.RepresentationDocumentVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.StringUtils

import java.security.cert.X509Certificate

class UserVSController {
	
	def subscriptionVSService
        
        def index() {}

	/**
	 *
	 * Servicio que sirve para comprobar el representante de un userVS
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/user/$nif/representative]
	 * @param [nif] NIF del userVS que se desea consultar.
	 * @responseContentType [application/json]
	 * @return documento JSON con información básica del representante asociado
	 *         al userVS cuyo nif se pada como parámetro nif
	 */
	def representative() {
		String nif = NifUtils.validate(params.nif)
		if(!nif) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'nifWithErrors', args:[params.nif])
			return false
		}
		UserVS userVS = UserVS.findByNif(nif)
		if(!userVS) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'userVSNotFoundByNIF', args:[nif])
			return false
		}
		String msg = null
		if(UserVS.Type.REPRESENTATIVE == userVS.type) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'userIsRepresentativeMsg', args:[nif])
			return false
		}
		if(!userVS.representative) {
			response.status = ResponseVS.SC_NOT_FOUND
			if(UserVS.Type.USER_WITH_CANCELLED_REPRESENTATIVE == userVS.type) {
				msg = message(code: 'userRepresentativeUnsubscribedMsg', args:[nif])
			} else msg = message(code: 'nifWithoutRepresentative', args:[nif])
			render msg
			return false
		} else {
			UserVS representative = userVS.representative
			String name = "${representative.name} ${representative.firstName}"
			def resultMap = [representativeId: representative.id, representativeName:name,
				representativeNIF:representative.nif]
			render resultMap as JSON
			return false
		}
	}
	
	/**
	 *
	 * Servicio que sirve para añadir usuarios de pruebas.
	 * SOLO DISPONIBLES EN ENTORNOS DE DESARROLLO.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/user]
	 * @param [userCert] CertificateVS de userVS en formato PEM
	 * 
	 * @requestContentType [application/x-x509-ca-cert]
	 * 
	 */
	def save() {
		if(!EnvironmentVS.DEVELOPMENT.equals(
			ApplicationContextHolder.getEnvironment())) {
			def msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		String pemCert = "${request.getInputStream()}"
		Collection<X509Certificate> userCertCollection = CertUtil.fromPEMToX509CertCollection(pemCert.getBytes())
		X509Certificate userCert = userCertCollection?.toArray()[0]
		if(userCert) {
			UserVS userVS = UserVS.getUsuario(userCert);
			ResponseVS responseVS = subscriptionVSService.checkUser(userVS, request.locale)
			responseVS.userVS.type = UserVS.Type.USER
			responseVS.userVS.representative = null
			responseVS.userVS.representativeMessage = null
			responseVS.userVS.representativeRegisterDate = null
			responseVS.userVS.metaInf = null
			UserVS.withTransaction {
				responseVS.userVS.save()
			}
			response.status = responseVS.statusCode
			render responseVS.message
		} else {
			response.status = ResponseVS.SC_ERROR
			render message(code:"nullCertificateErrorMsg")
		}
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
		if(!EnvironmentVS.DEVELOPMENT.equals(
			ApplicationContextHolder.getEnvironment())) {
			def msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
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