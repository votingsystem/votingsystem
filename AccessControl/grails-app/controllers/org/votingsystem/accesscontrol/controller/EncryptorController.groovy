package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.bouncycastle.util.encoders.Base64
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
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code:'requestWithoutFile'))]

		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		def messageJSON = JSON.parse(new String(params.requestBytes, "UTF-8"))
		if(!messageJSON.publicKey) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code:'publicKeyMissingErrorMsg'))]
		}
	    byte[] decodedPK = Base64.decode(messageJSON.publicKey);
	    PublicKey receiverPublic =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
	    //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
		messageJSON.message="Hello '${messageJSON.from}' from '${grailsApplication.config.grails.serverURL}'"
        return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: messageJSON.toString().getBytes())]
	}
	
	/**
	 * Servicio para comprobar la creación de documentos con multifirma
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/getMultiSignedMessage]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio.
	 *                     documento SMIME firmado.
	 * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
	 * @return  Recibo que consiste en el documento recibido con la signatureVS añadida del servidor.
	 */
	def getMultiSignedMessage() {
		if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
		response.contentType = ContentTypeVS.SIGNED
		SMIMEMessage smimeMessage = messageSMIMEReq.getSmimeMessage()
		
		String fromUser = "EncryptorController"
		String toUser = "MultiSignatureTestClient"
		String subject = "Multisigned response"
		SMIMEMessage smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(
			fromUser, toUser, smimeMessage, subject)

		
		//ByteArrayOutputStream messageBaos = new ByteArrayOutputStream();
		//smimeMessageResp.writeTo(messageBaos)
		//log.debug("========= ${new String(messageBaos.toByteArray())}")
		
		MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.TEST, content:smimeMessageResp.getBytes())
		return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIMEResp, type:TypeVS.TEST,
                contentType:ContentTypeVS.SIGNED)]
	}
	
	def validateTimeStamp() {
		if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
		}
		MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		SMIMEMessage smimeMessage = messageSMIMEReq.getSmimeMessage()
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

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }

}
