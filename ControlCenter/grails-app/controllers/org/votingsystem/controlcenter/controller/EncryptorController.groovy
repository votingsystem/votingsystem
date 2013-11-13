package org.votingsystem.controlcenter.controller

import java.io.BufferedReader;
import java.io.FileReader;
import java.security.Key
import java.security.KeyPair;
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter
import org.votingsystem.controlcenter.model.*
import org.votingsystem.model.ContextVS;
import grails.converters.JSON;
import java.io.BufferedReader
import org.bouncycastle.util.encoders.Base64;
import java.security.KeyFactory;
import grails.util.Environment
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.groovy.util.*

class EncryptorController {
	
	def grailsApplication
	def firmaService

    def index() { 
		if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
			VotingSystemApplicationContex.instance.environment)) {
			String msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		if(!params.requestBytes) {
			String msg = message(code:'evento.peticionSinArchivo')
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
		response.setContentType("multipart/encrypted")
		
		params.respuesta = new ResponseVS(statusCode:ResponseVS.SC_OK)
		
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
		if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
			VotingSystemApplicationContex.instance.environment)) {
			String msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		response.contentType = org.votingsystem.model.ContentTypeVS.SIGNED
			
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
		
		String fromUser = "EncryptorController"
		String toUser = "MultiSignatureTestClient"
		String subject = "Multisigned response"
		SMIMEMessageWrapper smimeMessageResp = firmaService.getMultiSignedMimeMessage(
			fromUser, toUser, smimeMessage, subject)

		MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.TEST,
			contenido:smimeMessageResp.getBytes())
		
		params.respuesta = new ResponseVS(statusCode:ResponseVS.SC_OK,
			data:data, type:TypeVS.TEST)
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
