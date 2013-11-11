package filters

import org.votingsystem.groovy.util.StringUtils;
import org.votingsystem.model.ResponseVS;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;

import grails.converters.JSON

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory

import org.votingsystem.controlcenter.model.*
import org.votingsystem.signature.smime.*

import javax.mail.internet.MimeMessage
import javax.servlet.http.HttpServletRequest

import org.springframework.web.context.request.RequestContextHolder

import java.security.cert.PKIXParameters;
import java.util.Map;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 
* */
class ControlCenterFilters {

    def grailsApplication 
	def messageSource
	def encryptionService
	def firmaService

    def filters = {
        
        paramsCheck(controller:'*', action:'*') {
            before = {
                log.debug "###########################<${params.controller}> - before ###################################"
                //log.debug "Method: " + request.method + " -Params: " + params 
				log.debug "Method: " + request.method
				log.debug "Params: " + params
				log.debug "request.contentType: " + request.contentType
                log.debug "getRemoteHost: " + request.getRemoteHost()
                log.debug "Request: " + request.getRequestURI()  + " - RemoteAddr: " + request.getRemoteAddr()
                log.debug "User agent: " + request.getHeader("User-Agent")
                log.debug "-----------------------------------------------------------------------------------"
				if(!params.int("max")) params.max = 20
                if(!params.int("offset")) params.offset = 0
                if(!params.sort) params.sort = "dateCreated"
                if(!params.order) params.order = "desc"
                response.setHeader("Cache-Control", "no-store")
				params.respuesta = null
				params.messageSMIMEReq = null
				params.receiverCert = null
				params.responseBytes = null
				params.pdfDocument = null
            }
			
			after = {
				MessageSMIME messageSMIME = params.messageSMIMEReq
				ResponseVS respuesta = params.respuesta
				if(messageSMIME && respuesta) {
					boolean operationOK = (ResponseVS.SC_OK == respuesta.statusCode)
					messageSMIME.evento = respuesta.eventVS
					messageSMIME.valido = operationOK
					messageSMIME.motivo = respuesta.message
					messageSMIME.tipo = respuesta.tipo
					MessageSMIME.withTransaction {
						messageSMIME.save(flush:true)
					}
					params.messageSMIMEReq = null
					log.debug "after - saved MessageSMIME '${messageSMIME.id}' -> '${messageSMIME.tipo}'"
				}
				if(response?.contentType?.contains("multipart/encrypted")) {
					log.debug "after - ENCRYPTED PLAIN TEXT"
					if(params.responseBytes && params.receiverCert) {
						ResponseVS encryptResponse =  encryptionService.encryptMessage(
							params.responseBytes, params.receiverCert)
						if (ResponseVS.SC_OK != encryptResponse.statusCode) {
							response.status = respuesta?.statusCode
							render respuesta?.message
							return false
						} else {
							response.contentLength = encryptResponse.messageBytes.length
							response.outputStream << encryptResponse.messageBytes
							response.outputStream.flush()
							return false
						}
					} else {
						log.error "after - ERROR - ENCRYPTED PLAIN TEXT"
					}
				}
				if(respuesta && ResponseVS.SC_OK != respuesta.statusCode) {
					log.error "after - respuesta - status: ${respuesta.statusCode} - contentType: ${response.contentType}"
					log.error "after - respuesta - message: ${respuesta.message}"
					response.status = respuesta.statusCode
					render respuesta.message
					return false
				}
				log.debug "after - status: ${response.status} - contentType: ${response.contentType}"
			}
        }
		
		pkcs7DocumentsFilter(controller:'*', action:'*') {
			before = {
				if(flash.forwarded) {
					log.debug("---- pkcs7DocumentsFilter - before - REQUEST FORWARDED- BYPASS PKCS7 FILTER");
					flash.forwarded = null
					return;
				}
				//Content-Type: application/x-pkcs7-signature, application/x-pkcs7-mime -> signed and encrypted
				byte[] requestBytes = null
				SMIMEMessageWrapper smimeMessageReq = null
				if(!request?.contentType?.contains("application/x-pkcs7-signature") &&
					!request?.contentType?.contains("application/x-pkcs7-mime")) {
					log.debug("---- pkcs7DocumentsFilter - before  - BYPASS PKCS7 FILTER")
					return
				} else {
					requestBytes = getBytesFromInputStream(request.getInputStream()) 
					//log.debug "---- pkcs7DocumentsFilter - consulta: ${new String(requestBytes)}"
					ResponseVS respuesta
					if(!requestBytes) {
						log.debug "---- pkcs7DocumentsFilter - before - REQUEST WITHOUT FILE ------------"
						response.status = ResponseVS.SC_ERROR_PETICION
						render(messageSource.getMessage(
							'evento.peticionSinArchivo', null, request.getLocale()))
						return false
					}
					if(request?.contentType?.contains("application/x-pkcs7-mime")) {
						if(request?.contentType?.contains("application/x-pkcs7-signature")) {
							log.debug "---- pkcs7DocumentsFilter - before -> SIGNED AND ENCRYPTED"
							respuesta =  encryptionService.decryptSMIMEMessage(
								requestBytes, request.getLocale())
							if(ResponseVS.SC_OK != respuesta.statusCode) {
								response.status = respuesta.statusCode
								render respuesta.message
								return false
							}
							smimeMessageReq = respuesta.smimeMessage
						} else {
							log.debug "---- pkcs7DocumentsFilter - before - ENCRYPTED - TODO"
						}
					} else if(request?.contentType?.contains("application/x-pkcs7-signature")) {
						log.debug "---- pkcs7DocumentsFilter - before - SIGNED"
						try {
							smimeMessageReq = new SMIMEMessageWrapper(
								new ByteArrayInputStream(requestBytes));
						} catch(Exception ex) {
							log.error(ex.getMessage(), ex)
							response.status = ResponseVS.SC_ERROR_PETICION
							render messageSource.getMessage(
								'signedDocumentErrorMsg', null, request.getLocale())
							return false
						}
					}
					respuesta = processSMIMERequest(smimeMessageReq, params, request)
					if(ResponseVS.SC_OK == respuesta.statusCode) {
						params.messageSMIMEReq = respuesta.messageSMIME
						return
					} else {
						response.status = respuesta?.statusCode
						render respuesta?.message
						return false
					}
				}
			}
			
			after = {
				if((!response?.contentType?.contains("application/x-pkcs7-signature") &&
					!response?.contentType?.contains("application/x-pkcs7-mime")) ||
					params.bypassPKCS7Filter) {
					log.debug "---- pkcs7DocumentsFilter - after - BYPASS PKCS7 FILTER"
					return
				}
				ResponseVS respuesta = params.respuesta
				MessageSMIME messageSMIME = respuesta?.messageSMIME
				if(messageSMIME) {
					byte[] smimeResponseBytes = messageSMIME.contenido
					X509Certificate encryptionReceiverCert = params.receiverCert 
					if(response?.contentType?.contains("application/x-pkcs7-mime")) {
						if(response?.contentType?.contains("application/x-pkcs7-signature")) {
							log.debug "---- pkcs7DocumentsFilter - after - SIGNED AND ENCRYPTED RESPONSE"
							//log.debug "---- pkcs7DocumentsFilter - after - receiver: ${encryptionReceiverCert.getSubjectDN()}"
							ResponseVS encryptResponse =  encryptionService.encryptSMIMEMessage(
								smimeResponseBytes, encryptionReceiverCert, request.getLocale())
							if(ResponseVS.SC_OK == encryptResponse.statusCode) {
								response.contentLength = encryptResponse.messageBytes.length
								response.outputStream << encryptResponse.messageBytes
								response.outputStream.flush()
							} else {
								log.debug "---- pkcs7DocumentsFilter - error encrypting response ${encryptResponse.message}";
								messageSMIME.valido = false
								messageSMIME.motivo = encryptResponse.message
								messageSMIME.save()
								response.contentType = "text/plain"
								response.status = encryptResponse.statusCode
								render encryptResponse.message
							}
							return false
						} else {
							//Document encrypted, encrypt -> respuesta.message
							log.debug "---- pkcs7DocumentsFilter - after  - TODO!!! ENCRYPTED RESPONSE"
						}
					} else if(response?.contentType?.contains("application/x-pkcs7-signature")) {
						log.debug "---- pkcs7DocumentsFilter - after - SIGNED RESPONSE"
						if(smimeResponseBytes) {
							response.contentLength = smimeResponseBytes?.length
							response.outputStream << smimeResponseBytes
							response.outputStream.flush()	
						} else {
							response.contentType = "text/plain"
							response.status = ResponseVS.SC_ERROR
							log.debug "---- pkcs7DocumentsFilter - after - EMPTY SIGNED RESPONSE"
							render "EMPTY SIGNED RESPONSE"
						}
						return false
					}
				}
			}
			
		}

    }
	
