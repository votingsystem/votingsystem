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

    def index() { 
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		if(!params.requestBytes) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code:'requestWithoutFile'))]

		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		byte[] solicitud = params.requestBytes
		//log.debug("Solicitud" + new String(solicitud))
		def messageJSON = JSON.parse(new String(solicitud))
		if(!messageJSON.publicKey) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code:'publicKeyMissingErrorMsg'))]
		}
	    byte[] decodedPK = Base64.decode(messageJSON.publicKey);
	    PublicKey receiverPublic =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
	    //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
		messageJSON.message="Hello '${messageJSON.from}' from server"
        return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: messageJSON.toString().getBytes())]
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
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
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
		return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIMEResp, type:TypeVS.TEST,
                contentType:ContentTypeVS.SIGNED)]
	}
	
	def validateTimeStamp() {
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
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
        if(!eventVS.isActive(signatureTime)) {
            String msg = message(code: "checkedDateRangeErrorMsg",
                    args: [signatureTime, eventVS.getDateBegin(), eventVS.getDateFinish()])
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR, msg)]
        } else return [responseVS:new ResponseVS(ResponseVS.SC_OK)]
	}

}
