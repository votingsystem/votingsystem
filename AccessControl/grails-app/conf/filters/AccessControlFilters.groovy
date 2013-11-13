package filters

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;

import org.votingsystem.groovy.util.StringUtils;
import org.votingsystem.model.TypeVS
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;

import grails.converters.JSON

import javax.mail.Header;
import javax.servlet.http.HttpServletRequest

import org.votingsystem.accesscontrol.model.*
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.*
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest
import org.springframework.web.servlet.FlashMap;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 
*/
class AccessControlFilters {

    def firmaService
	def encryptionService
    def grailsApplication
	def messageSource
	def pdfService

    def filters = {
        paramsCheck(controller:'*', action:'*') {
            before = {
                log.debug "###########################<${params.controller}> - before ###################################"
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
				params.messageSMIMEReq = null
				params.receiverCert = null
				params.responseBytes = null
				params.requestBytes = null
				params.respuesta = null
            }
			after = {
				MessageSMIME messageSMIME = params.messageSMIMEReq
				
				
				ResponseVS respuesta = params.respuesta
				if(messageSMIME && respuesta){
					MessageSMIME.withTransaction {
						messageSMIME = messageSMIME.merge()
						boolean operationOK = (ResponseVS.SC_OK == respuesta.statusCode)
						messageSMIME.evento = respuesta.eventVS
						messageSMIME.valido = operationOK
						messageSMIME.motivo = respuesta.message
						messageSMIME.type = respuesta.type
						messageSMIME.save(flush:true)
					}
					log.debug "after - saved MessageSMIME '${messageSMIME.id}' -> '${messageSMIME.type}'"
				}
				if(response?.contentType?.contains("multipart/encrypted")) {
					log.debug "after - ENCRYPTED PLAIN TEXT"
					if(params.responseBytes && (params.receiverCert || params.receiverPublicKey)) {
						ResponseVS encryptResponse = null
						if(params.receiverPublicKey) {
							encryptResponse =  encryptionService.encryptMessage(
								params.responseBytes, params.receiverPublicKey)
						} else if(params.receiverCert) {
							encryptResponse =  encryptionService.encryptToCMS(
								params.responseBytes, params.receiverCert)
						}
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
		
		filemapFilter(action:'FileMap', find:true) {
			before = {
				//accessRequest:application/x-pkcs7-signature, application/x-pkcs7-mime -> 
				//file name 'accessRequest' signed and encrypted
				log.debug "---- filemapFilter - before "
				if (!(request instanceof MultipartHttpServletRequest)) {
					log.debug "---- filemapFilter - before - ERROR - request NOT MultipartHttpServletRequest "
					response.status = ResponseVS.SC_ERROR_REQUEST
					render(messageSource.getMessage(
						'evento.peticionSinArchivo', null, request.getLocale()))
					return false
				}
				Map fileMap = ((MultipartHttpServletRequest)request)?.getFileMap();
				Set keyset = fileMap.keySet()
				for(String key:keyset) {
					String fileName
					String contentType
					if(key.contains(":")) {
						String[] keySplitted = key.split(":")
						fileName = keySplitted[0]
						contentType = keySplitted[1]
						ResponseVS respuesta = new ResponseVS(statusCode:ResponseVS.SC_ERROR)
						SMIMEMessageWrapper smimeMessageReq
						if(contentType.contains("application/x-pkcs7-mime")) {
							if(contentType.contains("application/x-pkcs7-signature")) {
								log.debug "---- filemapFilter - file: ${fileName} -> SIGNED AND ENCRYPTED"
								respuesta = encryptionService.decryptSMIMEMessage(
									fileMap.get(key)?.getBytes(), request.getLocale())
								if(ResponseVS.SC_OK != respuesta.statusCode) {
									response.status = respuesta.statusCode
									render respuesta.message
									return false
								}
								smimeMessageReq = respuesta.smimeMessage
							} else {
								log.debug "---- filemapFilter - before - file: ${fileName} -> ENCRYPTED "
								respuesta = encryptionService.decryptMessage(
									fileMap.get(key)?.getBytes(), request.getLocale())
								if(ResponseVS.SC_OK != respuesta.statusCode) {
									response.status = respuesta.statusCode
									render respuesta.message
									return false
								}
								params[fileName] = respuesta.messageBytes
							} 
						} else if(contentType.contains("application/x-pkcs7-signature")) {
							log.debug "---- filemapFilter - before - file: ${fileName} -> SIGNED"
							try {
								smimeMessageReq = new SMIMEMessageWrapper(
									new ByteArrayInputStream(fileMap.get(key)?.getBytes()));
							} catch(Exception ex) {
								log.error(ex.getMessage(), ex)
								response.status = ResponseVS.SC_ERROR_REQUEST
								render messageSource.getMessage(
									'signedDocumentErrorMsg', null, request.getLocale())
								return false
							}
						}
						if(smimeMessageReq) {
							respuesta = processSMIMERequest(smimeMessageReq, params, request)
							if(ResponseVS.SC_OK == respuesta.statusCode) {
								params[fileName] = respuesta.data
							} else {
								params[fileName] = null
								response.status = respuesta?.statusCode
								render respuesta?.message
								return false
							}
						}
					} else {
						params[key] = fileMap.get(key)?.getBytes()
						log.debug "---- filemapFilter - before - file: ${key} -> PLAIN"
					}
					
				}
			}

		}
		
		pkcs7DocumentsFilter(controller:'*', action:'*') {
			before = {
				log.debug("---- pkcs7DocumentsFilter - before -")
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
					//log.debug "---- pkcs7DocumentsFilter - before  - consulta: ${new String(requestBytes)}"
					requestBytes = getBytesFromInputStream(request.getInputStream())
					if(!requestBytes) {
						log.debug "---- pkcs7DocumentsFilter - before  - REQUEST WITHOUT FILE ------------"
						response.status = ResponseVS.SC_ERROR_REQUEST
						render(messageSource.getMessage(
							'evento.peticionSinArchivo', null, request.getLocale()))
						return false
					}
					ResponseVS respuesta
					if(request?.contentType?.contains("application/pdf")) {
						if(request?.contentType?.contains("application/x-pkcs7-mime")) {
							respuesta = encryptionService.decryptMessage(requestBytes, request.getLocale())
							if(ResponseVS.SC_OK != respuesta.statusCode) {
								log.debug "---- pkcs7DocumentsFilter - before  - PDF ENCRYPTION ERROR"
								response.status = respuesta.statusCode
								render respuesta.message
								return false
							}
							requestBytes = respuesta.messageBytes
						} else if(request?.contentType?.contains("application/x-pkcs7-signature")) {
							log.debug "---- pkcs7DocumentsFilter - before  - PDF SIGNED"
						} else {
							log.debug "---- pkcs7DocumentsFilter - before  - PLAIN PDF -"
							params.plainPDFDocument = requestBytes
						} 
						if(requestBytes) respuesta = pdfService.checkSignature(
								requestBytes, request.getLocale())
						if(ResponseVS.SC_OK != respuesta.statusCode) {
							log.debug "---- pkcs7DocumentsFilter - before  - PDF SIGNATURE ERROR"
							response.status = respuesta.statusCode
							render respuesta.message
							return false
						}
						params.pdfDocument = respuesta.data
					} else {
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
								log.debug "---- pkcs7DocumentsFilter - ENCRYPTED -TODO"
								respuesta =  encryptionService.decryptMessage(
									requestBytes, request.getLocale())
								if(ResponseVS.SC_OK != respuesta.statusCode) {
									response.status = respuesta.statusCode
									render respuesta.message
									return false
								}
								params.requestBytes = respuesta.messageBytes
								return
							}
						} else if(request?.contentType?.contains("application/x-pkcs7-signature")) {
							log.debug "---- pkcs7DocumentsFilter - before - SIGNED"							
							try {
								smimeMessageReq = new SMIMEMessageWrapper(
									new ByteArrayInputStream(requestBytes));
							} catch(Exception ex) {
								log.error(ex.getMessage(), ex)
								response.status = ResponseVS.SC_ERROR_REQUEST
								render messageSource.getMessage(
									'signedDocumentErrorMsg', null, request.getLocale())
								return false
							}
						}
						respuesta = processSMIMERequest(smimeMessageReq, params, request)
						if(ResponseVS.SC_OK == respuesta.statusCode) { 
							params.messageSMIMEReq = respuesta.data
							return
						} else {
							response.status = respuesta?.statusCode
							render respuesta?.message
							return false
						}						
					}
				}
			}
			
			after = {
				if((!response?.contentType?.contains("application/x-pkcs7-signature") &&
					!response?.contentType?.contains("application/x-pkcs7-mime")) ||
					flash.bypassPKCS7Filter) {
					log.debug "---- pkcs7DocumentsFilter - after - BYPASS PKCS7 FILTER"
					return
				}
				ResponseVS respuesta = params.respuesta
				MessageSMIME messageSMIME = respuesta.data
				if(messageSMIME) {
					byte[] smimeResponseBytes = messageSMIME?.contenido
					X509Certificate encryptionReceiverCert = params.receiverCert
					if(response?.contentType?.contains("application/x-pkcs7-mime")) {
						if(response?.contentType?.contains("application/x-pkcs7-signature")) {
							log.debug "---- pkcs7DocumentsFilter - after - SIGNED AND ENCRYPTED RESPONSE"
							//log.debug "---- pkcs7DocumentsFilter - after - receiver: ${encryptionReceiverCert.getSubjectDN()}"
							// ori -> ResponseVS encryptResponse =  encryptionService.encryptSMIMEMessage(messageSMIME.smimeMessage.getBytes(), encryptionReceiverCert, request.getLocale())
							ResponseVS encryptResponse =  encryptionService.encryptSMIMEMessage(
								messageSMIME.smimeMessage, encryptionReceiverCert, request.getLocale())
							if(ResponseVS.SC_OK == encryptResponse.statusCode) {
								response.contentLength = encryptResponse.messageBytes.length
								response.outputStream << encryptResponse.messageBytes
								response.outputStream.flush()
							} else {
								log.error "---- pkcs7DocumentsFilter - error encrypting response ${encryptResponse.message}";
								messageSMIME.valido = false
								messageSMIME.motivo = encryptResponse.message
								messageSMIME.save()
								response.contentType = "text/plain"
								response.status = encryptResponse.statusCode
								render encryptResponse.message
							}
							return false
						} else {
							//Document encrypted, encrypt -> flash.contentToEncrypt
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
							log.error "---- pkcs7DocumentsFilter - after - EMPTY SIGNED RESPONSE"
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
			} else certValidationResponse = firmaService.validateSMIME(
				smimeMessageReq, request.getLocale())

			MessageSMIME messageSMIME
			if(ResponseVS.SC_OK != certValidationResponse.statusCode) {
				messageSMIME = new MessageSMIME(valido:false,
					motivo:certValidationResponse.message,
					type:TypeVS.ERROR, contenido:smimeMessageReq.getBytes())
				MessageSMIME.withTransaction {
					messageSMIME.save()
				}
				log.error "*** Filter - processSMIMERequest - failed document validation - request rejected"
				log.error "*** Filter - processSMIMERequest - failed - status: ${certValidationResponse.statusCode}" +
						  " - message: ${certValidationResponse.message}"
				return certValidationResponse
			} else {
				Set<Usuario> usersVS = (Set<Usuario>)certValidationResponse.data;
				messageSMIME = new MessageSMIME(valido:true,
					signers:usersVS, smimeMessage:smimeMessageReq,
					evento:certValidationResponse.eventVS,
					usuario:usersVS?.iterator()?.next(),
					type:TypeVS.OK,
					contenido:smimeMessageReq.getBytes(),
					base64ContentDigest:smimeMessageReq.getContentDigestStr())
				MessageSMIME.withTransaction {
					messageSMIME.save()
				}
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIME)
		} else if(smimeMessageReq) {
			log.error "**** Filter - processSMIMERequest - signature ERROR - "
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:messageSource.getMessage('signatureErrorMsg', null, request.getLocale()))
		}
	}
	 
}