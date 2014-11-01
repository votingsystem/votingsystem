package org.votingsystem.accesscontrol.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.NifUtils
import java.security.cert.X509Certificate

/**
 * @infoController Validación de solicitudes de certificación
 * @descController Servicios relacionados con validación y signatureVS de certificados.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class CsrController {

	def csrService
    def signatureVSService
	
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
		if (!csrRequest) return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code: "csrRequestNotValidated"))]
        def certificate = CertificateVS.findWhere(userRequestCsrVS:csrRequest)
        if (!certificate) return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code: "csrGenerationErrorMsg"))]
        X509Certificate certX509 = CertUtils.loadCertificate(certificate.content)
        List<X509Certificate> certs = Arrays.asList(certX509, signatureVSService.getServerCert());
        return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PEM,
                messageBytes: CertUtils.getPEMEncoded (certs))]
	}

    def validate() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS:csrService.signCertUserVS(messageSMIME)]
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
        if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
        }
		String csrRequest = "${request.getInputStream()}"
		if (!csrRequest) {
            return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                    args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))]
		} else return [responseVS:csrService.saveUserCSR(csrRequest.getBytes())]
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
	def validateDev() {
		if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
        def requestJSON = request.JSON
		if (!requestJSON) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,"missing json data")]
		DeviceVS device = DeviceVS.findByDeviceId(requestJSON.deviceId?.trim())
		if (!device)return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "csrRequestNotFound", args:["deviceId: ${dataJSON?.deviceId}"]))]
		UserVS userVS
		String validatedNIF = NifUtils.validate(requestJSON?.nif)
		if(validatedNIF) userVS = UserVS.findByNif(validatedNIF)
		if (!userVS) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "csrRequestNotFound", args: ["nif: ${validatedNIF}"]))]
		UserRequestCsrVS csrRequest
		UserRequestCsrVS.withTransaction{
			csrRequest = UserRequestCsrVS.findByDeviceVSAndUserVSAndState(
				device, userVS, UserRequestCsrVS.State.PENDING);
		}
		if (!csrRequest) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "csrRequestNotFound", args: [requestJSON.toString()]))]
		else return [responseVS:csrService.signCertUserVS(csrRequest)]
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}