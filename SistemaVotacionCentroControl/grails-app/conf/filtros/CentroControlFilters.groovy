package filtros

import org.sistemavotacion.utils.StringUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import grails.converters.JSON

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory
import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.smime.*

import javax.mail.internet.MimeMessage
import javax.servlet.http.HttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import java.security.cert.PKIXParameters;
import java.util.Map;

import org.sistemavotacion.smime.SignedMailValidator.ValidationResult;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* 
* */
class CentroControlFilters {

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
				params.mensajeSMIMEReq = null
				params.receiverCert = null
				params.responseBytes = null
				params.pdfDocument = null
            }
			
			after = {
				MensajeSMIME mensajeSMIME = params.mensajeSMIMEReq
				Respuesta respuesta = params.respuesta
				if(mensajeSMIME && respuesta) {
					boolean operationOK = (Respuesta.SC_OK == respuesta.codigoEstado)
					mensajeSMIME.evento = respuesta.evento
					mensajeSMIME.valido = operationOK
					mensajeSMIME.motivo = respuesta.mensaje
					mensajeSMIME.tipo = respuesta.tipo
					MensajeSMIME.withTransaction {
						mensajeSMIME.save(flush:true)
					}
					params.mensajeSMIMEReq = null
					log.debug "paramsCheck - after - saved MensajeSMIME '${mensajeSMIME.id}' -> '${mensajeSMIME.tipo}'"
				}
				if(response?.contentType?.contains("multipart/encrypted")) {
					log.debug "---- paramsCheck - after - ENCRYPTED PLAIN TEXT"
					if(params.responseBytes && params.receiverCert) {
						Respuesta encryptResponse =  encryptionService.encryptMessage(
							params.responseBytes, params.receiverCert)
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
					Respuesta respuesta
					if(!requestBytes) {
						log.debug "---- pkcs7DocumentsFilter - before - REQUEST WITHOUT FILE ------------"
						response.status = Respuesta.SC_ERROR_PETICION
						render(messageSource.getMessage(
							'evento.peticionSinArchivo', null, request.getLocale()))
						return false
					}
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
							log.debug "---- pkcs7DocumentsFilter - before - ENCRYPTED - TODO"
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
						return
					} else {
						response.status = respuesta?.codigoEstado
						render respuesta?.mensaje
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
				Respuesta respuesta = params.respuesta
				MensajeSMIME mensajeSMIME = respuesta?.mensajeSMIME
				if(mensajeSMIME) {
					byte[] smimeResponseBytes = mensajeSMIME.contenido
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
								log.debug "---- pkcs7DocumentsFilter - error encrypting response ${encryptResponse.mensaje}";
								mensajeSMIME.valido = false
								mensajeSMIME.motivo = encryptResponse.mensaje
								mensajeSMIME.save()
								response.contentType = "text/plain"
								response.status = encryptResponse.codigoEstado
								render encryptResponse.mensaje
							}
							return false
						} else {
							//Document encrypted, encrypt -> respuesta.mensaje
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
							response.status = Respuesta.SC_ERROR
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
				log.error "**** Filter - processSMIMERequest - failed document validation - request rejected"
				log.error "**** Filter - processSMIMERequest - failed - status: ${certValidationResponse.codigoEstado}" +
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