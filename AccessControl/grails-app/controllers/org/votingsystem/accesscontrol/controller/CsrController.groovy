package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EnvironmentVS
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
	 * se ha validado la identidad del usuario.
	 *
	 * @httpMethod [GET]
	 * @param csrRequestId Identificador de la solicitud de certificación enviada previamente por el usuario.
	 * @return Si el sistema ha validado al usuario devuelve la solicitud de certificación firmada.
	 */
	def index() { 
		UserRequestCsrVS csrRequest
		UserRequestCsrVS.withTransaction {
			csrRequest = UserRequestCsrVS.findWhere(id:params.long('csrRequestId'), state:UserRequestCsrVS.State.OK)
		}
		if (csrRequest) {
			def certificate = CertificateVS.findWhere(userRequestCsrVS:csrRequest)
			if (certificate) {
				response.status = ResponseVS.SC_OK
				ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				File keyStoreFile = grailsApplication.mainContext.getResource(
					grailsApplication.config.VotingSystem.keyStorePath).getFile()
				
				String keyAlias = grailsApplication.config.VotingSystem.signKeysAlias
				String password = grailsApplication.config.VotingSystem.signKeysPassword
				KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
					FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
				Certificate[] certsServer =  keyStore.getCertificateChain(keyAlias);

                List<X509Certificate> certs = new ArrayList<X509Certificate>();
                certs.add(certX509)
                certs.addAll(Arrays.asList(certsServer))

                return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                        messageBytes: CertUtil.getPEMEncoded (certs))]
			} else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code: "csrGenerationErrorMsg"))]
		} else return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code: "csrRequestNotValidated"))]
	}
	
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS)<br/>
	 * Servicio para la creación de certificados de usuario.
	 * 
	 * @httpMethod [POST]
	 * @param csr Solicitud de certificación con los datos de usuario.
	 * @return Si todo es correcto devuelve un código de estado HTTP 200 y el identificador 
	 *         de la solicitud en la base de datos.
	 */
	def request() {
        if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
        }
		String consulta = "${request.getInputStream()}"
		if (!consulta) {
            return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                    args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))]
		} else return [responseVS:csrService.saveUserCSR(consulta.getBytes(), request.getLocale())]
	}
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS).
	 *
	 * Servicio que firma solicitudes de certificación de usuario.<br/>
	 *
	 * TODO - Hacer las validaciones sólo sobre solicitudes firmadas electrónicamente
	 * por personal dado de alta en la base de datos.
	 *
	 * @httpMethod [POST]
	 * @requestContentType [application/x-pkcs7-signature] Obligatorio. documento
	 * firmadoArchivo firmado en formato SMIME en cuyo content
	 * se encuentran los datos de la solicitud que se desea validar.
	 * <code>{deviceId:"000000000000000", phone:"15555215554", nif:"1R" }</code>
	 * 
	 * @return Si todo es correcto devuelve un código de estado HTTP 200.
	 */
	def validacion() {
        if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
        }
		MessageSMIME messageSMIME = request.messageSMIMEReq
		if(!messageSMIME) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
		}
		List<String> admins = grailsApplication.config.VotingSystem.adminsDNI
		UserVS userVS = messageSMIME.getUserVS()
		def docValidacionJSON = JSON.parse(messageSMIME.getSmimeMessage().getSignedContent())
		SMIMEMessageWrapper smimeMessageReq = messageSMIME.getSmimeMessage()
		if (admins.contains(userVS.nif) || userVS.nif.equals(docValidacionJSON.nif)) {
			DeviceVS dispositivo = DeviceVS.findWhere(deviceId: docValidacionJSON.deviceId)
			if (!dispositivo?.userVS) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code:"csrRequestNotFound",
                        args: [smimeMessageReq.getSignedContent()]))]
			}
			if(dispositivo.userVS.nif != userVS.nif) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code: "userWithoutPrivilegesToValidateCSR", args: [userVS.nif]))]
			}
			UserRequestCsrVS csrRequest = UserRequestCsrVS.findWhere(userVS:dispositivo.userVS,
				state:UserRequestCsrVS.State.PENDING);
			if (!csrRequest) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code: "userCSRNotFoundMsg", args: [userVSMovil.nif]))]
			}
            return [responseVS:csrService.signCertUserVS(csrRequest, request.getLocale())]
		} else return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                message(code: "userWithoutPrivilegesToValidateCSR", args: [userVS.nif]))]
	}
	
	/**
     * ==================================================
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
	 * ==================================================
	 * 
	 * Servicio que signatureVS solicitudes de certificación de usuario.<br/>
	 * 
	 * @httpMethod [POST]
	 * @requestContentType [application/json] documento JSON con los datos del usuario
	 * <code>{deviceId:"000000000000000", phone:"15555215554", nif:"1R" }</code>
	 * @return Si todo es correcto devuelve un código de estado HTTP 200.
	 */
	def validate() {
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		String requestStr = "${request.getInputStream()}"
        log.debug "requestStr: ${requestStr}"
		if (!requestStr) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render(view:"index")
			return false	
		}
		def dataJSON = JSON.parse(requestStr)
		DeviceVS device = DeviceVS.findByDeviceId(dataJSON?.deviceId?.trim())
		if (!device) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "csrRequestNotFound", args:["deviceId: ${dataJSON?.deviceId}"]))]
		}
		UserVS userVS
		String validatedNIF = NifUtils.validate(dataJSON?.nif)
		if(validatedNIF) userVS = UserVS.findByNif(validatedNIF)
		if (!userVS) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "csrRequestNotFound", args: ["nif: ${validatedNIF}"]))]
		}
		UserRequestCsrVS csrRequest
		UserRequestCsrVS.withTransaction{
			csrRequest = UserRequestCsrVS.findByDeviceVSAndUserVSAndState(
				device, userVS, UserRequestCsrVS.State.PENDING);
		}
		if (!csrRequest) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "csrRequestNotFound", args: [requestStr]))]
		} else return [responseVS:csrService.signCertUserVS(csrRequest, request.getLocale())]
	}
	
}