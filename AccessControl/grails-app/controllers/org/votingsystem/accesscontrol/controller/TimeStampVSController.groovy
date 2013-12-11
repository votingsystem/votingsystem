package org.votingsystem.accesscontrol.controller

import org.bouncycastle.tsp.TSPException
import org.bouncycastle.tsp.TSPValidationException
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.TimeStampVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.FileUtils

import java.util.concurrent.atomic.AtomicBoolean

class TimeStampVSController {
	
	def timeStampVSService

	/**
	 * Servicio de generación de sellos de tiempo.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/timeStamp]
	 * @param [timeStampRequest] Solicitud de sellado de tiempo en formato RFC 3161.
	 * @responseContentType [application/timestamp-query]
	 * @responseContentType [application/timestamp-response]
	 * 
	 * @return Si todo es correcto un sello de tiempo en formato RFC 3161.
	 */
	def index() {
        byte[] timeStampRequestBytes = FileUtils.getBytesFromInputStream(request.getInputStream())
        params.responseVS = timeStampVSService.processRequest(timeStampRequestBytes, request.getLocale())
	}
	
	/**
	 * Servicio que devuelve el certificado con el que se firman los sellos de tiempo
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/timeStamp/cert]
	 *
	 * @return El certificado en formato PEM con el que se firman los sellos de tiempo
	 */
	def cert() {
        params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, contentType:ContentTypeVS.PEM,
                messageBytes:timeStampVSService.getSigningCertPEMBytes())
	}

    /**
     * Servicio de pruebas para sellos de tiempo
     *
     * @httpMethod [GET]
     * @serviceURL [/timeStamp/test]
     *
     * @return Si se recibe un mensaje firmado con un sello de tiempo correcto devuelve un codigo de estado 200
     */
    def test() {
        MessageSMIME messageSMIMEReq = params.messageSMIMEReq
        if(!messageSMIMEReq) {
            params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:message(code:'requestWithoutFile'), type:TypeVS.ERROR)
        } else params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, message:"TIMESTAMP OK", type: TypeVS.TEST)
    }

	/*def test() {
		TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
		String pueba = "prueba"
		//reqgen.setReqPolicy(m_sPolicyOID);
		
		MessageDigest sha = MessageDigest.getInstance("SHA1");
		byte[] digest =  sha.digest(pueba.getBytes() );
		String digestStr = new String(Base64.encode(digest));
		log.debug("--- digestStr : ${digestStr}")
		
		TimeStampRequest timeStampRequest = reqgen.generate(TSPAlgorithms.SHA256, digest);
		TimeStampRequest timeStampRequestServer = new TimeStampRequest(timeStampRequest.getEncoded())
		//String timeStampRequestStr = new String(Base64.encode(timeStampRequestBytes));
		//log.debug("timeStampRequestStr : ${timeStampRequestStr}")
		render timeStampService.processRequest(timeStampRequest.getEncoded(), null).statusCode
		return
	}*/
	
	/**
	 * Servicio que devuelve un sello de tiempo previamente generado y guardado en la base de datos.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/timeStamp/$serialNumber]
	 * @param [serialNumber] Número de serie del sello de tiempo.
	 * @return El sello de tiempo en formato RFC 3161.
	 */
	def getBySerialNumber() {
		if(params.long('serialNumber')) {
			TimeStampVS timeStamp
			TimeStampVS.withTransaction { timeStamp = TimeStampVS.findBySerialNumber(params.long('serialNumber')) }
			if(timeStamp)  params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK,
                messageBytes: timeStamp.getTokenBytes(), contentType:ContentTypeVS.CMS_SIGNED)
			else params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND, "ERROR")
		} else params.responseVS = new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))
	}

}
