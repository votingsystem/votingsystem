package filtros

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import org.sistemavotacion.utils.StringUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import grails.converters.JSON

import javax.mail.Header;
import javax.servlet.http.HttpServletRequest
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.smime.*
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest
import org.springframework.web.servlet.FlashMap;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* 
*/
class ControlAccesoFilters {

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
				params.mensajeSMIMEReq = null
				params.receiverCert = null
				params.responseBytes = null
				params.requestBytes = null
				params.forwarded = null
				params.pdfDocument = null
				params.respuesta = null
            }
			after = {
				MensajeSMIME mensajeSMIME = params.mensajeSMIMEReq
				
				
				Respuesta respuesta = params.respuesta
				if(mensajeSMIME && respuesta){
					MensajeSMIME.withTransaction {
						mensajeSMIME = mensajeSMIME.merge()
						boolean operationOK = (Respuesta.SC_OK == respuesta.codigoEstado)
						mensajeSMIME.evento = respuesta.evento
						mensajeSMIME.valido = operationOK
						mensajeSMIME.motivo = respuesta.mensaje
						mensajeSMIME.tipo = respuesta.tipo
						mensajeSMIME.save(flush:true)
					}
					log.debug "paramsCheck - after - saved MensajeSMIME '${mensajeSMIME.id}' -> '${mensajeSMIME.tipo}'"
				}
				if(response?.contentType?.contains("multipart/encrypted")) {
					log.debug "---- paramsCheck - after - ENCRYPTED PLAIN TEXT"
					if(params.responseBytes && (params.receiverCert || params.receiverPublicKey)) {
						Respuesta encryptResponse = null
						if(params.receiverPublicKey) {
							encryptResponse =  encryptionService.encryptMessage(
								params.responseBytes, params.receiverPublicKey)
						} else if(params.receiverCert) {
							encryptResponse =  encryptionService.encryptToCMS(
								params.responseBytes, params.receiverCert)
						}
						if (Respuesta.SC_OK != encryptResponse.codigoEstado) {
							response.status = respuesta?.codigoEstado
							render respuesta?.mensaje
							return false
						} else {
							response.contentLength = encryptResponse.messageBytes.length
							response.outputStream << encryptResponse.messageBytes
							response.outputStream.flush()
							return false
						}
					} else {
						log.error "---- paramsCheck - after - ERROR - ENCRYPTED PLAIN TEXT"
					}
				}
				if(respuesta && Respuesta.SC_OK != respuesta.codigoEstado) {
					log.error "**** paramsCheck - after - respuesta - status: ${respuesta.codigoEstado} - contentType: ${response.contentType}"
					log.error "**** paramsCheck - after - respuesta - mensaje: ${respuesta.mensaje}"
					response.status = respuesta.codigoEstado
					render respuesta.mensaje
					
					return false
				}
				log.debug "---- paramsCheck - after - status: ${response.status} - contentType: ${response.contentType}"
			}
        }
		
		filemapFilter(action:'FileMap', find:true) {
			before = {
				//accessRequest:application/x-pkcs7-signature, application/x-pkcs7-mime -> 
				//file name 'accessRequest' signed and encrypted
				log.debug "---- filemapFilter - before "
				if (!(request instanceof MultipartHttpServletRequest)) {
					log.debug "---- filemapFilter - before - ERROR - request NOT MultipartHttpServletRequest "
					response.status = Respuesta.SC_ERROR_PETICION
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
						Respuesta respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION)
						SMIMEMessageWrapper smimeMessageReq
						if(contentType.contains("application/x-pkcs7-mime")) {
							if(contentType.contains("application/x-pkcs7-signature")) {
								log.debug "---- filemapFilter - file: ${fileName} -> SIGNED AND ENCRYPTED"
								respuesta = encryptionService.decryptSMIMEMessage(
									fileMap.get(key)?.getBytes(), request.getLocale())
								if(Respuesta.SC_OK != respuesta.codigoEstado) {
									response.status = respuesta.codigoEstado
									render respuesta.mensaje
									return false
								}
								smimeMessageReq = respuesta.smimeMessage
							} else {
								log.debug "---- filemapFilter - before - file: ${fileName} -> ENCRYPTED "
								respuesta = encryptionService.decryptMessage(
									fileMap.get(key)?.getBytes(), request.getLocale())
								if(Respuesta.SC_OK != respuesta.codigoEstado) {
									response.status = respuesta.codigoEstado
									render respuesta.mensaje
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
								response.status = Respuesta.SC_ERROR_PETICION
								render messageSource.getMessage(
									'signedDocumentErrorMsg', null, request.getLocale())
								return false
							}
						}
						if(smimeMessageReq) {
							respuesta = processSMIMERequest(smimeMessageReq, params, request)
							if(Respuesta.SC_OK == respuesta.codigoEstado) {
								params[fileName] = respuesta.mensajeSMIME
							} else {
								params[fileName] = null
								response.status = respuesta?.codigoEstado
								render respuesta?.mensaje
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
				if(params.forwarded) {
					log.debug("---- pkcs7DocumentsFilter - before - REQUEST FORWARDED- BYPASS PKCS7 FILTER");
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
					requestBytes = "${request.getInputStream()}".getBytes() 
					if(!requestBytes) {
						log.debug "---- pkcs7DocumentsFilter - before  - REQUEST WITHOUT FILE ------------"
						response.status = Respuesta.SC_ERROR_PETICION
						render(messageSource.getMessage(
							'evento.peticionSinArchivo', null, request.getLocale()))
						return false
					}
					Respuesta respuesta
					if(request?.contentType?.contains("application/pdf")) {
						if(request?.contentType?.contains("application/x-pkcs7-mime")) {
							if(request?.contentType?.contains("application/x-pkcs7-signature")) {
								log.debug "---- pkcs7DocumentsFilter - before  -> PDF SIGNED AND ENCRYPTED"
								respuesta = encryptionService.decryptMessage(requestBytes, request.getLocale())
								if(Respuesta.SC_OK != respuesta.codigoEstado) {
									log.debug "---- pkcs7DocumentsFilter - before  - PDF ENCRYPTION ERROR"
									response.status = respuesta.codigoEstado
									render respuesta.mensaje
									return false
								}
								requestBytes = respuesta.messageBytes
							} else {
								log.debug "---- pkcs7DocumentsFilter - before  -> PDF ENCRYPTED - TODO"	
							}
						} else if(request?.contentType?.contains("application/x-pkcs7-signature")) {
							log.debug "---- pkcs7DocumentsFilter - before  - PDF SIGNED"
						} else {
							log.debug "---- pkcs7DocumentsFilter - before  - PLAIN PDF - TODO"
							requestBytes = null
						} 
						if(requestBytes) respuesta = pdfService.checkSignature(
								requestBytes, request.getLocale())
						if(Respuesta.SC_OK != respuesta.codigoEstado) {
							log.debug "---- pkcs7DocumentsFilter - before  - PDF SIGNATURE ERROR"
							response.status = respuesta.codigoEstado
							render respuesta.mensaje
							return false
						}
						params.pdfDocument = respuesta.documento
					} else {
						if(request?.contentType?.contains("application/x-pkcs7-mime")) {
							if(request?.contentType?.contains("application/x-pkcs7-signature")) {
								log.debug "---- pkcs7DocumentsFilter - before -> SIGNED AND ENCRYPTED"
								respuesta =  encryptionService.decryptSMIMEMessage(
									requestBytes, request.getLocale())
								if(Respuesta.SC_OK != respuesta.codigoEstado) {
									response.status = respuesta.codigoEstado
									render respuesta.mensaje
									return false
								}
								smimeMessageReq = respuesta.smimeMessage
							} else {
								log.debug "---- pkcs7DocumentsFilter - ENCRYPTED -TODO"
								respuesta =  encryptionService.decryptMessage(
									requestBytes, request.getLocale())
								if(Respuesta.SC_OK != respuesta.codigoEstado) {
									response.status = respuesta.codigoEstado
									render respuesta.mensaje
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
								response.status = Respuesta.SC_ERROR_PETICION
								render messageSource.getMessage(
									'signedDocumentErrorMsg', null, request.getLocale())
								return false
							}
						}
						respuesta = processSMIMERequest(smimeMessageReq, params, request)
						if(Respuesta.SC_OK == respuesta.codigoEstado) { 
							params.mensajeSMIMEReq = respuesta.mensajeSMIME
							params.mensajeSMIMEReq = respuesta.mensajeSMIME
							return
						} else {
							response.status = respuesta?.codigoEstado
							render respuesta?.mensaje
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
				Respuesta respuesta = params.respuesta
				MensajeSMIME mensajeSMIME = respuesta.mensajeSMIME
				if(mensajeSMIME) {
					byte[] smimeResponseBytes = mensajeSMIME?.contenido
					X509Certificate encryptionReceiverCert = params.receiverCert
					if(response?.contentType?.contains("application/x-pkcs7-mime")) {
						if(response?.contentType?.contains("application/x-pkcs7-signature")) {
							log.debug "---- pkcs7DocumentsFilter - after - SIGNED AND ENCRYPTED RESPONSE"
							//log.debug "---- pkcs7DocumentsFilter - after - receiver: ${encryptionReceiverCert.getSubjectDN()}"
							Respuesta encryptResponse =  encryptionService.encryptSMIMEMessage(
								smimeResponseBytes, encryptionReceiverCert, request.getLocale())
							if(Respuesta.SC_OK == encryptResponse.codigoEstado) {
								response.contentLength = encryptResponse.messageBytes.length
								response.outputStream << encryptResponse.messageBytes
								response.outputStream.flush()
							} else {
								log.error "---- pkcs7DocumentsFilter - error encrypting response ${encryptResponse.mensaje}";
								mensajeSMIME.valido = false
								mensajeSMIME.motivo = encryptResponse.mensaje
								mensajeSMIME.save()
								response.contentType = "text/plain"
								response.status = encryptResponse.codigoEstado
								render encryptResponse.mensaje
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
							response.status = Respuesta.SC_ERROR_EJECUCION
							log.error "---- pkcs7DocumentsFilter - after - EMPTY SIGNED RESPONSE"
							render "EMPTY SIGNED RESPONSE"
						}
						return false
					}
				}
			}
			
		}

    }
	
	private Respuesta processSMIMERequest(SMIMEMessageWrapper smimeMessageReq,
		Map params, HttpServletRequest request) {
		if (smimeMessageReq?.isValidSignature()) {
			log.debug "---- Filter - processSMIMERequest - signature OK - "
			Respuesta certValidationResponse = null;
			if("voto".equals(params.controller)) {
				certValidationResponse = firmaService.validateSMIMEVote(
					smimeMessageReq, request.getLocale())
			} else certValidationResponse = firmaService.validateSMIME(
				smimeMessageReq, request.getLocale())

			MensajeSMIME mensajeSMIME
			if(Respuesta.SC_OK != certValidationResponse.codigoEstado) {
				mensajeSMIME = new MensajeSMIME(valido:false,
					motivo:certValidationResponse.mensaje,
					tipo:Tipo.ERROR, contenido:smimeMessageReq.getBytes())
				MensajeSMIME.withTransaction {
					mensajeSMIME.save()
				}
				log.error "*** Filter - processSMIMERequest - failed document validation - request rejected"
				log.error "*** Filter - processSMIMERequest - failed - status: ${certValidationResponse.codigoEstado}" +
						  " - mensaje: ${certValidationResponse.mensaje}"
				return certValidationResponse
			} else {
				mensajeSMIME = new MensajeSMIME(valido:true,
					signers:certValidationResponse.usuarios,
					smimeMessage:smimeMessageReq,
					evento:certValidationResponse.evento,
					usuario:certValidationResponse.usuarios?.iterator()?.next(),
					tipo:certValidationResponse.tipo,
					contenido:smimeMessageReq.getBytes(),
					base64ContentDigest:smimeMessageReq.getContentDigestStr())
				MensajeSMIME.withTransaction {
					mensajeSMIME.save()
				}
			}
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensajeSMIME:mensajeSMIME)
		} else if(smimeMessageReq) {
			log.error "**** Filter - processSMIMERequest - signature ERROR - "
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:messageSource.getMessage('signatureErrorMsg', null, request.getLocale()))
		}
	}
	 
}