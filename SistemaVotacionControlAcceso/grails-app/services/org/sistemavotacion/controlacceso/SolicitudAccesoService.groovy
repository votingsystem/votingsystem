package org.sistemavotacion.controlacceso

import grails.converters.JSON;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream
import javax.mail.internet.MimeMessage;

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.*;
import java.security.cert.X509Certificate;
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class SolicitudAccesoService {
	
	static scope = "request"

    static transactional = true
	
	def messageSource
    def firmaService
    def grailsApplication
	def encryptionService
	
	//{"operation":"SOLICITUD_ACCESO","hashSolicitudAccesoBase64":"...",
	// "eventId":"..","eventURL":"...","UUID":"..."}
	private Respuesta checkAccessRequestJSONData(JSONObject accessDataJSON, Locale locale) {
		int status = Respuesta.SC_ERROR_PETICION
		Tipo tipoRespuesta = Tipo.SOLICITUD_ACCESO_ERROR
		org.bouncycastle.tsp.TimeStampToken tms;
		String msg
		try {
			Tipo operationType = Tipo.valueOf(accessDataJSON.operation)
			if (accessDataJSON.eventId && accessDataJSON.eventURL &&
				accessDataJSON.hashSolicitudAccesoBase64 && 
				(Tipo.SOLICITUD_ACCESO == operationType)) {
				status = Respuesta.SC_OK
			} else msg = messageSource.getMessage('accessRequestWithErrorsMsg', null, locale)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('accessRequestWithErrorsMsg', null, locale)
		}
		if(Respuesta.SC_OK == status) tipoRespuesta = Tipo.SOLICITUD_ACCESO
		else log.error("checkAccessRequestJSONData - msg: ${msg} - data:${accessDataJSON.toString()}")
		return new Respuesta(codigoEstado:status, mensaje:msg, tipo:tipoRespuesta)
	}
	
    Respuesta saveRequest(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		Usuario firmante = mensajeSMIMEReq.getUsuario()
		log.debug("saveRequest - firmante: ${firmante.nif}")
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()
		String msg
        try {
			def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
			Respuesta respuesta = checkAccessRequestJSONData(mensajeJSON, locale)
			if(Respuesta.SC_OK !=  respuesta.codigoEstado) return respuesta
			def hashSolicitudAccesoBase64
			def tipoRespuesta
			def solicitudAcceso
			def eventoVotacion
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.findById(Long.valueOf(mensajeJSON.eventId))
			}
			if (eventoVotacion) {
				if (!eventoVotacion.isOpen()) {
					msg = messageSource.getMessage('evento.mensajeCerrado', null, locale)
					log.error("- saveRequest - EVENT CLOSED - ${msg}")
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
						tipo:Tipo.SOLICITUD_ACCESO_ERROR, mensaje:msg)
				}
				SolicitudAcceso.withTransaction {
					solicitudAcceso = SolicitudAcceso.findWhere(usuario:firmante,
						eventoVotacion:eventoVotacion, estado:Tipo.OK)
				}/*
				if (solicitudAcceso){//Ha votado el usuario?
						msg = "${grailsApplication.config.grails.serverURL}/mensajeSMIME/${solicitudAcceso.mensajeSMIME.id}"
						log.error("- saveRequest ==============- ACCESS REQUEST ERROR - ${msg}")
						/*return new Respuesta(solicitudAcceso:solicitudAcceso, 
							tipo:Tipo.SOLICITUD_ACCESO_ERROR, mensaje:msg, evento:eventoVotacion,
							codigoEstado:Respuesta.SC_ERROR_VOTO_REPETIDO)*/
				//} else {//es el hash único?*/
					hashSolicitudAccesoBase64 = mensajeJSON.hashSolicitudAccesoBase64
					boolean hashSolicitudAccesoRepetido = (SolicitudAcceso.findWhere(
							hashSolicitudAccesoBase64:hashSolicitudAccesoBase64) != null)
					if (hashSolicitudAccesoRepetido) {
						msg = messageSource.getMessage('error.HashRepetido', null, locale)
						log.error("- saveRequest -ERROR ACCESS REQUEST HAS REPEATED -> ${hashSolicitudAccesoBase64} - ${msg}")
						return new Respuesta(tipo:Tipo.SOLICITUD_ACCESO_ERROR, mensaje:msg,
								codigoEstado:Respuesta.SC_ERROR_PETICION, evento:eventoVotacion)
					} else {//Todo OK
						solicitudAcceso = new SolicitudAcceso(usuario:firmante,
							mensajeSMIME:mensajeSMIMEReq,
							estado: SolicitudAcceso.Estado.OK,
							hashSolicitudAccesoBase64:hashSolicitudAccesoBase64,
							eventoVotacion:eventoVotacion)
						SolicitudAcceso.withTransaction {
							if (!solicitudAcceso.save()) {
								solicitudAcceso.errors.each { log.error("- saveRequest - ERROR - ${it}")}
							}
						}
						return new Respuesta(tipo:Tipo.SOLICITUD_ACCESO,
								codigoEstado:Respuesta.SC_OK, evento:eventoVotacion,
								solicitudAcceso:solicitudAcceso)
					}
				//}
			} else {
				msg = messageSource.getMessage( 'eventNotFound',
							[mensajeJSON.eventId].toArray(), locale)
				log.error("- saveRequest - Event Id not found - > ${mensajeJSON.eventId} - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
						tipo:Tipo.SOLICITUD_ACCESO_ERROR, mensaje:msg)
			}
		}catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION,
					tipo:Tipo.SOLICITUD_ACCESO_ERROR,
					mensaje:messageSource.getMessage(
					'accessRequestWithErrorsMsg', null, locale))
		}
    }
	
	def rechazarSolicitud(SolicitudAcceso solicitudAcceso) {
		log.debug("rechazarSolicitud '${solicitudAcceso.id}'")
		solicitudAcceso = solicitudAcceso.merge()
		solicitudAcceso.estado = SolicitudAcceso.Estado.ANULADO
		solicitudAcceso.save()
	}

}