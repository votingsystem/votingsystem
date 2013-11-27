package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.util.StringUtils
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.DeviceVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.UserRequestCsrVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.FileUtils
import org.votingsystem.util.NifUtils

import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.X509Certificate
/**
 * @infoController Validación de solicitudes de certificación
 * @descController Servicios relacionados con validación y signatureVS de certificados.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class CsrController {

	def csrService
	
	/**
	 * Servicio que devuelve las solicitudes de certificados firmadas una vez que
	 * se ha validado la identidad del userVS.
	 *
	 * @httpMethod [GET]
	 * @param idSolicitudCSR Identificador de la solicitud de certificación enviada previamente por el userVS.
	 * @return Si el sistema ha validado al userVS devuelve la solicitud de certificación firmada.
	 */
	def index() { 
		UserRequestCsrVS solicitudCSR
		UserRequestCsrVS.withTransaction {
			solicitudCSR = UserRequestCsrVS.findWhere(
				id:params.long('idSolicitudCSR'), state:UserRequestCsrVS.State.OK)
		}
		if (solicitudCSR) {
			def certificate = CertificateVS.findWhere(solicitudCSRUsuario:solicitudCSR)
			if (certificate) {
				response.status = ResponseVS.SC_OK
				ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				File keyStoreFile = grailsApplication.mainContext.getResource(
					grailsApplication.config.VotingSystem.keyStorePath).getFile()
				
				String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
				String password = grailsApplication.config.VotingSystem.signKeysPassword
				KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
					FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
				Certificate[] certsServer =  keyStore.getCertificateChain(aliasClaves);
				
				List<X509Certificate> certs =new ArrayList<X509Certificate>();
				certs.add(certX509);
				for(Certificate cert : certsServer) {
					certs.add((X509Certificate)cert);
				}
				
				log.debug("certs.size(): " + certs.size())
				
				
				byte[] pemCert = CertUtil.getPEMEncoded(certs)
				
				//byte[] pemCert = CertUtil.getPEMEncoded (certX509)
				response.setContentType(ContentTypeVS.TEXT)
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: "csr.ErrorGeneracionCert")
			return false
		} 
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: "csrRequestNotValidated")
		return false
	}
	
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS)<br/>
	 * Servicio para la creación de certificados de userVS.
	 * 
	 * @httpMethod [POST]
	 * @param csr Solicitud de certificación con los datos de userVS.
	 * @return Si todo es correcto devuelve un código de estado HTTP 200 y el identificador 
	 *         de la solicitud en la base de datos.
	 */
	def solicitar() {
        if(!EnvironmentVS.DEVELOPMENT.equals(
                ApplicationContextHolder.getEnvironment())) {
            def msg = message(code: "serviceDevelopmentModeMsg")
            log.error msg
            response.status = ResponseVS.SC_ERROR_REQUEST
            render msg
            return false
        }
		String consulta = "${request.getInputStream()}"
		if (!consulta) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'requestWithErrorsHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
			return false
		}
		ResponseVS responseVS = csrService.saveUserCSR(consulta.getBytes(), request.getLocale())
		response.status = responseVS.statusCode
		render responseVS.message
		return false;
	}
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS).
	 *
	 * Servicio que signatureVS solicitudes de certificación de userVS.<br/>
	 *
	 * TODO - Hacer las validaciones sólo sobre solicitudes firmadas electrónicamente
	 * por personal dado de alta en la base de datos.
	 *
	 * @httpMethod [POST]
	 * @requestContentType [application/x-pkcs7-signature] Obligatorio. PDFDocumentVS
	 * firmadoArchivo firmado en formato SMIME en cuyo content
	 * se encuentran los datos de la solicitud que se desea validar.
	 * <code>{deviceId:"000000000000000", phone:"15555215554", nif:"1R" }</code>
	 * 
	 * @return Si todo es correcto devuelve un código de estado HTTP 200.
	 */
	def validacion() {
        if(!EnvironmentVS.DEVELOPMENT.equals(
                ApplicationContextHolder.getEnvironment())) {
            def msg = message(code: "serviceDevelopmentModeMsg")
            log.error msg
            response.status = ResponseVS.SC_ERROR_REQUEST
            render msg
            return false
        }
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		List<String> administradores = Arrays.asList(
			grailsApplication.config.VotingSystem.adminsDNI.split(","))
		UserVS userVS = messageSMIME.getUserVS()
		def docValidacionJSON = JSON.parse(messageSMIME.getSmimeMessage().getSignedContent())
		SMIMEMessageWrapper smimeMessageReq = messageSMIME.getSmimeMessage()
		if (administradores.contains(userVS.nif) || userVS.nif.equals(docValidacionJSON.nif)) {
			String msg = message(code: "userWithCSRPrivileges", args: [userVS.nif])
			DeviceVS dispositivo = DeviceVS.findWhere(deviceId: docValidacionJSON.deviceId)
			if (!dispositivo?.userVS) {
				response.status = ResponseVS.SC_ERROR_REQUEST
				render message(code: "csr.solicitudNoEncontrada", args: [smimeMessageReq.getSignedContent()])
				return false
			}
			if(dispositivo.userVS.nif != userVS.nif) {
				log.debug "UserVS con nif:'${userVS.nif}' intentando validar deviceVS:" +
					"'${dispositivo.id}' con nif:'${dispositivo.userVS.nif}'"
				render message(code: "userWithoutPrivilegesToValidateCSR", args: [userVS.nif])
				return false
			}
			UserRequestCsrVS solicitudCSR = UserRequestCsrVS.findWhere(userVS:dispositivo.userVS,
				state:UserRequestCsrVS.State.PENDING);
			if (!solicitudCSR) {
				response.status = ResponseVS.SC_ERROR_REQUEST
				render message(code: "userCSRNotFoundMsg", args: [userVSMovil.nif])
				return false
			}
			ResponseVS csrValidationResponseVS = csrService.signCertUserVS(solicitudCSR, request.getLocale())
			if (ResponseVS.SC_OK == csrValidationResponseVS.statusCode) {
				response.status = ResponseVS.SC_OK
				render message(code: "csr.generacionCertOK")
			} else {
				response.status = csrValidationResponseVS.statusCode
				render csrValidationResponseVS.message
			}
			return false
		} else {
			String msg = message(code: "userWithoutPrivilegesToValidateCSR", args: [userVS.nif])
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
	}
	
	/**
     * ==================================================
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
	 * ==================================================
	 * 
	 * Servicio que signatureVS solicitudes de certificación de userVS.<br/>
	 * 
	 * @httpMethod [POST]
	 * @requestContentType [application/json] PDFDocumentVS JSON con los datos del userVS
	 * <code>{deviceId:"000000000000000", phone:"15555215554", nif:"1R" }</code>
	 * @return Si todo es correcto devuelve un código de estado HTTP 200.
	 */
	def validar() {
		if(!EnvironmentVS.DEVELOPMENT.equals(
			ApplicationContextHolder.getEnvironment())) {
			def msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		String consulta = "${request.getInputStream()}"
		if (!consulta) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render(view:"index")
			return false	
		}
		log.debug ("consulta: ${consulta}")
		def consultaJSON = JSON.parse(consulta)
		DeviceVS dispositivo = DeviceVS.findByDeviceId(consultaJSON?.deviceId?.trim())
		if (!dispositivo) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: "csr.solicitudNoEncontrada", args: 
				["deviceId: ${consultaJSON?.deviceId}"])
			return false
		}
		UserVS userVS
		String nifValidado = NifUtils.validate(consultaJSON?.nif)
		if(nifValidado) userVS = UserVS.findByNif(nifValidado)
		if (!userVS) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: "csr.solicitudNoEncontrada", args: ["nif: ${nifValidado}"])
			return false
		}
		UserRequestCsrVS solicitudCSR
		UserRequestCsrVS.withTransaction{
			solicitudCSR = UserRequestCsrVS.findByDispositivoAndUsuarioAndState(
				dispositivo, userVS, UserRequestCsrVS.State.PENDING);
		}
		if (!solicitudCSR) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: "csr.solicitudNoEncontrada", args: [consulta])
			return false
		}
		ResponseVS csrValidationResponseVS = csrService.signCertUserVS(
			solicitudCSR, request.getLocale())
		if (ResponseVS.SC_OK == csrValidationResponseVS.statusCode) {
			response.status = ResponseVS.SC_OK
			render message(code: "csr.generacionCertOK")
		} else {
			response.status = csrValidationResponseVS.statusCode
			render csrValidationResponseVS.message
		}
		return false
	}
	
}