	/*
	 * requestBytes = "${request.getInputStream()}".getBytes() gives problems
	 * working with pdf
	 */
	public byte[] getBytesFromInputStream(InputStream entrada) throws IOException {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		byte[] buf =new byte[5120];
		int len;
		while((len = entrada.read(buf)) > 0){
			salida.write(buf,0,len);
		}
		salida.close();
		entrada.close();
		return salida.toByteArray();
	}
	
	private ResponseVS processSMIMERequest(SMIMEMessageWrapper smimeMessageReq,
		Map params, HttpServletRequest request) {
		if (smimeMessageReq?.isValidSignature()) {
			log.debug "---- Filter - processSMIMERequest - signature OK - "
			ResponseVS certValidationResponse = null;
			if("voto".equals(params.controller)) {
				certValidationResponse = firmaService.validateSMIMEVote(
					smimeMessageReq, request.getLocale())
			} else if("anuladorVoto".equals(params.controller)) {
				certValidationResponse = firmaService.validateSMIMEVoteCancelation(
					params.url, smimeMessageReq, request.getLocale())
			} else certValidationResponse = firmaService.validateSMIME(
				smimeMessageReq, request.getLocale())

			MessageSMIME messageSMIME
			if(ResponseVS.SC_OK != certValidationResponse.statusCode) {
				messageSMIME = new MessageSMIME(valido:false,
					motivo:certValidationResponse.message,
					tipo:Tipo.ERROR, contenido:smimeMessageReq.getBytes())
				MessageSMIME.withTransaction {
					messageSMIME.save()
				}
				log.error "**** Filter - processSMIMERequest - failed document validation - request rejected"
				log.error "**** Filter - processSMIMERequest - failed - status: ${certValidationResponse.statusCode}" +
						  " - message: ${certValidationResponse.message}"
				return certValidationResponse
			} else {
				messageSMIME = new MessageSMIME(valido:true,
					signers:certValidationResponse.usuarios,
					smimeMessage:smimeMessageReq,
					evento:certValidationResponse.evento,
					usuario:certValidationResponse.usuarios?.iterator()?.next(),
					tipo:certValidationResponse.tipo,
					contenido:smimeMessageReq.getBytes(),
					base64ContentDigest:smimeMessageReq.getContentDigestStr())
				MessageSMIME.withTransaction {
					messageSMIME.save()
				}
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK, messageSMIME:messageSMIME)
		} else if(smimeMessageReq) {
			log.error "**** Filter - processSMIMERequest - signature ERROR - "
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
				message:messageSource.getMessage('signatureErrorMsg', null, request.getLocale()))
		}
	}
	
}