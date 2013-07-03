package org.sistemavotacion.controlacceso

import javax.mail.internet.MimeMessage;
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.utils.StringUtils;
import grails.converters.JSON;
import java.util.Locale;
import java.util.Map;
import org.codehaus.groovy.grails.web.json.JSONObject
import javax.mail.internet.InternetAddress;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.security.cert.X509Certificate;
import org.sistemavotacion.utils.VotingSystemApplicationContex


class VotoService {
	
	def messageSource
	def grailsApplication
	def firmaService
	def httpService
	def encryptionService
	
    synchronized Respuesta validateVote(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		log.debug ("validateVote - ")
		EventoVotacion evento = mensajeSMIMEReq.evento
		String localServerURL = grailsApplication.config.grails.serverURL
		String msg
		try {
			SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()
			Certificado certificadoVoto = smimeMessageReq.getInformacionVoto().getCertificado()
			
			
			def votoJSON = JSON.parse(smimeMessageReq.getSignedContent())
			OpcionDeEvento opcionSeleccionada =
				evento.comprobarOpcionId(Long.valueOf(votoJSON.opcionSeleccionadaId))
			if (!opcionSeleccionada) {
				msg = messageSource.getMessage('validacionVoto.errorOptionSelected', 
					[votoJSON.opcionSeleccionadaId].toArray(), locale)
				log.error ("validateVote - ERROR OPTION -> '${msg}'")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
			}

			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = evento.centroControl.serverURL
			String subject = messageSource.getMessage(
				'validacionVoto.smimeMessageSubject', null, locale)
			smimeMessageReq.setMessageID("${localServerURL}/mensajeSMIME/${mensajeSMIMEReq.id}")
						
			SMIMEMessageWrapper smimeMessageResp = firmaService.getMultiSignedMimeMessage(
				fromUser,toUser, smimeMessageReq, subject)
			mensajeSMIMEReq.tipo = Tipo.VOTO_VALIDADO_CONTROL_ACCESO
			mensajeSMIMEReq.contenido = smimeMessageResp.getBytes()
			MensajeSMIME mensajeSMIMEResp = mensajeSMIMEReq
			MensajeSMIME.withTransaction {
				mensajeSMIMEResp.save()
			}
			
			certificadoVoto.estado = Certificado.Estado.UTILIZADO;
			Voto voto = new Voto(opcionDeEvento:opcionSeleccionada,
				eventoVotacion:evento, estado:Voto.Estado.OK,
				certificado:certificadoVoto, mensajeSMIME:mensajeSMIMEResp)
			Voto.withTransaction {
				voto.save()
			}
			X509Certificate controlCenterCert = smimeMessageReq.getInformacionVoto()?.
				getServerCerts()?.iterator()?.next()				
				
			return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento, 
				tipo:Tipo.VOTO_VALIDADO_CONTROL_ACCESO,
				certificado:controlCenterCert, mensajeSMIME:mensajeSMIMEResp)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:messageSource.getMessage('voteErrorMsg', null, locale), 
				tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
		}
    }
	
	private Respuesta checkCancelJSONData(JSONObject cancelDataJSON) {
		def origenHashCertificadoVoto = cancelDataJSON.origenHashCertificadoVoto
		def hashCertificadoVotoBase64 = cancelDataJSON.hashCertificadoVotoBase64
		def origenHashSolicitudAcceso = cancelDataJSON.origenHashSolicitudAcceso
		def hashSolicitudAccesoBase64 = cancelDataJSON.hashSolicitudAccesoBase64
		if(!origenHashCertificadoVoto || !hashCertificadoVotoBase64 ||
			!origenHashSolicitudAcceso || !hashSolicitudAccesoBase64) {
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:messageSource.getMessage('anulacionVoto.dataError', null, locale), 
				tipo:Tipo.ANULADOR_VOTO_ERROR)
		}
		def hashCertificadoVoto = CMSUtils.obtenerHashBase64(origenHashCertificadoVoto,
			"${grailsApplication.config.SistemaVotacion.votingHashAlgorithm}")
		def hashSolicitud = CMSUtils.obtenerHashBase64(origenHashSolicitudAcceso,
			"${grailsApplication.config.SistemaVotacion.votingHashAlgorithm}")
		if (!hashSolicitudAccesoBase64.equals(hashSolicitud))
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:messageSource.getMessage(
				'anulacionVoto.accessRequestHashError', null, locale), tipo:Tipo.ANULADOR_VOTO_ERROR)
		if (!hashCertificadoVotoBase64.equals(hashCertificadoVoto))
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:messageSource.getMessage(
					'anulacionVoto.errorEnHashCertificado', null, locale), tipo:Tipo.ANULADOR_VOTO_ERROR)
		return new Respuesta(codigoEstado:Respuesta.SC_OK)
	}
	
	private Respuesta checkCancelResponseJSONData(
		JSONObject requestDataJSON, JSONObject responseDataJSON) {
		Respuesta respuesta = checkCancelJSONData(responseDataJSON)
		if(Respuesta.SC_OK != respuesta.codigoEstado) return respuesta
		if(!(requestDataJSON.hashCertificadoVotoBase64.equals(
			responseDataJSON.hashCertificadoVotoBase64)) || 
			!(requestDataJSON.hashSolicitudAccesoBase64.equals(
			responseDataJSON.hashSolicitudAccesoBase64))){
			String msg = messageSource.getMessage(
					'cancelDataWithErrorsMsg', null, locale)
			log.error("checkCancelResponseJSONData - requestDataJSON: '${requestDataJSON}'" + 
				" - responseDataJSON: '${responseDataJSON}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:msg, tipo:Tipo.ANULADOR_VOTO_ERROR)
		} else return new Respuesta(codigoEstado:Respuesta.SC_OK)
	}
	
	public synchronized Respuesta processCancel (MensajeSMIME mensajeSMIME, Locale locale) {
		Usuario signer = mensajeSMIME.getUsuario();
		SMIMEMessageWrapper smimeMessage = mensajeSMIME.getSmimeMessage();
		log.debug ("processCancel - ${smimeMessage.getSignedContent()}")
		MensajeSMIME mensajeSMIMEResp;
		EventoVotacion eventoVotacion;
		try {
			def cancelDataJSON = JSON.parse(mensajeSMIME.getSmimeMessage().getSignedContent())
			Respuesta respuesta = checkCancelJSONData(cancelDataJSON)
			if(Respuesta.SC_OK != respuesta.codigoEstado) return respuesta
			def hashCertificadoVotoBase64 = cancelDataJSON.hashCertificadoVotoBase64
			def hashSolicitudAccesoBase64 = cancelDataJSON.hashSolicitudAccesoBase64

			String msg
			def solicitudAcceso = SolicitudAcceso.findWhere(hashSolicitudAccesoBase64:hashSolicitudAccesoBase64)
			if (!solicitudAcceso) return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:messageSource.getMessage('anulacionVoto.errorSolicitudNoEncontrada', null, locale),
				tipo:Tipo.ANULADOR_VOTO_ERROR)
			if(solicitudAcceso.estado.equals(SolicitudAcceso.Estado.ANULADO)) {
				msg = messageSource.getMessage(
					'anulacionVoto.errorSolicitudAnulada', null, locale)
				log.error("processCancel - ERROR ACCESS REQUEST ALREADY CANCELLED - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ANULACION_REPETIDA, 
					mensaje:msg, tipo:Tipo.ANULADOR_VOTO_ERROR)
			}
			Certificado certificado = Certificado.findWhere(hashCertificadoVotoBase64:hashCertificadoVotoBase64)
			if (!certificado){
				msg = messageSource.getMessage(
					'anulacionVoto.errorSolicitudCSRNoEncontrada', null, locale)
				log.error("processCancel - ERROR CSR NOT FOUND - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:msg, tipo:Tipo.ANULADOR_VOTO_ERROR)
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
				return new Respuesta(codigoEstado:Respuesta.SC_ANULACION_REPETIDA,
					mensajeSMIME:anuladorVoto.mensajeSMIME, tipo:Tipo.ANULADOR_VOTO_ERROR,
					mensaje:msg, evento:eventoVotacion)
			}
			Date timeStampDate = signer.getTimeStampToken().getTimeStampInfo().genTime
			if(!eventoVotacion.isActiveDate(timeStampDate)) {
				msg = messageSource.getMessage('timestampDateErrorMsg', 
					[timeStampDate, eventoVotacion.fechaInicio, 
						eventoVotacion.getDateFinish()].toArray(), locale)
				log.error("processCancel - DATE ERROR - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:msg, tipo:Tipo.ANULADOR_VOTO_ERROR)
			}
			
			//smimeMessageResp -> Message no notificate cancelation to Control Center
			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = eventoVotacion.centroControl.serverURL
			String subject = messageSource.getMessage(
				'mime.asunto.anulacionVotoValidada', null, locale)
			smimeMessage.setMessageID("${grailsApplication.config.grails.serverURL}" +
				"/mensajeSMIME/${mensajeSMIME.id}")
			SMIMEMessageWrapper smimeMessageResp = firmaService.getMultiSignedMimeMessage(
				fromUser, toUser, smimeMessage, subject)
			mensajeSMIMEResp = new MensajeSMIME(tipo:Tipo.RECIBO,
				smimePadre:mensajeSMIME, evento:eventoVotacion, valido:true)
			AnuladorVoto.Estado estadoAnulador
			if(!voto){//Access request without vote
				msg = messageSource.getMessage('anulacionVoto.errorVotoNoEncontrado', null, locale)
				log.debug ("processCancel - VOTE NOT FOUND - ${msg}")
				mensajeSMIMEResp.contenido = smimeMessageResp.getBytes()
				estadoAnulador = AnuladorVoto.Estado.SIN_NOTIFICAR//Access request without vote
			} else {//Notify Control Center
				def cancelDataJSONResp
				String msgArg
				String centroControlURL = eventoVotacion.centroControl.serverURL
				String urlAnulacionVoto = "${centroControlURL}/anuladorVoto"
				Respuesta encryptResponse = encryptionService.encryptSMIMEMessage(
					smimeMessageResp, eventoVotacion.getControlCenterCert(), locale)
				if (Respuesta.SC_OK != encryptResponse.codigoEstado) return encryptResponse
				String contentType = "${grailsApplication.config.pkcs7SignedContentType};" +
					"${grailsApplication.config.pkcs7EncryptedContentType}"
				Respuesta respuestaCentroControl = httpService.sendMessage(
					encryptResponse.messageBytes, contentType, urlAnulacionVoto)
				if (Respuesta.SC_OK == respuestaCentroControl.codigoEstado) {	
					//decrypt response
					respuestaCentroControl = encryptionService.decryptSMIMEMessage(
							respuestaCentroControl.mensaje.getBytes(), locale)
					if(Respuesta.SC_OK != respuestaCentroControl.codigoEstado) {
						msgArg = messageSource.getMessage(
							'encryptedMessageErrorMsg', null, locale)
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[msgArg].toArray(), locale)
						log.debug ("processCancel --- Problem with response encryption - ${msg}")
						return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
								mensaje:msg, evento:eventoVotacion, tipo:Tipo.ANULADOR_VOTO_ERROR)
					}
					smimeMessageResp = respuestaCentroControl.smimeMessage;//already decrypted
					//check message content
					cancelDataJSONResp = JSON.parse(smimeMessageResp.getSignedContent())
					respuesta = checkCancelResponseJSONData(cancelDataJSON, cancelDataJSONResp)
					if(Respuesta.SC_OK != respuesta.codigoEstado) {
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[respuesta.mensaje].toArray(), locale)
						log.debug ("processCancel - ${msg}")
						return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
								mensaje:msg, evento:eventoVotacion, tipo:Tipo.ANULADOR_VOTO_ERROR)
					}
					mensajeSMIMEResp.contenido = smimeMessageResp.getBytes()
					estadoAnulador = AnuladorVoto.Estado.NOTIFICADO
				} else if(Respuesta.SC_ANULACION_REPETIDA) {
					respuestaCentroControl =  encryptionService.decryptSMIMEMessage(
						respuestaCentroControl.mensaje.getBytes(), locale)
					if(Respuesta.SC_OK != respuestaCentroControl.codigoEstado) {
						msgArg = messageSource.getMessage(
							'encryptedMessageErrorMsg', null, locale) 
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[msgArg].toArray(), locale)
						log.debug ("processCancel *** Problem with response encryption - ${msg}")
						return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
								mensaje:msg, evento:eventoVotacion, tipo:Tipo.ANULADOR_VOTO_ERROR)
					}
					smimeMessageResp = respuestaCentroControl.smimeMessage
					Respuesta respuestaValidacion = firmaService.
							validateSignersCertificate(smimeMessageResp, locale)
					if(!firmaService.isSystemSignedMessage(respuestaValidacion.usuarios)) {
						msgArg = messageSource.getMessage('unknownReceipt', null, locale)
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[msgArg].toArray(), locale)
						log.error("processCancel - Not local receipt - ${msg}")
						mensajeSMIMEResp = new MensajeSMIME(valido:false,	
							tipo:Tipo.RECIBO_ERROR, contenido:smimeMessageResp.getBytes(),
							 evento:eventoVotacion, motivo:msg) 
						MensajeSMIME.withTransaction {
							mensajeSMIMEResp.save()
						}
						return new Respuesta(codigoEstado: Respuesta.SC_ERROR_PETICION,
							mensaje:msg, evento:eventoVotacion, tipo:Tipo.ANULADOR_VOTO_ERROR)
					}
					cancelDataJSONResp = JSON.parse(smimeMessageResp.getSignedContent())
					respuesta = checkCancelResponseJSONData(cancelDataJSON, cancelDataJSONResp)
					if(Respuesta.SC_OK != respuesta.codigoEstado) {
						msg = messageSource.getMessage('controCenterCommunicationErrorMsg',
							[respuesta.mensaje].toArray(), locale)
						log.error("processCancel - response data with errors - ${msg}")
						return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
							mensaje:msg, evento:eventoVotacion, tipo:Tipo.ANULADOR_VOTO_ERROR)
					}		
					mensajeSMIMEResp.contenido = smimeMessageResp.getBytes()
					estadoAnulador = AnuladorVoto.Estado.NOTIFICADO
				} else {
					respuestaCentroControl.evento = eventoVotacion
					return respuestaCentroControl
				}
			}
			MensajeSMIME.withTransaction {
				mensajeSMIMEResp.save()
			}
			anuladorVoto = new AnuladorVoto(
				mensajeSMIME:mensajeSMIMEResp, estado:estadoAnulador,
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
			return new Respuesta(codigoEstado:Respuesta.SC_OK, anuladorVoto:anuladorVoto, 
				mensajeSMIME:mensajeSMIMEResp, evento:eventoVotacion, tipo:Tipo.ANULADOR_VOTO)
		}catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:ex.getMessage(), evento:eventoVotacion, tipo:Tipo.ANULADOR_VOTO_ERROR)
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
			votoSMIMEURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/${voto.mensajeSMIME.id}"]
		if(Voto.Estado.ANULADO == voto?.estado) {
			votoMap.anulacionURL="${grailsApplication.config.grails.serverURL}/anuladorVoto/voto/${voto.id}"
		}
		return votoMap
	}
	
	public Map getAnuladorVotoMap(AnuladorVoto anulador) {
		if(!anulador) return [:]
		Map anuladorMap = [id:anulador.id,
			votoURL:"${grailsApplication.config.grails.serverURL}/voto/${anulador.voto.id}",
			anuladorSMIMEURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/${anulador.mensajeSMIME.id}"]
		return anuladorMap
	}
	
}