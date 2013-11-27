package org.votingsystem.accesscontrol.controller

import org.bouncycastle.tsp.TSPException
import org.bouncycastle.tsp.TSPValidationException
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.TimeStampVS
import org.votingsystem.model.ResponseVS

import java.util.concurrent.atomic.AtomicBoolean

class TimeStampVSController {
	
	def timeStampVSService
	
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
			ResponseVS responseVS = null
			while(pending.get()) {
				try {
					responseVS = timeStampVSService.processRequest(timeStampRequestBytes, request.getLocale())
					pending.set(false)
				} catch(TSPException ex) {
					log.error(ex.getMessage(), ex)
					if(attempts < maxNumAttempts) {
						attempts++;
					} else {
						log.debug(" ---- consumidos los tres intentos")
						pending.set(false)
						responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_TIMESTAMP,
							message:message(code: 'error.timeStampGeneration'))
					} 
				} catch(TSPValidationException ex) {
					log.error(ex.getMessage(), ex)
					if(attempts < maxNumAttempts) {
						attempts++;
					} else {
						log.debug(" ---- consumidos los tres intentos")
						pending.set(false)
						responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_TIMESTAMP,
							message:message(code: 'error.timeStampGeneration'))
					}
				}catch(Exception ex) {
					log.error(ex.getMessage(), ex)
					responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_TIMESTAMP,
						message:message(code: 'error.timeStampGeneration'))
					pending.set(false)
				}
			}
			
			if(ResponseVS.SC_OK == responseVS.statusCode) {
				response.status = ResponseVS.SC_OK
				response.contentType = "application/timestamp-response"
				response.outputStream << responseVS.messageBytes // Performing a binary stream copy
				//response.outputStream.flush()
			} else {
				response.status = responseVS.statusCode
				render responseVS.message
			} 
			return false
		} catch(Exception ex) {
			log.debug (ex.getMessage(), ex)
			response.status = ResponseVS.SC_ERROR_REQUEST
		}
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'requestWithErrorsHTML', args:[
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
		byte[] signingCertBytes = timeStampVSService.getSigningCert()
		if(signingCertBytes) {
			response.contentLength = signingCertBytes.length
			response.outputStream <<  signingCertBytes
			response.outputStream.flush()
		} else {
			response.status = ResponseVS.SC_ERROR 
			render message(code: 'serviceErrorMsg')
		}
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
            String msg = message(code:'requestWithoutFile')
            log.error msg
            response.status = ResponseVS.SC_ERROR_REQUEST
            render msg
            return false
        } else {
            response.status = ResponseVS.SC_OK
            render "TIMESTAMP OK"
            return false
        }

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
			TimeStampVS selloDeTiempo
			TimeStampVS.withTransaction {
				selloDeTiempo = TimeStampVS.findBySerialNumber(params.long('serialNumber'))
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
		render message(code: 'requestWithErrorsHTML', args:[
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
