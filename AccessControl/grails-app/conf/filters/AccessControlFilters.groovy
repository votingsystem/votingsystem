package filters

import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import java.security.cert.X509Certificate;
import org.votingsystem.model.TypeVS
import org.springframework.web.multipart.MultipartHttpServletRequest
import javax.servlet.http.HttpServletRequest
import org.votingsystem.model.*
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.*

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 
*/
class AccessControlFilters {

    def signatureVSService
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
				params.responseVS = null
            }
			after = {
				MessageSMIME messageSMIME = params.messageSMIMEReq
				ResponseVS responseVS = params.responseVS
				if(messageSMIME && responseVS){
					MessageSMIME.withTransaction {
						messageSMIME = messageSMIME.merge()
						messageSMIME.eventVS = responseVS.eventVS
						messageSMIME.metaInf = responseVS.message
						messageSMIME.type = responseVS.type
						messageSMIME.save(flush:true)
					}
					log.debug "after - saved MessageSMIME '${messageSMIME.id}' -> '${messageSMIME.type}'"
				}
				if(response?.contentType?.contains(ContentTypeVS.MULTIPART_ENCRYPTED)) {
					log.debug "after - ENCRYPTED PLAIN TEXT"
					if(params.responseBytes && (params.receiverCert || params.receiverPublicKey)) {
						ResponseVS encryptResponse = null
						if(params.receiverPublicKey) {
							encryptResponse =  signatureVSService.encryptMessage(
                                    params.responseBytes, params.receiverPublicKey)
						} else if(params.receiverCert) {
							encryptResponse =  signatureVSService.encryptToCMS(
								params.responseBytes, params.receiverCert)
						}
						if (ResponseVS.SC_OK != encryptResponse.statusCode) {
							response.status = responseVS?.statusCode
							render responseVS?.message
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
				if(responseVS && ResponseVS.SC_OK != responseVS.statusCode) {
					log.error "after - responseVS - status: ${responseVS.statusCode} - contentType: ${response.contentType}"
					log.error "after - responseVS - message: ${responseVS.message}"
					response.status = responseVS.statusCode
					render responseVS.message
					
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
					render(messageSource.getMessage('requestWithoutFile', null, request.getLocale()))
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
						ResponseVS responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR)
						SMIMEMessageWrapper smimeMessageReq
						if(contentType.contains(ContentTypeVS.ENCRYPTED)) {
							if(contentType.contains(ContentTypeVS.SIGNED)) {
								log.debug "---- filemapFilter - file: ${fileName} -> SIGNED AND ENCRYPTED"
								responseVS = signatureVSService.decryptSMIMEMessage(
									fileMap.get(key)?.getBytes(), request.getLocale())
								if(ResponseVS.SC_OK != responseVS.statusCode) {
									response.status = responseVS.statusCode
									render responseVS.message
									return false
								}
								smimeMessageReq = responseVS.smimeMessage
							} else {
								log.debug "---- filemapFilter - before - file: ${fileName} -> ENCRYPTED "
								responseVS = signatureVSService.decryptMessage(
									fileMap.get(key)?.getBytes(), request.getLocale())
								if(ResponseVS.SC_OK != responseVS.statusCode) {
									response.status = responseVS.statusCode
									render responseVS.message
									return false
								}
								params[fileName] = responseVS.messageBytes
							} 
						} else if(contentType.contains(ContentTypeVS.SIGNED)) {
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
							responseVS = processSMIMERequest(smimeMessageReq, params, request)
							if(ResponseVS.SC_OK == responseVS.statusCode) {
								params[fileName] = responseVS.data
							} else {
								params[fileName] = null
								response.status = responseVS?.statusCode
								render responseVS?.message
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
				if(!request?.contentType?.contains(ContentTypeVS.SIGNED) &&
					!request?.contentType?.contains(ContentTypeVS.ENCRYPTED)) {
					log.debug("---- pkcs7DocumentsFilter - before  - BYPASS PKCS7 FILTER")
					return
				} else {
					//log.debug "---- pkcs7DocumentsFilter - before  - consulta: ${new String(requestBytes)}"
					requestBytes = getBytesFromInputStream(request.getInputStream())
					if(!requestBytes) {
						log.debug "---- pkcs7DocumentsFilter - before  - REQUEST WITHOUT FILE ------------"
						response.status = ResponseVS.SC_ERROR_REQUEST
						render(messageSource.getMessage(
							'requestWithoutFile', null, request.getLocale()))
						return false
					}
					ResponseVS responseVS
					if(request?.contentType?.contains(ContentTypeVS.PDF)) {
						if(request?.contentType?.contains(ContentTypeVS.ENCRYPTED)) {
							responseVS = signatureVSService.decryptMessage(requestBytes, request.getLocale())
							if(ResponseVS.SC_OK != responseVS.statusCode) {
								log.debug "---- pkcs7DocumentsFilter - before  - PDF ENCRYPTION ERROR"
								response.status = responseVS.statusCode
								render responseVS.message
								return false
							}
							requestBytes = responseVS.messageBytes
						} else if(request?.contentType?.contains(ContentTypeVS.SIGNED)) {
							log.debug "---- pkcs7DocumentsFilter - before  - PDF SIGNED"
						} else {
							log.debug "---- pkcs7DocumentsFilter - before  - PLAIN PDF -"
							params.plainPDFDocument = requestBytes
						} 
						if(requestBytes) responseVS = pdfService.checkSignature(
								requestBytes, request.getLocale())
						if(ResponseVS.SC_OK != responseVS.statusCode) {
							log.debug "---- pkcs7DocumentsFilter - before  - PDF SIGNATURE ERROR"
							response.status = responseVS.statusCode
							render responseVS.message
							return false
						}
						params.pdfDocument = responseVS.data
					} else {
						if(request?.contentType?.contains(ContentTypeVS.ENCRYPTED)) {
							if(request?.contentType?.contains(ContentTypeVS.SIGNED)) {
								log.debug "---- pkcs7DocumentsFilter - before -> SIGNED AND ENCRYPTED"
								responseVS =  signatureVSService.decryptSMIMEMessage(
									requestBytes, request.getLocale())
								if(ResponseVS.SC_OK != responseVS.statusCode) {
									response.status = responseVS.statusCode
									render responseVS.message
									return false
								}
								smimeMessageReq = responseVS.smimeMessage
							} else {
								responseVS =  signatureVSService.decryptMessage(requestBytes, request.getLocale())
								if(ResponseVS.SC_OK != responseVS.statusCode) {
									response.status = responseVS.statusCode
									render responseVS.message
									return false
								}
								params.requestBytes = responseVS.messageBytes
								return
							}
						} else if(request?.contentType?.contains(ContentTypeVS.SIGNED)) {
							log.debug "---- pkcs7DocumentsFilter - before - SIGNED"							
							try {
								smimeMessageReq = new SMIMEMessageWrapper(new ByteArrayInputStream(requestBytes));
							} catch(Exception ex) {
								log.error(ex.getMessage(), ex)
								response.status = ResponseVS.SC_ERROR_REQUEST
								render messageSource.getMessage('signedDocumentErrorMsg', null, request.getLocale())
								return false
							}
						}
						responseVS = processSMIMERequest(smimeMessageReq, params, request)
						if(ResponseVS.SC_OK == responseVS.statusCode) {
							params.messageSMIMEReq = responseVS.data
							return
						} else {
							response.status = responseVS?.statusCode
							render responseVS?.message
							return false
						}						
					}
				}
			}
			
			after = {
				if((!response?.contentType?.contains(ContentTypeVS.SIGNED) &&
					!response?.contentType?.contains(ContentTypeVS.ENCRYPTED)) ||
					flash.bypassPKCS7Filter) {
					log.debug "---- pkcs7DocumentsFilter - after - BYPASS PKCS7 FILTER"
					return
				}
				ResponseVS responseVS = params.responseVS
				MessageSMIME messageSMIME = responseVS.data
				if(messageSMIME) {
					byte[] smimeResponseBytes = messageSMIME?.content
					X509Certificate encryptionReceiverCert = params.receiverCert
					if(response?.contentType?.contains(ContentTypeVS.ENCRYPTED)) {
						if(response?.contentType?.contains(ContentTypeVS.SIGNED)) {
							log.debug "---- pkcs7DocumentsFilter - after - SIGNED AND ENCRYPTED RESPONSE"
							//log.debug "---- pkcs7DocumentsFilter - after - receiver: ${encryptionReceiverCert.getSubjectDN()}"
							// ori -> ResponseVS encryptResponse =  signatureVSService.encryptSMIMEMessage(messageSMIME.smimeMessage.getBytes(), encryptionReceiverCert, request.getLocale())
							ResponseVS encryptResponse =  signatureVSService.encryptSMIMEMessage(
								messageSMIME.smimeMessage, encryptionReceiverCert, request.getLocale())
							if(ResponseVS.SC_OK == encryptResponse.statusCode) {
								response.contentLength = encryptResponse.messageBytes.length
								response.outputStream << encryptResponse.messageBytes
								response.outputStream.flush()
							} else {
								log.error "---- pkcs7DocumentsFilter - error encrypting response ${encryptResponse.message}";
								messageSMIME.metaInf = encryptResponse.message
								messageSMIME.save()
								response.contentType = ContentTypeVS.TEXT
								response.status = encryptResponse.statusCode
								render encryptResponse.message
							}
							return false
						} else {
							//Document encrypted, encrypt -> flash.contentToEncrypt
							log.debug "---- pkcs7DocumentsFilter - after  - TODO!!! ENCRYPTED RESPONSE"
						}
					} else if(response?.contentType?.contains(ContentTypeVS.SIGNED)) {
						log.debug "---- pkcs7DocumentsFilter - after - SIGNED RESPONSE"
						if(smimeResponseBytes) {
							response.contentLength = smimeResponseBytes?.length
							response.outputStream << smimeResponseBytes
							response.outputStream.flush()
						} else {
							response.contentType = ContentTypeVS.TEXT
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
	
	private ResponseVS processSMIMERequest(SMIMEMessageWrapper smimeMessageReq,Map params, HttpServletRequest request) {
		if (smimeMessageReq?.isValidSignature()) {
			log.debug "---- Filter - processSMIMERequest - signature OK - "
			ResponseVS certValidationResponse = null;
			if("voteVS".equals(params.controller)) {
				certValidationResponse = signatureVSService.validateSMIMEVote(smimeMessageReq, request.getLocale())
			} else certValidationResponse = signatureVSService.validateSMIME(smimeMessageReq, request.getLocale())

			MessageSMIME messageSMIME
			if(ResponseVS.SC_OK != certValidationResponse.statusCode) {
				messageSMIME = new MessageSMIME(metaInf:certValidationResponse.message, type:TypeVS.ERROR,
                        content:smimeMessageReq.getBytes())
				MessageSMIME.withTransaction { messageSMIME.save() }
				log.error "*** Filter - processSMIMERequest - failed document validation - request rejected"
				log.error "*** Filter - processSMIMERequest - failed - status: ${certValidationResponse.statusCode}" +
						  " - message: ${certValidationResponse.message}"
				return certValidationResponse
			} else {
				messageSMIME = new MessageSMIME(signers:certValidationResponse.data?.checkedSigners,
                    userVS:certValidationResponse.data?.checkedSigner, smimeMessage:smimeMessageReq,
					eventVS:certValidationResponse.eventVS, type:TypeVS.OK,
                    content:smimeMessageReq.getBytes(), base64ContentDigest:smimeMessageReq.getContentDigestStr())
				MessageSMIME.withTransaction {messageSMIME.save()}
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK, data:messageSMIME)
		} else if(smimeMessageReq) {
			log.error "**** Filter - processSMIMERequest - signature ERROR - "
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:messageSource.getMessage('signatureErrorMsg', null, request.getLocale()))
		}
	}
	 
}