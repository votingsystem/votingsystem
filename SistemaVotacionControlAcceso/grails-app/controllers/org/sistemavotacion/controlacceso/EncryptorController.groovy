package org.sistemavotacion.controlacceso

import java.io.BufferedReader;
import java.io.FileReader;
import java.security.Key
import java.security.KeyPair;
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter
import org.sistemavotacion.controlacceso.modelo.MensajeSMIME
import org.sistemavotacion.controlacceso.modelo.Respuesta
import grails.converters.JSON;
import java.io.BufferedReader
import org.bouncycastle.util.encoders.Base64;
import java.security.KeyFactory;
import grails.util.Environment
import org.sistemavotacion.smime.SMIMEMessageWrapper
import org.sistemavotacion.utils.*
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.util.*

class EncryptorController {
	
	def grailsApplication
	def firmaService
	def timeStampService

    def index() { 
		if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
			VotingSystemApplicationContex.instance.environment)) {
			String msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		if(!params.requestBytes) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		byte[] solicitud = params.requestBytes
		//log.debug("Solicitud" + new String(solicitud))
		def mensajeJSON = JSON.parse(new String(solicitud))
		if(!mensajeJSON.publicKey) {
			String msg = message(code: "publicKeyMissingErrorMsg")
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
	    byte[] decodedPK = Base64.decode(mensajeJSON.publicKey);
	    PublicKey receiverPublic =  KeyFactory.getInstance("RSA").
	            generatePublic(new X509EncodedKeySpec(decodedPK));
	    //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
		mensajeJSON.message="Hello '${mensajeJSON.from}' from server"
		params.receiverPublicKey = receiverPublic
		response.setContentType("multipart/encrypted")
		params.respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK)
		params.responseBytes = mensajeJSON.toString().getBytes()
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
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		MensajeSMIME mensajeSMIMEReq = params.mensajeSMIMEReq
		if(!mensajeSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		response.contentType = "${grailsApplication.config.pkcs7SignedContentType}"
			
		SMIMEMessageWrapper smimeMessage = mensajeSMIMEReq.getSmimeMessage()
		
		String fromUser = "EncryptorController"
		String toUser = "MultiSignatureTestClient"
		String subject = "Multisigned response"
		SMIMEMessageWrapper smimeMessageResp = firmaService.getMultiSignedMimeMessage(
			fromUser, toUser, smimeMessage, subject)
		
		//smimeMessageResp.init()
		
		//ByteArrayOutputStream messageBaos = new ByteArrayOutputStream();
		//smimeMessageResp.writeTo(messageBaos)
		//log.debug("========= ${new String(messageBaos.toByteArray())}")

		
		MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(tipo:Tipo.TEST,
			contenido:smimeMessage.getBytes())
		
		params.respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK,
			mensajeSMIME:mensajeSMIMEResp, tipo:Tipo.TEST)
	}
	
	def validateTimeStamp() {
		if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
			VotingSystemApplicationContex.instance.environment)) {
			String msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		MensajeSMIME mensajeSMIMEReq = params.mensajeSMIMEReq
		if(!mensajeSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		SMIMEMessageWrapper smimeMessage = mensajeSMIMEReq.getSmimeMessage()
		Usuario usuario = mensajeSMIMEReq.getUsuario()
		//Date fechaFin = DateUtils.getDateFromString("2014-01-01 00:00:00")
		
		Evento evento
		Evento.withTransaction{
			evento = Evento.get(1)
			//evento.fechaFin = fechaFin
		}
		
		Respuesta timeStampVerification = timeStampService.validateToken(
			usuario.getTimeStampToken(), evento, request.locale)
		
		timeStampVerification.tipo = Tipo.TEST
		params.respuesta = timeStampVerification
		if(Respuesta.SC_OK == timeStampVerification.codigoEstado) {
			response.status = Respuesta.SC_OK
			render timeStampVerification.mensaje
		}
		
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
