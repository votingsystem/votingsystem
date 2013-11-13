package org.votingsystem.controlcenter.service

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.Set;


import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.codehaus.groovy.grails.web.json.JSONArray;
import org.codehaus.groovy.grails.web.json.JSONObject;
import org.votingsystem.controlcenter.model.*
import org.springframework.context.*

import groovyx.net.http.*

import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.cms.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.*;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import grails.converters.JSON

import org.votingsystem.util.*
import org.votingsystem.signature.smime.*;
import org.springframework.beans.factory.DisposableBean;

import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* */
class EventoVotacionService {
	
    static transactional = false
	
	//static scope = "session"
	
	def messageSource
	def subscripcionService
    def grailsApplication  
	def httpService
	def etiquetaService
	def firmaService
	
	List<String> administradoresSistema
	
	ResponseVS saveEvent(MessageSMIME messageSMIMEReq, Locale locale) {		
        log.debug("- saveEvent")
		ResponseVS respuesta
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		String msg
		try {
			ControlAcceso controlAcceso = subscripcionService.checkAccessControl(
				smimeMessageReq.getHeader("serverURL")[0])
			if(!controlAcceso) {
				msg = message(code:'accessControlNotFound', args:[serverURL])
				log.debug("- saveEvent - ${msg}")
				return new ResponseVS(type:TypeVS.VOTING_EVENT_ERROR,
					message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
			}
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			if(!messageJSON.certCAVotacion || !messageJSON.usuario ||
				!messageJSON.id || !messageJSON.opciones || !messageJSON.URL ||
				!messageJSON.centroControl) {
				msg = messageSource.getMessage('documentParamsErrorMsg', null, locale)
				log.error("saveEvent - ERROR - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, type:TypeVS.VOTING_EVENT_ERROR)	
			}
			String serverURL = grailsApplication.config.grails.serverURL
			if (!serverURL.equals(messageJSON.centroControl?.serverURL)) {
				msg = messageSource.getMessage('localServerURLErrorMsg', 
					[messageJSON.centroControl?.serverURL, serverURL].toArray(), locale)
				log.error("saveEvent - ERROR - ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.VOTING_EVENT_ERROR)	
			}
			X509Certificate certCAVotacion = CertUtil.fromPEMToX509Cert(messageJSON.certCAVotacion?.bytes)
			byte[] cadenaCertificacion = messageJSON.cadenaCertificacion?.getBytes()
			X509Certificate userCert = CertUtil.fromPEMToX509Cert(messageJSON.usuario?.bytes)
			
			Usuario user = Usuario.getUsuario(userCert);
			//Publish request comes with Access Control cert
			respuesta = subscripcionService.checkUser(user, locale)
			if(ResponseVS.SC_OK != respuesta.statusCode) {
				log.error("saveEvent - USER CHECK ERROR - ${respuesta.message}")
				return  new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:respuesta.message, type:TypeVS.VOTING_EVENT_ERROR)	
			} 
			user = respuesta.usuario
			def evento = new EventoVotacion(eventoVotacionId:messageJSON.id,
				asunto:messageJSON.asunto, cadenaCertificacionControlAcceso:cadenaCertificacion,
				contenido:messageJSON.contenido, url:messageJSON.URL, controlAcceso:controlAcceso,
				usuario:user, fechaInicio:DateUtils.getDateFromString(messageJSON.fechaInicio),
				fechaFin:DateUtils.getDateFromString(messageJSON.fechaFin))
			respuesta = setEventDatesState(evento, locale)
			if(ResponseVS.SC_OK != respuesta.statusCode) {
				return  new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:respuesta.message, type:TypeVS.VOTING_EVENT_ERROR)
			}
			EventoVotacion.withTransaction { evento.save() }
			Certificado certificadoCAVotacion = new Certificado(esRaiz:true,
				actorConIP:controlAcceso, estado:Certificado.Estado.OK,
				type:Certificado.Type.RAIZ_VOTOS, eventoVotacion:evento,
				contenido:certCAVotacion.getEncoded(),
				numeroSerie:certCAVotacion.getSerialNumber().longValue(),
				validoDesde:certCAVotacion?.getNotBefore(),
				validoHasta:certCAVotacion?.getNotAfter())
			Certificado.withTransaction {certificadoCAVotacion.save()}
			salvarOpciones(evento, messageJSON)
			if (messageJSON.etiquetas) {
				Set<Etiqueta> etiquetas = etiquetaService.guardarEtiquetas(messageJSON.etiquetas)
				evento.setEtiquetaSet(etiquetas)
			}
			evento.save()
			log.debug("saveEvent - SAVED event - '${evento.id}'")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, 
				eventVS:evento, type:TypeVS.VOTING_EVENT)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message: messageSource.getMessage('saveDocumentoErrorMsg', null, locale),
				type:TypeVS.VOTING_EVENT_ERROR)
		}
	}
	
    Set<OpcionDeEvento> salvarOpciones(EventoVotacion evento, JSONObject json) {
        log.debug("salvarOpciones - ")
        def opcionesSet = json.opciones.collect { opcionItem ->
                def opcion = new OpcionDeEvento(eventoVotacion:evento,
                        contenido:opcionItem.contenido, opcionDeEventoId:opcionItem.id)
                return opcion.save();
        }
        return opcionesSet
    }

	ResponseVS setEventDatesState (EventoVotacion evento, Locale locale) {
		if(evento.fechaInicio.after(evento.fechaFin)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:messageSource.getMessage('dateRangeErrorMsg',
					[evento.fechaInicio, evento.fechaFin].toArray(), locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(evento.fechaFin)) evento.setEstado(
			EventoVotacion.Estado.FINALIZADO)
		if (fecha.after(evento.fechaInicio) && fecha.before(evento.fechaFin))
			evento.setEstado(EventoVotacion.Estado.ACTIVO)
		if (fecha.before(evento.fechaInicio)) evento.setEstado(
			EventoVotacion.Estado.PENDIENTE_COMIENZO)
		log.debug("setEventdatesState - estado ${evento.estado.toString()}")
		return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}
	
	
	ResponseVS comprobarFechasEvento (EventoVotacion evento, Locale locale) {
		log.debug("comprobarFechasEvento")
		if(evento.estado && evento.estado == EventoVotacion.Estado.CANCELADO) {
			return new ResponseVS(statusCode:ResponseVS.SC_OK, evento:evento)
		}
		if(evento.fechaInicio.after(evento.fechaFin)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:messageSource.getMessage(
                'error.fechaInicioAfterFechaFinalMsg', null, locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(evento.fechaFin) && 
			evento.estado != EventoVotacion.Estado.FINALIZADO) {
			EventoVotacion.withTransaction {
				evento.estado = EventoVotacion.Estado.FINALIZADO
				evento.save()
			}
		} else if(evento.fechaInicio.after(fecha) && 
			evento.estado != EventoVotacion.Estado.PENDIENTE_COMIENZO) {
			EventoVotacion.withTransaction {
				evento.estado = EventoVotacion.Estado.PENDIENTE_COMIENZO
				evento.save()
			}
		} else if(evento.fechaInicio.before(fecha) &&
			evento.fechaFin.after(fecha) && 
			evento.estado != EventoVotacion.Estado.ACTIVO) {
			EventoVotacion.withTransaction {
				evento.estado = EventoVotacion.Estado.ACTIVO
				evento.save()
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, evento:evento)
	}
	
	//{"operation":"EVENT_CANCELLATION","accessControlURL":"...","eventId":"..","estado":"CANCELADO","UUID":"..."}
	private ResponseVS checkCancelEventJSONData(JSONObject cancelDataJSON, Locale locale) {
		int status = ResponseVS.SC_ERROR_REQUEST
		TypeVS typeVS = TypeVS.EVENT_CANCELLATION_ERROR
		String msg
		try {
			TypeVS operationType = TypeVS.valueOf(cancelDataJSON.operation)
			if (cancelDataJSON.accessControlURL && cancelDataJSON.eventId &&
				cancelDataJSON.estado && (TypeVS.EVENT_CANCELLATION == operationType) &&
				((EventoVotacion.Estado.CANCELADO == EventoVotacion.Estado.valueOf(cancelDataJSON.estado)) ||
					(EventoVotacion.Estado.BORRADO_DE_SISTEMA == EventoVotacion.Estado.valueOf(cancelDataJSON.estado)))) {
				status = ResponseVS.SC_OK
			} else {
				msg = messageSource.getMessage(
					'evento.datosCancelacionError', null, locale)
			}
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('evento.datosCancelacionError', null, locale)
		}
		if(ResponseVS.SC_OK == status) typeVS = TypeVS.EVENT_CANCELLATION
		else log.error("checkCancelEventJSONData - msg: ${msg} - data:${cancelDataJSON.toString()}")
		return new ResponseVS(statusCode:status, message:msg, type:typeVS)
	}
	
	public ResponseVS cancelEvent(MessageSMIME messageSMIMEReq, Locale locale) {
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		Usuario signer = messageSMIMEReq.usuario
		EventoVotacion evento
		String msg
		try {
			log.debug("cancelEvent - message: ${smimeMessageReq.getSignedContent()}")
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			ResponseVS respuesta = checkCancelEventJSONData(messageJSON, locale)
			if(ResponseVS.SC_OK !=  respuesta.statusCode) return respuesta
			byte[] certChainBytes
			EventoVotacion.withTransaction {
				evento = EventoVotacion.findWhere(
					eventoVotacionId:messageJSON.eventId?.toString())
				certChainBytes = evento?.cadenaCertificacionControlAcceso
			}
			if(!evento) {
				msg = messageSource.getMessage('evento.eventoNotFound',
					[messageJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					type:TypeVS.EVENT_CANCELLATION_ERROR, message:msg)
			}
			if(evento.estado != EventoVotacion.Estado.ACTIVO) {
				msg = messageSource.getMessage('eventAllreadyCancelledMsg',
					[messageJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_CANCELLATION_REPEATED,
					type:TypeVS.EVENT_CANCELLATION_ERROR, message:msg)
			}
			
			Collection<X509Certificate> certColl = CertUtil.fromPEMToX509CertCollection(certChainBytes)
			X509Certificate accessControlCert = certColl.iterator().next()
			if(!firmaService.isSignerCertificate(messageSMIMEReq.getSigners(), accessControlCert)) {
				msg = messageSource.getMessage('eventCancelacionCertError', null, locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					type:TypeVS.EVENT_CANCELLATION_ERROR, message:msg, evento:evento)
			}
			//new state must be or CANCELLED or DELETED
			EventoVotacion.Estado newState = EventoVotacion.Estado.valueOf(messageJSON.estado)
			if(!(newState == EventoVotacion.Estado.BORRADO_DE_SISTEMA || 
				newState == EventoVotacion.Estado.CANCELADO)) {
				msg = messageSource.getMessage('eventCancelacionStateError', 
					[messageJSON.estado].toArray(), locale)
				log.error("cancelEvent new state error - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					type:TypeVS.EVENT_CANCELLATION_ERROR, message:msg, eventVS:evento)
			}
			//local receipt
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = evento.controlAcceso.serverURL
			String subject = messageSource.getMessage(
				'mime.asunto.cancelEventValidated', null, locale)
			
			SMIMEMessageWrapper smimeMessageResp = firmaService.
					getMultiSignedMimeMessage(fromUser, toUser, smimeMessageReq, subject)

						
			MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
				smimePadre:messageSMIMEReq, evento:evento, valido:true,
				contenido:smimeMessageResp.getBytes())
			if (!messageSMIMEResp.validate()) {
				messageSMIMEResp.errors.each {
					log.debug("messageSMIMEResp - error: ${it}")
				}
			}
			MessageSMIME.withTransaction {
				if (!messageSMIMEResp.save()) {
					messageSMIMEResp.errors.each {
						log.error("cancel event error saving messageSMIMEResp - ${it}")}
				}
			}
			evento.estado = newState
			evento.dateCanceled = DateUtils.getTodayDate();
			EventoVotacion.withTransaction { 
				if (!evento.save()) {
					evento.errors.each {
						log.error("cancel event error saving evento - ${it}")}
				}
			}
			log.debug("cancelEvent - cancelled event with id: ${evento.id}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK,message:msg,
				type:TypeVS.EVENT_CANCELLATION, data:messageSMIMEResp,
				eventVS:evento)			
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('evento.datosCancelacionError', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:msg, eventVS:evento, type:TypeVS.EVENT_CANCELLATION_ERROR)
		}
	}

	public Map optenerEventoVotacionJSONMap(EventoVotacion eventoItem) {
		def eventoMap = [id: eventoItem.id,
			fechaCreacion: eventoItem.dateCreated,
			asunto:eventoItem.asunto, contenido:eventoItem.contenido,
			etiquetas:eventoItem.etiquetaSet?.collect {etiquetaItem ->
					return [id:etiquetaItem.id, contenido:etiquetaItem.nombre]},
			duracion:DateUtils.getElapsedTime(eventoItem.getFechaInicio(),
			eventoItem.getFechaFin()),
			URL:eventoItem.url,
			estado:eventoItem.estado.toString(),
			fechaInicio:eventoItem.getFechaInicio(),
			fechaFin:eventoItem.getFechaFin(),
			eventoVotacionId:eventoItem.eventoVotacionId,
			certificadoCA_DeEvento:"${grailsApplication.config.grails.serverURL}/certificado/eventCA?eventAccessControlURL=${eventoItem.url}",
			informacionVotosEnControlAccesoURL:"${eventoItem.controlAcceso.serverURL}/eventoVotacion/${eventoItem.eventoVotacionId}/informacionVotos",
			informacionVotosURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/votes?eventAccessControlURL=${eventoItem.url}"]
			def controlAccesoMap = [serverURL:eventoItem.controlAcceso.serverURL,
					nombre:eventoItem.controlAcceso.nombre]
			eventoMap.controlAcceso = controlAccesoMap
			if(eventoItem.usuario) eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
			else eventoMap.usuario = null
		eventoMap.opciones = eventoItem.opciones?.collect {opcion ->
				return [id:opcion.id, contenido:opcion.contenido,
				opcionDeEventoId:opcion.opcionDeEventoId]}
		return eventoMap
	}
	
	boolean isUserAdmin(String nif) {
		log.debug("isUserAdmin - nif: ${nif}")
		if(!administradoresSistema) {
			administradoresSistema = Arrays.asList(
			"${grailsApplication.config.VotingSystem.adminsDNI}".split(","))
		}
		return administradoresSistema.contains(nif)
	}
}