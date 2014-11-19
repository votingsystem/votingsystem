package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class EncryptorController {
	
	def grailsApplication
	def signatureVSService

    def index() { 
		if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		if(!params.requestBytes) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code:'requestWithoutFile'))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		
		def messageJSON = JSON.parse(new String(params.requestBytes, "UTF-8"))
		
		if(!messageJSON.publicKey) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code:'publicKeyMissingErrorMsg'))]
		}
		
	    byte[] decodedPK = Base64.getDecoder().decode(messageJSON.publicKey);
	    PublicKey receiverPublic =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
	    //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
		messageJSON.message="Hello '${messageJSON.from}' from '${grailsApplication.config.grails.serverURL}'"
        return [receiverPublicKey:receiverPublic, responseVS: new ResponseVS(statusCode:ResponseVS.SC_OK, contentType:
                ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: messageJSON.toString().getBytes())]
	}
	
	
	/**
	 * Servicio para comprbar la creación de documentos con multifirma
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/getMultiSignedMessage]
	 * @requestContentType [application/pkcs7-signature] Obligatorio.
	 *                     Documento SMIME firmado.
	 * @responseContentType [application/pkcs7-signature]. Recibo firmado por el sistema.
	 * @return  Recibo que consiste en el documento recibido con la firma añadida del servidor.
	 */
	def getMultiSignedMessage() {
		if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        response.contentType = ContentTypeVS.SIGNED.getName()
		String fromUser = "EncryptorController"
		String toUser = "MultiSignatureTestClient"
		String subject = "Multisigned response"
		SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned(
			fromUser, toUser, messageSMIME.getSMIME(), subject)
        return [responseVS : new ResponseVS(statusCode:ResponseVS.SC_OK, messageBytes: smimeMessageResp.getBytes(),
                type:TypeVS.TEST, contentType: ContentTypeVS.JSON_SIGNED)]
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
