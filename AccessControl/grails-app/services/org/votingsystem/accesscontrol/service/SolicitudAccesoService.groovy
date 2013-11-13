package org.votingsystem.accesscontrol.service

import grails.converters.JSON;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream

import javax.mail.internet.MimeMessage;

import org.votingsystem.util.DateUtils;
import org.bouncycastle.tsp.TimeStampToken;
import org.codehaus.groovy.grails.web.json.JSONObject;
import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.*;

import java.security.cert.X509Certificate;
import java.util.Locale;

import org.votingsystem.groovy.util.VotingSystemApplicationContex

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class SolicitudAccesoService {
	
	//static scope = "request"

    static transactional = true
	
	def messageSource
    def firmaService
    def grailsApplication
	def encryptionService
	def timeStampService
	
	//{"operation":"ACCESS_REQUEST","hashSolicitudAccesoBase64":"...",
	// "eventId":"..","eventURL":"...","UUID":"..."}
	private ResponseVS checkAccessRequestJSONData(JSONObject accessDataJSON, Locale locale) {
		int status = ResponseVS.SC_ERROR_REQUEST
		TypeVS typeRespuesta = TypeVS.ACCESS_REQUEST_ERROR
		org.bouncycastle.tsp.TimeStampToken tms;
		String msg
		try {
			TypeVS operationType = TypeVS.valueOf(accessDataJSON.operation)
			if (accessDataJSON.eventId && accessDataJSON.eventURL &&
				accessDataJSON.hashSolicitudAccesoBase64 && 
				(TypeVS.ACCESS_REQUEST == operationType)) {
				status = ResponseVS.SC_OK
			} else msg = messageSource.getMessage('accessRequestWithErrorsMsg', null, locale)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('accessRequestWithErrorsMsg', null, locale)
		}
		if(ResponseVS.SC_OK == status) typeRespuesta = TypeVS.ACCESS_REQUEST
		else log.error("checkAccessRequestJSONData - msg: ${msg} - data:${accessDataJSON.toString()}")
		return new ResponseVS(statusCode:status, message:msg, type:typeRespuesta)
	}
	
    ResponseVS saveRequest(MessageSMIME messageSMIMEReq, Locale locale) {
		Usuario firmante = messageSMIMEReq.getUsuario()
		log.debug("saveRequest - firmante: ${firmante.nif}")
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		String msg
        try {
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			ResponseVS respuesta = checkAccessRequestJSONData(messageJSON, locale)
			if(ResponseVS.SC_OK !=  respuesta.statusCode) return respuesta
			def hashSolicitudAccesoBase64
			def typeRespuesta
			def solicitudAcceso
			def eventoVotacion
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.findById(Long.valueOf(messageJSON.eventId))
			}
			if (eventoVotacion) {
				if (!eventoVotacion.isOpen(DateUtils.getTodayDate())) {
					msg = messageSource.getMessage('evento.messageCerrado', null, locale)
					log.error("saveRequest - EVENT CLOSED - ${msg}")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
						type:TypeVS.ACCESS_REQUEST_ERROR, message:msg)
				}
				SolicitudAcceso.withTransaction {
					solicitudAcceso = SolicitudAcceso.findWhere(usuario:firmante,
						eventoVotacion:eventoVotacion, estado:TypeVS.OK)
				}
				if (solicitudAcceso){//Ha votado el usuario?
						msg = "${grailsApplication.config.grails.serverURL}/messageSMIME/${solicitudAcceso.messageSMIME.id}"
						log.error("saveRequest - ACCESS REQUEST ERROR - ${msg}")
						return new ResponseVS(solicitudAcceso:solicitudAcceso, 
							type:TypeVS.ACCESS_REQUEST_ERROR, message:msg, eventVS:eventoVotacion,
							statusCode:ResponseVS.SC_ERROR_VOTE_REPEATED)
				} else {
					//TimeStamp comes cert validated from filters. Check date
					respuesta = timeStampService.validateToken(firmante.getTimeStampToken(), 
						eventoVotacion, locale)
					log.debug("saveRequest - validateTokenDate status: ${respuesta.statusCode}")
					if(ResponseVS.SC_OK != respuesta.statusCode) {
						log.error("saveRequest - ERROR TOKEN DATE VALIDATION -${respuesta.message}")
						return new ResponseVS(solicitudAcceso:solicitudAcceso, 
							type:TypeVS.ACCESS_REQUEST_ERROR, 
							message:espuesta.message, eventVS:eventoVotacion,
							statusCode:ResponseVS.SC_ERROR_VOTE_REPEATED) 
					}
					//es el hash único?
					hashSolicitudAccesoBase64 = messageJSON.hashSolicitudAccesoBase64
					boolean hashSolicitudAccesoRepetido = (SolicitudAcceso.findWhere(
							hashSolicitudAccesoBase64:hashSolicitudAccesoBase64) != null)
					if (hashSolicitudAccesoRepetido) {
						msg = messageSource.getMessage('error.HashRepetido', null, locale)
						log.error("saveRequest -ERROR ACCESS REQUEST HAS REPEATED -> ${hashSolicitudAccesoBase64} - ${msg}")
						return new ResponseVS(type:TypeVS.ACCESS_REQUEST_ERROR, message:msg,
								statusCode:ResponseVS.SC_ERROR_REQUEST, eventVS:eventoVotacion)
					} else {//Todo OK
					
					VotingEvent votingEvent = null
					if(Usuario.Type.REPRESENTATIVE == firmante.type) {
						votingEvent = VotingEvent.ACCESS_REQUEST_REPRESENTATIVE.setData(
							firmante, eventoVotacion)
					} else if(firmante.representative) {
						votingEvent = VotingEvent.ACCESS_REQUEST_USER_WITH_REPRESENTATIVE.setData(
							firmante, eventoVotacion)
					} else {
						votingEvent = VotingEvent.ACCESS_REQUEST.setData(
							firmante, eventoVotacion)
					}
					
					solicitudAcceso = new SolicitudAcceso(usuario:firmante,
						messageSMIME:messageSMIMEReq,
						estado: SolicitudAcceso.Estado.OK,
						hashSolicitudAccesoBase64:hashSolicitudAccesoBase64,
						eventoVotacion:eventoVotacion)
					SolicitudAcceso.withTransaction {
						if (!solicitudAcceso.save()) {
							solicitudAcceso.errors.each { log.error("- saveRequest - ERROR - ${it}")}
						}
					}
					return new ResponseVS(type:TypeVS.ACCESS_REQUEST,
							statusCode:ResponseVS.SC_OK, eventVS:eventoVotacion,
							solicitudAcceso:solicitudAcceso)
					}
				}
			} else {
				msg = messageSource.getMessage( 'eventNotFound',
							[messageJSON.eventId].toArray(), locale)
				log.error("saveRequest - Event Id not found - > ${messageJSON.eventId} - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
						type:TypeVS.ACCESS_REQUEST_ERROR, message:msg)
			}
		}catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
					type:TypeVS.ACCESS_REQUEST_ERROR,
					message:messageSource.getMessage(
					'accessRequestWithErrorsMsg', null, locale))
		}
    }
	
	def rechazarSolicitud(SolicitudAcceso solicitudAcceso, String detalles) {
		log.debug("rechazarSolicitud '${solicitudAcceso.id}'")
		solicitudAcceso.detalles = detalles
		solicitudAcceso = solicitudAcceso.merge()
		solicitudAcceso.estado = SolicitudAcceso.Estado.ANULADO
		solicitudAcceso.save()
	}

}