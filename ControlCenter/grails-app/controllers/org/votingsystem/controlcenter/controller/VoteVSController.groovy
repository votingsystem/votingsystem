package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.VoteVS
import org.votingsystem.model.VoteVSCanceller
import org.votingsystem.util.ApplicationContextHolder

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.cert.X509Certificate
/**
 * @infoController Servicio de Votos
 * @descController Servicio que procesa los votesVS recibidos.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class VoteVSController {

    def voteVSService
	
	
	/**
	 * Servicio que recoge los votesVS enviados por los usersVS.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/voteVS]
	 * @contentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio. El archivo de voto firmado por el
	 *        <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Certificado-de-voto">certificado de VoteVS.</a>
	 * @return  <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-VoteVS">El recibo del voto.</a>
	 */
	def index() {
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
        if(!messageSMIMEReq) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))
            return
        }
		ResponseVS responseVS = voteVSService.validateVote(messageSMIMEReq, request.getLocale())
		if (ResponseVS.SC_OK == responseVS.statusCode) {
            params.receiverCert = responseVS.data?.receiverCert
			responseVS.data = responseVS.data?.messageSMIME
			if(messageSMIMEReq.getUserVS())
				response.addHeader("representativeNIF", messageSMIMEReq.getUserVS().nif)
            String voteURL = "${createLink(controller:'messageSMIME', absolute:'true')}/${responseVS?.data?.id}"
            response.setHeader('voteURL', voteURL)
		}
        params.responseVS = responseVS
	}
	
	/**
	 * Servicio que devuelve la información de un voto a partir del identificador
	 * del mismo en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/voteVS/${id}]
	 * @param [id] Obligatorio. Identificador del voto en la base de datos
	 * @responseContentType [application/json]
	 * @return Documento JSON con la información del voto solicitado.
	 */
	def get() {
		VoteVS voteVS
		Map  voteVSMap
		VoteVS.withTransaction {
			voteVS = VoteVS.get(params.long('id'))
			if(voteVS) voteVSMap = voteVSService.getVotoMap(voteVS)
		}
        if(!voteVS) params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'voteNotFound', args:[params.id]))
		else render voteVSMap as JSON
	}
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). 
	 * Servicio que devuelve los votesVS con errores de una votación
	 * @httpMethod [GET]
	 * @serviceURL [/errors/event/${id}]
	 * @param [id] Obligatorio. Identificador del evento en la base de datos
	 * del Control de Acceso
	 * @responseContentType [application/zip]
	 * @return Documento ZIP con los errores de una votación
	 */
	def errors() {
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))
            return
		}
		log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
		EventVS event = EventVS.getAt(params.long('id'))
		if(!event) {
            params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'eventVSNotFound', args:[params.id]))
		} else {
            def errorMessages
            MessageSMIME.withTransaction {
                errorMessages = MessageSMIME.findAllByEventVSAndTypeAndType(event, TypeVS.ERROR, TypeVS.VOTE_ERROR)
            }
            render errorMessages.size()
            return false
        }
	}
	
	/**
	 * Servicio que devuelve la información de un voto a partir del
	 * hash asociado al mismo
	 * @httpMethod [GET]
	 * @serviceURL [/voteVS/hashHex/$hashHex]
	 * @param [hashHex] Obligatorio. Hash en hexadecimal asociado al voto.
	 * @responseContentType [application/json]
	 * @return Documento JSON con la información del voto solicitado.
	 */
	def hashCertVoteHex() {
		if (params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertVoteBase64 = new String(
				hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertVoteBase64: ${hashCertVoteBase64}"
			CertificateVS certificate
			CertificateVS.withTransaction {
				certificate = CertificateVS.findWhere(hashCertVoteBase64:hashCertVoteBase64)
			}
			if(!certificate) {
                params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'certByHEXNotFound', args:[params.hashHex]))
				return
			}
			VoteVS voteVS
			def voteVSMap
			VoteVS.withTransaction {
				voteVS = VoteVS.findWhere(certificate:certificate)
				voteVSMap = voteVSService.getVotoMap(voteVS)
			}
			if(!voteVS) {
                params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'voteVS.voteVSConCertNotFound', args:[params.hashHex]))
				return
			}
			 
			if(VoteVS.State.CANCELLED.equals(voteVS.state)) {
				VoteVSCanceller voteVSCanceller
				VoteVSCanceller.withTransaction {
					voteVSCanceller = VoteVSCanceller.findWhere(voteVS:voteVS)
				}
				voteVSMap.cancellerURL="${grailsApplication.config.grails.serverURL}/messageSMIME/${voteVSCanceller?.messageSMIME?.id}"
			}
			render voteVSMap as JSON
			return
		}
        params.responseVS = new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))
	}

	
	/*
	 def testAsync () {
		 log.debug "Arranco controlador"
		 def aCtx = startAsync()
		 aCtx.setTimeout(ResponseVS.SC_ERROR0);
		 //aCtx.complete()
		 render "Todo ok"
	 }
	 
	 def post () {
	 	 MimeMessage smimeMessageReq = params.smimeMessageReq
		 Respuesta responseVS = voteVSService.validarFirmaUsuario(
			 smimeMessageReq, request.getLocale())
		 if (ResponseVS.SC_OK== responseVS.statusCode) {
			 def ctx = startAsync()
			 ctx.setTimeout(10000);
			 
			 EventVS eventVS = responseVS.eventVS
			 def future = callAsync {
				  return voteVSService.sendVoteToControlAccess(
				  smimeMessage, eventVS, request.getLocale())
			 }
			 responseVS = future.get()
			 if (ResponseVS.SC_OK == responseVS?.statusCode) {
				 ctx.response.status = ResponseVS.SC_OK
				 ctx.response.setContentType(ContentTypeVS.SIGNED_AND_ENCRYPTED.getName())
				 ctx.response.contentLength = responseVS.voteVS.messageSMIME.content.length
				 ctx.response.outputStream <<  responseVS.voteVS.messageSMIME.content
				 ctx.response.outputStream.flush()
			 } 
			 ctx.complete();
		 } else if (ResponseVS.SC_ERROR_VOTE_REPEATED == responseVS.statusCode){
			 response.status = ResponseVS.SC_ERROR_VOTE_REPEATED
			 response.contentLength = responseVS.voteVS.messageSMIME.content.length
			 response.outputStream <<  responseVS.voteVS.messageSMIME.content
			 response.outputStream.flush()
			 return false
		 }
	 }*/
	
}