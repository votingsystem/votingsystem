package filters

import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import java.security.cert.X509Certificate
import javax.servlet.http.HttpServletRequest

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 
* */
class ControlCenterFilters {

    def grailsApplication 
	def messageSource
	def encryptionService
	def signatureVSService

    def filters = {
        
        paramsCheck(controller:'*', action:'*') {
            before = {
                log.debug "###########################<${params.controller}> - before ################################"
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
				params.responseVS = null
				params.messageSMIMEReq = null
				params.receiverCert = null
				params.responseBytes = null
				params.pdfDocument = null
            }
			
			after = {
				MessageSMIME messageSMIME = params.messageSMIMEReq
				ResponseVS responseVS = params.responseVS
				if(messageSMIME && responseVS) {
					messageSMIME.eventVS = responseVS.eventVS
					messageSMIME.metaInf = responseVS.message
					messageSMIME.type = responseVS.type
					MessageSMIME.withTransaction { messageSMIME.save(flush:true) }
					params.messageSMIMEReq = null
					log.debug "after - saved MessageSMIME '${messageSMIME.id}' -> '${messageSMIME.type}'"
				}
				if(response.contentType?.contains(ContentTypeVS.MULTIPART_ENCRYPTED)) {
					log.debug "after - ENCRYPTED PLAIN TEXT"
					if(params.responseBytes && params.receiverCert) {
						ResponseVS encryptResponse =  encryptionService.encryptMessage(
                                params.responseBytes, params.receiverCert)
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
					} else log.error " ### after - ERROR - ENCRYPTED PLAIN TEXT"
				}
				if(responseVS != null && ResponseVS.SC_OK != responseVS?.statusCode) {
					log.error "after - ERROR - message: ${responseVS.message}"
					response.status = responseVS.statusCode
					render responseVS.message
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
				if(!request?.contentType?.contains(ContentTypeVS.SIGNED) &&
					!request?.contentType?.contains(ContentTypeVS.ENCRYPTED)) {
					log.debug("---- pkcs7DocumentsFilter - before  - BYPASS PKCS7 FILTER")
					return
				} else {
					requestBytes = getBytesFromInputStream(request.getInputStream()) 
					//log.debug "---- pkcs7DocumentsFilter - consulta: ${new String(requestBytes)}"
					ResponseVS responseVS
					if(!requestBytes) {
						log.debug "---- pkcs7DocumentsFilter - before - REQUEST WITHOUT FILE ------------"
						response.status = ResponseVS.SC_ERROR_REQUEST
						render(messageSource.getMessage('requestWithoutFile', null, request.getLocale()))
						return false
					}
					if(request?.contentType?.contains(ContentTypeVS.ENCRYPTED)) {
						if(request?.contentType?.contains(ContentTypeVS.SIGNED)) {
							log.debug "---- pkcs7DocumentsFilter - before -> SIGNED AND ENCRYPTED"
							responseVS =  encryptionService.decryptSMIMEMessage(
								requestBytes, request.getLocale())
							if(ResponseVS.SC_OK != responseVS.statusCode) {
								response.status = responseVS.statusCode
								render responseVS.message
								return false
							}
							smimeMessageReq = responseVS.smimeMessage
						} else {
							log.debug "---- pkcs7DocumentsFilter - before - ENCRYPTED - TODO"
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
			
			after = {
				if((!response?.contentType?.contains(ContentTypeVS.SIGNED) &&
					!response?.contentType?.contains(ContentTypeVS.ENCRYPTED)) || params.bypassPKCS7Filter) {
					log.debug "---- pkcs7DocumentsFilter - after - BYPASS PKCS7 FILTER"
					return
				}
				ResponseVS responseVS = params.responseVS
				MessageSMIME messageSMIME = responseVS?.data
				if(messageSMIME) {
					byte[] smimeResponseBytes = messageSMIME.content
					X509Certificate encryptionReceiverCert = params.receiverCert 
					if(response?.contentType?.contains(ContentTypeVS.ENCRYPTED)) {
						if(response?.contentType?.contains(ContentTypeVS.SIGNED)) {
							log.debug "---- pkcs7DocumentsFilter - after - SIGNED AND ENCRYPTED RESPONSE"
							ResponseVS encryptResponse =  encryptionService.encryptSMIMEMessage(
								smimeResponseBytes, encryptionReceiverCert, request.getLocale())
							if(ResponseVS.SC_OK == encryptResponse.statusCode) {
								response.contentLength = encryptResponse.messageBytes.length
								response.outputStream << encryptResponse.messageBytes
								response.outputStream.flush()
							} else {
								log.debug "- pkcs7DocumentsFilter - ErrorEncryptingResponse ${encryptResponse.message}";
								messageSMIME.metaInf = encryptResponse.message
								messageSMIME.save()
								response.contentType = ContentTypeVS.TEXT
								response.status = encryptResponse.statusCode
								render encryptResponse.message
							}
							return false
						} else {
							//Document encrypted, encrypt -> responseVS.message
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
							log.debug "---- pkcs7DocumentsFilter - after - EMPTY SIGNED RESPONSE"
							render "EMPTY SIGNED RESPONSE"
						}
						return false
					}
				}
			}
		}
    }
	
	/**
	 * requestBytes = "${request.getInputStream()}".getBytes() gives problems with pdfs
	 */
	public byte[] getBytesFromInputStream(InputStream entrada) throws IOException {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		byte[] buf =new byte[5120];
		int len;
		while((len = entrada.read(buf)) > 0){ salida.write(buf,0,len); }
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
			} else if("voteVSCanceller".equals(params.controller)) {
				certValidationResponse = signatureVSService.validateSMIMEVoteCancelation(
					params.url, smimeMessageReq, request.getLocale())
			} else certValidationResponse = signatureVSService.validateSMIME(smimeMessageReq, request.getLocale())
			MessageSMIME messageSMIME
			if(ResponseVS.SC_OK != certValidationResponse.statusCode) {
				messageSMIME = new MessageSMIME(valido:false, metaInf:certValidationResponse.message,
					type:TypeVS.ERROR, content:smimeMessageReq.getBytes())
				MessageSMIME.withTransaction { messageSMIME.save() }
				log.error "**** Filter - processSMIMERequest - failed document validation - request rejected"
				log.error "**** Filter - processSMIMERequest - failed - status: ${certValidationResponse.statusCode}" +
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