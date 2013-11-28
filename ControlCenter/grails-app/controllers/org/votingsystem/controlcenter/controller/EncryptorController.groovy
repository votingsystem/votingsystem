package org.votingsystem.controlcenter.controller

import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.TypeVS
import org.votingsystem.util.ApplicationContextHolder

import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

import org.bouncycastle.openssl.PEMWriter
import grails.converters.JSON
import org.bouncycastle.util.encoders.Base64
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper

class EncryptorController {
	
	def grailsApplication
	def signatureVSService

    def index() { 
		if(!EnvironmentVS.DEVELOPMENT.equals(
			ApplicationContextHolder.getEnvironment())) {
			String msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		if(!params.requestBytes) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		byte[] solicitud = params.requestBytes
		//log.debug("Solicitud" + new String(solicitud))
		
		def messageJSON = JSON.parse(new String(solicitud))
		
		if(!messageJSON.publicKey) {
			String msg = message(code: "publicKeyMissingErrorMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		
	    byte[] decodedPK = Base64.decode(messageJSON.publicKey);
	    PublicKey receiverPublic =  KeyFactory.getInstance("RSA").
	            generatePublic(new X509EncodedKeySpec(decodedPK));
	    //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
		
		messageJSON.message="Hello '${messageJSON.from}' from server"
		
		params.receiverPublicKey = receiverPublic
		response.setContentType(ContentTypeVS.MULTIPART_ENCRYPTED)
		
		params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK)
		
		params.responseBytes = messageJSON.toString().getBytes()
		
	}
	
	
	/**
	 * Servicio para comprbar la creación de documentos con multifirma
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/getMultiSignedMessage]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio.
	 *                     Documento SMIME firmado.
	 * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
	 * @return  Recibo que consiste en el documento recibido con la firma añadida del servidor.
	 */
	def getMultiSignedMessage() {
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
			String msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
        response.contentType = ContentTypeVS.SIGNED
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
		
		String fromUser = "EncryptorController"
		String toUser = "MultiSignatureTestClient"
		String subject = "Multisigned response"
		SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
			fromUser, toUser, smimeMessage, subject)
		MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.TEST, content:smimeMessageResp.getBytes())
		params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIMEResp, type:TypeVS.TEST)
	}
	
	private getPemBytesFromKey(Key key) {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
		pemWrt.writeObject(key);
		pemWrt.close();
		bOut.close();
		return bOut.toByteArray()
	}
}
