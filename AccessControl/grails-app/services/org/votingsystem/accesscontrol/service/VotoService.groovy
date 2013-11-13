package org.votingsystem.accesscontrol.service

import javax.mail.internet.MimeMessage;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.groovy.util.StringUtils;
import grails.converters.JSON;
import org.votingsystem.model.ContextVS;
import java.util.Locale;
import java.util.Map;
import org.codehaus.groovy.grails.web.json.JSONObject
import javax.mail.internet.InternetAddress;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.cert.X509Certificate;
import org.votingsystem.groovy.util.VotingSystemApplicationContex


class VotoService {
	
	def messageSource
	def grailsApplication
	def firmaService
	def httpService
	def encryptionService
	
    synchronized ResponseVS validateVote(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug ("validateVote - ")
		EventoVotacion evento = messageSMIMEReq.evento
		String localServerURL = grailsApplication.config.grails.serverURL
		String msg
		try {
			SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
			Certificado certificadoVoto = smimeMessageReq.getVoteVS().getCertificado()
			
			
			def votoJSON = JSON.parse(smimeMessageReq.getSignedContent())
			OpcionDeEvento opcionSeleccionada =
				evento.comprobarOpcionId(Long.valueOf(votoJSON.opcionSeleccionadaId))
			if (!opcionSeleccionada) {
				msg = messageSource.getMessage('validacionVoto.errorOptionSelected', 
					[votoJSON.opcionSeleccionadaId].toArray(), locale)
				log.error ("validateVote - ERROR OPTION -> '${msg}'")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, type:TypeVS.VOTE_ERROR, eventVS:evento)
			}

			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = evento.centroControl.serverURL
			String subject = messageSource.getMessage(
				'validacionVoto.smimeMessageSubject', null, locale)
			smimeMessageReq.setMessageID("${localServerURL}/messageSMIME/${messageSMIMEReq.id}")
						
			SMIMEMessageWrapper smimeMessageResp = firmaService.getMultiSignedMimeMessage(
				fromUser,toUser, smimeMessageReq, subject)
			messageSMIMEReq.type = TypeVS.VOTO_VALIDADO_ACCESS_CONTROL
			messageSMIMEReq.contenido = smimeMessageResp.getBytes()
			MessageSMIME messageSMIMEResp = messageSMIMEReq
			MessageSMIME.withTransaction {
				messageSMIMEResp.save()
			}
			
			certificadoVoto.estado = Certificado.Estado.UTILIZADO;
			Voto voto = new Voto(opcionDeEvento:opcionSeleccionada,
				eventoVotacion:evento, estado:Voto.Estado.OK,
				certificado:certificadoVoto, messageSMIME:messageSMIMEResp)
			Voto.withTransaction {
				voto.save()
			}
			X509Certificate controlCenterCert = smimeMessageReq.getVoteVS()?.
				getServerCerts()?.iterator()?.next()				
			Map data = [certificate:certificado, messageSMIME:messageSMIMEResp]
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:evento, 
				type:TypeVS.VOTO_VALIDADO_ACCESS_CONTROL, data:data)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:messageSource.getMessage('voteErrorMsg', null, locale), 
				type:TypeVS.VOTE_ERROR, eventVS:evento)
		}
    }
	
	private ResponseVS checkCancelJSONData(JSONObject cancelDataJSON) {
		def origenHashCertificadoVoto = cancelDataJSON.origenHashCertificadoVoto
		def hashCertificadoVotoBase64 = cancelDataJSON.hashCertificadoVotoBase64
		def origenHashSolicitudAcceso = cancelDataJSON.origenHashSolicitudAcceso
		def hashSolicitudAccesoBase64 = cancelDataJSON.hashSolicitudAccesoBase64
		if(!origenHashCertificadoVoto || !hashCertificadoVotoBase64 ||
			!origenHashSolicitudAcceso || !hashSolicitudAccesoBase64) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:messageSource.getMessage('anulacionVoto.dataError', null, locale), 
				type:TypeVS.CANCEL_VOTE_ERROR)
		}
		def hashCertificadoVoto = CMSUtils.obtenerHashBase64(origenHashCertificadoVoto,
			"${grailsApplication.config.VotingSystem.votingHashAlgorithm}")
		def hashSolicitud = CMSUtils.obtenerHashBase64(origenHashSolicitudAcceso,
			"${grailsApplication.config.VotingSystem.votingHashAlgorithm}")
		if (!hashSolicitudAccesoBase64.equals(hashSolicitud))
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
				'anulacionVoto.accessRequestHashError', null, locale), type:TypeVS.CANCEL_VOTE_ERROR)
		if (!hashCertificadoVotoBase64.equals(hashCertificadoVoto))
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.getMessage(
					'anulacionVoto.errorEnHashCertificado', null, locale), type:TypeVS.CANCEL_VOTE_ERROR)
		return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}
	
	private ResponseVS checkCancelResponseJSONData(
		JSONObject requestDataJSON, JSONObject responseDataJSON) {
		ResponseVS respuesta = checkCancelJSONData(responseDataJSON)
		if(ResponseVS.SC_OK != respuesta.statusCode) return respuesta
		if(!(requestDataJSON.hashCertificadoVotoBase64.equals(
			responseDataJSON.hashCertificadoVotoBase64)) || 
			!(requestDataJSON.hashSolicitudAccesoBase64.equals(
			responseDataJSON.hashSolicitudAccesoBase64))){
			String msg = messageSource.getMessage(
					'cancelDataWithErrorsMsg', null, locale)
			log.error("checkCancelResponseJSONData - requestDataJSON: '${requestDataJSON}'" + 
				" - responseDataJSON: '${responseDataJSON}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:msg, type:TypeVS.CANCEL_VOTE_ERROR)
		} else return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}
	
	public synchronized ResponseVS processCancel (MessageSMIME messageSMIME, Locale locale) {
		Usuario signer = messageSMIME.getUsuario();
		SMIMEMessageWrapper smimeMessage = messageSMIME.getSmimeMessage();
		log.debug ("processCancel - ${smimeMessage.getSignedContent()}")
		MessageSMIME messageSMIMEResp;
		EventoVotacion eventoVotacion;
		try {
			def cancelDataJSON = JSON.parse(messageSMIME.getSmimeMessage().getSignedContent())
			ResponseVS respuesta = checkCancelJSONData(cancelDataJSON)
			if(ResponseVS.SC_OK != respuesta.statusCode) return respuesta
			def hashCertificadoVotoBase64 = cancelDataJSON.hashCertificadoVotoBase64
			def hashSolicitudAccesoBase64 = cancelDataJSON.hashSolicitudAccesoBase64

			String msg
			def solicitudAcceso = SolicitudAcceso.findWhere(hashSolicitudAccesoBase64:hashSolicitudAccesoBase64)
			if (!solicitudAcceso) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:messageSource.getMessage('anulacionVoto.errorSolicitudNoEncontrada', null, locale),
				type:TypeVS.CANCEL_VOTE_ERROR)
			if(solicitudAcceso.estado.equals(SolicitudAcceso.Estado.ANULADO)) {
				msg = messageSource.getMessage(
					'anulacionVoto.errorSolicitudAnulada', null, locale)
				log.error("processCancel - ERROR ACCESS REQUEST ALREADY CANCELLED - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_CANCELLATION_REPEATED, 
					message:msg, type:TypeVS.CANCEL_VOTE_ERROR)
			}
			Certificado certificado = Certificado.findWhere(hashCertificadoVotoBase64:hashCertificadoVotoBase64)
			if (!certificado){
				msg = messageSource.getMessage(
					'anulacionVoto.errorSolicitudCSRNoEncontrada', null, locale)
				log.error("processCancel - ERROR CSR NOT FOUND - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.CANCEL_VOTE_ERROR)
			} 
			else eventoVotacion = certificado.eventoVotacion
			def voto = Voto.findWhere(certificado:certificado)
			AnuladorVoto anuladorVoto = AnuladorVoto.findWhere(
				hashCertificadoVotoBase64:hashCertificadoVotoBase64)
			if(anuladorVoto) {
				String voteURL = "${grailsApplication.config.grails.serverURL}/voto/${anuladorVoto.voto.id}"
				msg = messageSource.getMessage('voteAlreadyCancelled',
						[voteURL].toArray(), locale)
				log.error("processCancel - REAPEATED CANCEL REQUEST - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_CANCELLATION_REPEATED,
					data:anuladorVoto.messageSMIME, type:TypeVS.CANCEL_VOTE_ERROR,
					message:msg, eventVS:eventoVotacion)
			}
			Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().genTime
			if(!eventoVotacion.isActiveDate(timeStampDate)) {
				msg = messageSource.getMessage('timestampDateErrorMsg', 
					[timeStampDate, eventoVotacion.fechaInicio, 
						eventoVotacion.getDateFinish()].toArray(), locale)
				log.error("processCancel - DATE ERROR - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.CANCEL_VOTE_ERROR)
			}
			
			//smimeMessageResp -> Message no notificate cancelation to Control Center
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = eventoVotacion.centroControl.serverURL
			String subject = messageSource.getMessage(
				'mime.asunto.anulacionVotoValidada', null, locale)
			smimeMessage.setMessageID("${grailsApplication.config.grails.serverURL}" +
				"/messageSMIME/${messageSMIME.id}")
			SMIMEMessageWrapper smimeMessageResp = firmaService.getMultiSignedMimeMessage(
				fromUser, toUser, smimeMessage, subject)
			messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
				smimePadre:messageSMIME, evento:eventoVotacion, valido:true)
			AnuladorVoto.Estado estadoAnulador
			if(!voto){//Access request without vote
				msg = messageSource.getMessage('anulacionVoto.errorVotoNoEncontrado', null, locale)
				log.debug ("processCancel - VOTE NOT FOUND - ${msg}")
				messageSMIMEResp.contenido = smimeMessageResp.getBytes()
				estadoAnulador = AnuladorVoto.Estado.SIN_NOTIFICAR//Access request without vote
			} else {//Notify Control Center
				def cancelDataJSONResp
				String msgArg
				String centroControlURL = eventoVotacion.centroControl.serverURL
				String eventURL = "${grailsApplication.config.grails.serverURL}/eventoVotacion/${eventoVotacion.id}"
				String urlAnulacionVoto = "${centroControlURL}/anuladorVoto?url=${eventURL}"
				ResponseVS encryptResponse = encryptionService.encryptSMIMEMessage(
					smimeMessageResp.getBytes(), eventoVotacion.getControlCenterCert(), locale)
				if (ResponseVS.SC_OK != encryptResponse.statusCode) return encryptResponse
				ResponseVS respuestaCentroControl = httpService.sendMessage(
					encryptResponse.messageBytes, ContentTypeVS.SIGNED_AND_ENCRYPTED, urlAnulacionVoto)
				if (ResponseVS.SC_OK == respuestaCentroControl.statusCode) {	
					//decrypt response
					respuestaCentroControl = encryptionService.decryptSMIMEMessage(
							respuestaCentroControl.message.getBytes(), locale)
					if(ResponseVS.SC_OK != respuestaCentroControl.statusCode) {
						msgArg = messageSource.getMessage(
							'encryptedMessageErrorMsg', null, locale)
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[msgArg].toArray(), locale)
						log.debug ("processCancel --- Problem with response encryption - ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
								message:msg, eventVS:eventoVotacion, type:TypeVS.CANCEL_VOTE_ERROR)
					}
					smimeMessageResp = respuestaCentroControl.smimeMessage;//already decrypted
					//check message content
					cancelDataJSONResp = JSON.parse(smimeMessageResp.getSignedContent())
					respuesta = checkCancelResponseJSONData(cancelDataJSON, cancelDataJSONResp)
					if(ResponseVS.SC_OK != respuesta.statusCode) {
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[respuesta.message].toArray(), locale)
						log.debug ("processCancel - ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
								message:msg, eventVS:eventoVotacion, type:TypeVS.CANCEL_VOTE_ERROR)
					}
					messageSMIMEResp.contenido = smimeMessageResp.getBytes()
					estadoAnulador = AnuladorVoto.Estado.NOTIFICADO
				} else if(ResponseVS.SC_CANCELLATION_REPEATED) {
					respuestaCentroControl =  encryptionService.decryptSMIMEMessage(
						respuestaCentroControl.message.getBytes(), locale)
					if(ResponseVS.SC_OK != respuestaCentroControl.statusCode) {
						msgArg = messageSource.getMessage(
							'encryptedMessageErrorMsg', null, locale) 
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[msgArg].toArray(), locale)
						log.debug ("processCancel *** Problem with response encryption - ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
								message:msg, eventVS:eventoVotacion, type:TypeVS.CANCEL_VOTE_ERROR)
					}
					smimeMessageResp = respuestaCentroControl.smimeMessage
					ResponseVS respuestaValidacion = firmaService.
							validateSignersCertificate(smimeMessageResp, locale)
					if(!firmaService.isSystemSignedMessage((Set<Usuario>)respuestaValidacion.data)) {
						msgArg = messageSource.getMessage('unknownReceipt', null, locale)
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[msgArg].toArray(), locale)
						log.error("processCancel - Not local receipt - ${msg}")
						messageSMIMEResp = new MessageSMIME(valido:false,	
							type:TypeVS.RECEIPT_ERROR, contenido:smimeMessageResp.getBytes(),
							 evento:eventoVotacion, motivo:msg) 
						MessageSMIME.withTransaction {
							messageSMIMEResp.save()
						}
						return new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
							message:msg, eventVS:eventoVotacion, type:TypeVS.CANCEL_VOTE_ERROR)
					}
					cancelDataJSONResp = JSON.parse(smimeMessageResp.getSignedContent())
					respuesta = checkCancelResponseJSONData(cancelDataJSON, cancelDataJSONResp)
					if(ResponseVS.SC_OK != respuesta.statusCode) {
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[respuesta.message].toArray(), locale)
						log.error("processCancel - response data with errors - ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
							message:msg, eventVS:eventoVotacion, type:TypeVS.CANCEL_VOTE_ERROR)
					}		
					messageSMIMEResp.contenido = smimeMessageResp.getBytes()
					estadoAnulador = AnuladorVoto.Estado.NOTIFICADO
				} else {
					respuestaCentroControl.evento = eventoVotacion
					return respuestaCentroControl
				}
			}
			MessageSMIME.withTransaction {
				messageSMIMEResp.save()
			}
			anuladorVoto = new AnuladorVoto(
				messageSMIME:messageSMIMEResp, estado:estadoAnulador,
				solicitudAcceso:solicitudAcceso, solicitudCSRVoto:certificado.solicitudCSRVoto,
				origenHashSolicitudAccesoBase64:cancelDataJSON.origenHashSolicitudAcceso,
				origenHashCertificadoVotoBase64:cancelDataJSON.origenHashCertificadoVoto,
				hashSolicitudAccesoBase64:hashSolicitudAccesoBase64,
				hashCertificadoVotoBase64:hashCertificadoVotoBase64,
				eventoVotacion:eventoVotacion, voto:voto)
			if (!anuladorVoto.save()) {
			    anuladorVoto.errors.each { log.error("processCancel - error - ${it}")}
			}
			if(voto) {
				voto.estado = Voto.Estado.ANULADO
				voto.save()
			}
			solicitudAcceso.estado = SolicitudAcceso.Estado.ANULADO
			solicitudAcceso.save()			
			certificado.solicitudCSRVoto.estado = SolicitudCSRVoto.Estado.ANULADA
			certificado.estado = Certificado.Estado.ANULADO
			certificado.save()
			return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.CANCEL_VOTE, 
				data:messageSMIMEResp, eventVS:eventoVotacion)
		}catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:ex.getMessage(), eventVS:eventoVotacion, type:TypeVS.CANCEL_VOTE_ERROR)
		}
	}
	
	public Map getVotoMap(Voto voto) {
		if(!voto) return [:]
		HexBinaryAdapter hexConverter = new HexBinaryAdapter();
		String hashHex = hexConverter.marshal(
			voto.certificado?.hashCertificadoVotoBase64?.getBytes());
		Map votoMap = [id:voto.id,
			hashCertificadoVotoBase64:voto.certificado.hashCertificadoVotoBase64,
			opcionDeEventoId:voto.opcionDeEvento.id,
			eventoVotacionId:voto.eventoVotacion.id,
			eventoVotacionURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/${voto.eventoVotacion?.id}",
			estado:voto?.estado?.toString(),
			certificadoURL:"${grailsApplication.config.grails.serverURL}/certificado/voto/hashHex/${hashHex}",
			votoSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${voto.messageSMIME.id}"]
		if(Voto.Estado.ANULADO == voto?.estado) {
			votoMap.anulacionURL="${grailsApplication.config.grails.serverURL}/anuladorVoto/voto/${voto.id}"
		}
		return votoMap
	}
	
	public Map getAnuladorVotoMap(AnuladorVoto anulador) {
		if(!anulador) return [:]
		Map anuladorMap = [id:anulador.id,
			votoURL:"${grailsApplication.config.grails.serverURL}/voto/${anulador.voto.id}",
			anuladorSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${anulador.messageSMIME.id}"]
		return anuladorMap
	}
	
}