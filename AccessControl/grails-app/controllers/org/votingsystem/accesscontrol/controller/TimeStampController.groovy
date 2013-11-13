package org.votingsystem.accesscontrol.controller

import grails.converters.JSON

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.votingsystem.accesscontrol.model.SelloTiempo
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.votingsystem.model.ResponseVS;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.tsp.TSPException
import org.bouncycastle.tsp.TSPValidationException 

class TimeStampController {
	
	def timeStampService
	
	int maxNumAttempts = 3

	/**
	 * Servicio de generación de sellos de tiempo.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/timeStamp/$serialNumber]
	 * @param [timeStampRequest] Solicitud de sellado de tiempo en formato RFC 3161.
	 * @responseContentType [application/timestamp-query]
	 * @responseContentType [application/timestamp-response]
	 * 
	 * @return Si todo es correcto un sello de tiempo en formato RFC 3161.
	 */
	def index() {
		try {
			byte[] timeStampRequestBytes = getStringFromInputStream(request.getInputStream()) 
			
			//String timeStampRequestStr = new String(Base64.encode(timeStampRequestBytes));
			//log.debug(" - timeStampRequestStr: ${timeStampRequestStr}")
			
			int attempts = 0
			AtomicBoolean pending = new AtomicBoolean(Boolean.TRUE)
			ResponseVS respuesta = null
			while(pending.get()) {
				try {
					respuesta = timeStampService.processRequest(
						timeStampRequestBytes, request.getLocale())
					pending.set(false)
				} catch(TSPException ex) {
					log.error(ex.getMessage(), ex)
					if(attempts < maxNumAttempts) {
						attempts++;
					} else {
						log.debug(" ---- consumidos los tres intentos")
						pending.set(false)
						respuesta = new ResponseVS(statusCode:ResponseVS.SC_ERROR_TIMESTAMP,
							message:message(code: 'error.timeStampGeneration'))
					} 
				} catch(TSPValidationException ex) {
					log.error(ex.getMessage(), ex)
					if(attempts < maxNumAttempts) {
						attempts++;
					} else {
						log.debug(" ---- consumidos los tres intentos")
						pending.set(false)
						respuesta = new ResponseVS(statusCode:ResponseVS.SC_ERROR_TIMESTAMP,
							message:message(code: 'error.timeStampGeneration'))
					}
				}catch(Exception ex) {
					log.error(ex.getMessage(), ex)
					respuesta = new ResponseVS(statusCode:ResponseVS.SC_ERROR_TIMESTAMP,
						message:message(code: 'error.timeStampGeneration'))
					pending.set(false)
				}
			}
			
			if(ResponseVS.SC_OK == respuesta.statusCode) {
				response.status = ResponseVS.SC_OK
				response.contentType = "application/timestamp-response"
				response.outputStream << respuesta.messageBytes // Performing a binary stream copy
				//response.outputStream.flush()
			} else {
				response.status = respuesta.statusCode
				render respuesta.message
			} 
			return false
		} catch(Exception ex) {
			log.debug (ex.getMessage(), ex)
			response.status = ResponseVS.SC_ERROR_REQUEST
		}
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
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
		byte[] signingCertBytes = timeStampService.getSigningCert()
		if(signingCertBytes) {
			response.contentLength = signingCertBytes.length
			response.outputStream <<  signingCertBytes
			response.outputStream.flush()
		} else {
			response.status = ResponseVS.SC_ERROR 
			render message(code: 'serviceErrorMsg')
		}
	}
	
	/*def prueba() {
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
			SelloTiempo selloDeTiempo
			SelloTiempo.withTransaction {
				selloDeTiempo = SelloTiempo.findBySerialNumber(params.long('serialNumber'))
			}
			if(selloDeTiempo) {
				response.status = ResponseVS.SC_OK
				response.outputStream << selloDeTiempo.getTokenBytes() // Performing a binary stream copy
				response.outputStream.flush()
			} else {
				response.status = ResponseVS.SC_NOT_FOUND
				render "ERROR"
			}
			return false
		}
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
		return false
		return
	}
	
	private byte[] getStringFromInputStream(InputStream entrada) throws IOException {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		byte[] buf =new byte[1024];
		int len;
		while((len = entrada.read(buf)) > 0){
			salida.write(buf,0,len);
		}
		salida.close();
		entrada.close();
		return salida.toByteArray();
	}

}
