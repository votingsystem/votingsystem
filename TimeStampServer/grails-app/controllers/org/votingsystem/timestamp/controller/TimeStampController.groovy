package org.votingsystem.timestamp.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.TimeStampVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.FileUtils
import java.util.Calendar

class TimeStampController {
	
	def timeStampService
    def timeStampTestService

    /**
     * Servicio de generación de sellos de tiempo 'discretos'. Para dificultar la asociación de socilicitudes de acceso
     * a votos
     *
     * @httpMethod [POST]
     * @serviceURL [/timeStamp/discrete]
     * @param [timeStampRequest] Solicitud de sellado de tiempo en formato RFC 3161.
     * @responseContentType [application/timestamp-query]
     * @responseContentType [application/timestamp-response]
     *
     * @return Si todo es correcto un sello de tiempo en formato RFC 3161 con la hora de la petición (con minutos y
     * segundos puestos a 0)
     */
    def discrete() {
        byte[] timeStampRequestBytes = FileUtils.getBytesFromInputStream(request.getInputStream())
        Calendar calendar = Calendar.getInstance()
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        ResponseVS responseVS = timeStampService.processRequest(timeStampRequestBytes, calendar.getTime(),
                request.getLocale())
        response.status = responseVS.getStatusCode()
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            response.contentLength = responseVS.getMessageBytes().length
            response.setContentType(responseVS.getContentType().getName())
            response.outputStream <<  responseVS.getMessageBytes()
            response.outputStream.flush()
        } else render responseVS.getMessage()
        return false
    }

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
        ResponseVS responseVS = timeStampService.processRequest(timeStampRequestBytes, Calendar.getInstance().getTime())
        response.status = responseVS.getStatusCode()
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            response.contentLength = responseVS.getMessageBytes().length
            response.setContentType(responseVS.getContentType().getName())
            response.outputStream <<  responseVS.getMessageBytes()
            response.outputStream.flush()
        } else render responseVS.getMessage()
        return false
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
        byte[] signingCertPEMBytes = timeStampService.getSigningCertPEMBytes()
        response.status = ResponseVS.SC_OK
        response.contentLength = signingCertPEMBytes.length
        response.setContentType(ContentTypeVS.X509_CA.getName())
        response.outputStream <<  signingCertPEMBytes
        response.outputStream.flush()
        return false
	}
	
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
			if(timeStamp) {
                response.status = ResponseVS.SC_OK
                response.setContentType(ContentTypeVS.CMS_SIGNED.getName())
                response.contentLength = timeStamp.getTokenBytes().length
                response.outputStream <<  timeStamp.getTokenBytes()
                response.outputStream.flush()
            } else {
                response.status = ResponseVS.SC_NOT_FOUND
                render "ERROR"
            }
		} else {
            response.status = ResponseVS.SC_ERROR_REQUEST
            render message(code: 'missingParamErrorMsg', args:["serialNumber"])
        }
        return false
	}

    /**
     * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Servicio que añade Autoridades de Confianza.<br/>
     * Sirve para poder validar documentos firmados con sellos de tiempo emitidos por el servidor.
     *
     * @httpMethod [POST]
     * @param documento firmado en formato SMIME
     *
     * @return Si todo va bien devuelve un código de estado HTTP 200.
     */
    def validateTestMessage() {
        ResponseVS responseVS =  timeStampTestService.validateMessage("${request.getInputStream()}".getBytes())
        response.status = responseVS.statusCode
        render responseVS.message
        return false
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        ResponseVS responseVS = ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))
        response.status = responseVS.statusCode
        render responseVS.message
        return false
    }
}
