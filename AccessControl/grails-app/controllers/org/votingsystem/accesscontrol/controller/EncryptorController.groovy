package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.bouncycastle.openssl.PEMWriter
import org.bouncycastle.util.encoders.Base64
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.UserVS
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper

import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class EncryptorController {
	
	def grailsApplication
	def signatureVSService
	def timeStampVSService

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
	    PublicKey receiverPublic =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
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
	 *                     documento SMIME firmado.
	 * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
	 * @return  Recibo que consiste en el documento recibido con la signatureVS añadida del servidor.
	 */
	def getMultiSignedMessage() {
		if(!EnvironmentVS.DEVELOPMENT.equals(
			ApplicationContextHolder.getEnvironment())) {
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

		
		//ByteArrayOutputStream messageBaos = new ByteArrayOutputStream();
		//smimeMessageResp.writeTo(messageBaos)
		//log.debug("========= ${new String(messageBaos.toByteArray())}")
		
		MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.TEST, content:smimeMessageResp.getBytes())
		params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIMEResp, type:TypeVS.TEST)
	}
	
	def validateTimeStamp() {
		if(!EnvironmentVS.DEVELOPMENT.equals(
			ApplicationContextHolder.getEnvironment())) {
			String msg = message(code: "serviceDevelopmentModeMsg")
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
		UserVS userVS = messageSMIMEReq.getUserVS()
		//Date dateFinish = DateUtils.getDateFromString("2014-01-01 00:00:00")
		def msgJSON = JSON.parse(smimeMessage.getSignedContent())
		
		EventVS eventVS
		EventVS.withTransaction{
			eventVS = EventVS.get(msgJSON.eventId)
			//eventVS.dateFinish = dateFinish
		}

        Date signatureTime = userVS.getTimeStampToken()?.getTimeStampInfo().getGenTime()
        ResponseVS responseVS = null

        if(!eventVS.isActive(signatureTime)) {
            String msg = message(code: "checkedDateRangeErrorMsg",
                    args: [signatureTime, eventVS.getDateBegin(), eventVS.getDateFinish()])
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, msg)
        } else responseVS = new ResponseVS(ResponseVS.SC_OK)

        responseVS.type = TypeVS.TEST
		params.responseVS = responseVS
        response.status = responseVS.statusCode
        render responseVS.message
		
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
