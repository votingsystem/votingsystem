package org.votingsystem.accesscontrol.controller

import grails.converters.JSON

import java.security.MessageDigest
import java.security.cert.X509Certificate;
import java.util.Collection;

import org.votingsystem.util.DateUtils;
import org.votingsystem.util.StringUtils;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.votingsystem.accesscontrol.model.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.*

import grails.util.*

import org.votingsystem.groovy.util.*

class UserController {
	
	def subscripcionService
        
        def index() {}

	/**
	 *
	 * Servicio que sirve para comprobar el representante de un usuario
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/user/$nif/representative]
	 * @param [nif] NIF del usuario que se desea consultar.
	 * @responseContentType [application/json]
	 * @return Documento JSON con información básica del representante asociado
	 *         al usuario cuyo nif se pada como parámetro nif
	 */
	def representative() {
		String nif = StringUtils.validarNIF(params.nif)
		if(!nif) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'error.errorNif', args:[params.nif])
			return false
		}
		Usuario usuario = Usuario.findByNif(nif)
		if(!usuario) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'usuario.nifNoEncontrado', args:[nif])
			return false
		}
		String msg = null
		if(Usuario.Type.REPRESENTATIVE == usuario.type) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'userIsRepresentativeMsg', args:[nif])
			return false
		}
		if(!usuario.representative) {
			response.status = ResponseVS.SC_NOT_FOUND
			if(Usuario.Type.USER_WITH_CANCELLED_REPRESENTATIVE == usuario.type) {
				msg = message(code: 'userRepresentativeUnsubscribedMsg', args:[nif])
			} else msg = message(code: 'nifWithoutRepresentative', args:[nif])
			render msg
			return false
		} else {
			Usuario representative = usuario.representative
			String name = "${representative.nombre} ${representative.primerApellido}"
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
	 * @param [userCert] Certificado de usuario en formato PEM
	 * 
	 * @requestContentType [application/x-x509-ca-cert]
	 * 
	 */
	def save() {
		if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
			VotingSystemApplicationContex.instance.environment)) {
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
			Usuario usuario = Usuario.getUsuario(userCert);
			ResponseVS respuesta = subscripcionService.checkUser(usuario, request.locale)
			respuesta.usuario.type = Usuario.Type.USER
			respuesta.usuario.representative = null
			respuesta.usuario.representativeMessage = null
			respuesta.usuario.representativeRegisterDate = null
			respuesta.usuario.metaInf = null
			Usuario.withTransaction {
				respuesta.usuario.save()
			}
			response.status = respuesta.statusCode
			render respuesta.message
		} else {
			response.status = ResponseVS.SC_ERROR
			render message(code:"error.nullCertificate")
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
		if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
			VotingSystemApplicationContex.instance.environment)) {
			def msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		def users = Usuario.findAll()
		
		users.each { user -> 
			user.type = Usuario.Type.USER
			user.representative = null
			user.representativeMessage = null
			user.representativeRegisterDate = null
			user.metaInf = null
			user.info = null
			
			def repDocsFromUser
			RepresentationDocument.withTransaction {
				repDocsFromUser = RepresentationDocument.findAllWhere(user:user)
				repDocsFromUser.each { repDocFromUser ->
					repDocFromUser.state = RepresentationDocument.State.CANCELLED
					repDocFromUser.dateCanceled = DateUtils.getTodayDate()
					repDocFromUser.save()
				}
			}
			
			String userId = String.format('%05d', user.id)
			render "prepareUserBaseData - user: ${userId} of ${users.size()}" + 
				" - ${repDocsFromUser.size()} representations<br/>"
			log.info("prepareUserBaseData - user: ${userId} of ${users.size()}" + 
				" - ${repDocsFromUser.size()} representations");
		}
		response.status = ResponseVS.SC_OK
		render "OK"
	}
	
}