package org.votingsystem.accesscontrol.service

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.votingsystem.accesscontrol.model.Evento;
import org.votingsystem.accesscontrol.model.EventoFirma;
import org.votingsystem.accesscontrol.model.EventoReclamacion;
import org.votingsystem.accesscontrol.model.EventoVotacion;
import org.votingsystem.accesscontrol.model.OpcionDeEvento;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.*;
import org.votingsystem.groovy.util.VotingSystemApplicationContex;

import grails.converters.JSON
import groovy.util.ConfigObject;

import java.util.Locale;

import javax.mail.internet.InternetAddress

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EventoService {
		
	static transactional = true
	
	List<String> administradoresSistema
	def messageSource
	def subscripcionService
	def grailsApplication
	def httpService
	def firmaService
	def filesService

	
	ResponseVS comprobarFechasEvento (Evento evento, Locale locale) {
		log.debug("comprobarFechasEvento")
		if(evento.estado && evento.estado == Evento.Estado.CANCELADO) {
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:evento)
		}
		if(evento.fechaInicio.after(evento.fechaFin)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:messageSource.getMessage(
				'error.fechaInicioAfterFechaFinalMsg', null, locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(evento.fechaFin) &&
			evento.estado != Evento.Estado.FINALIZADO) {
			EventoVotacion.withTransaction {
				evento.estado = Evento.Estado.FINALIZADO
				evento.save()
			}
		} else if(evento.fechaInicio.after(fecha) &&
			evento.estado != Evento.Estado.PENDIENTE_COMIENZO) {
			EventoVotacion.withTransaction {
				evento.estado = Evento.Estado.PENDIENTE_COMIENZO
				evento.save()
			}
		} else if(evento.fechaInicio.before(fecha) &&
			evento.fechaFin.after(fecha) &&
			evento.estado != Evento.Estado.ACTIVO) {
			EventoVotacion.withTransaction {
				evento.estado = Evento.Estado.ACTIVO
				evento.save()
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:evento)
	}
	
   ResponseVS setEventDatesState (Evento evento, Locale locale) {
		Evento.Estado estado
		if(evento.fechaInicio.after(evento.fechaFin)) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:messageSource.getMessage('dateRangeErrorMsg', 
					[evento.fechaInicio, evento.fechaFin].toArray(), locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(evento.fechaFin)) evento.setEstado(Evento.Estado.FINALIZADO)
		if (fecha.after(evento.fechaInicio) && fecha.before(evento.fechaFin))
			evento.setEstado(Evento.Estado.ACTIVO)
		if (fecha.before(evento.fechaInicio)) evento.setEstado(Evento.Estado.PENDIENTE_COMIENZO)
		log.debug("setEventdatesState - estado ${evento.estado.toString()}")
		return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:evento)
	}
	
	boolean isUserAdmin(String nif) {
		if(!administradoresSistema) {
			administradoresSistema = Arrays.asList(
			"${grailsApplication.config.VotingSystem.adminsDNI}".split(","))
		}
		return administradoresSistema.contains(nif)
	}
   
	//{"operation":"EVENT_CANCELLATION","accessControlURL":"...","eventId":"..","estado":"CANCELADO","UUID":"..."}
	private ResponseVS checkCancelEventJSONData(JSONObject cancelDataJSON, Locale locale) {
		int status = ResponseVS.SC_ERROR_REQUEST
		TypeVS typeRespuesta = TypeVS.EVENT_CANCELLATION_ERROR
		String msg
		try {
			TypeVS operationType = TypeVS.valueOf(cancelDataJSON.operation)
			if (cancelDataJSON.accessControlURL && cancelDataJSON.eventId && 
				cancelDataJSON.estado && (TypeVS.EVENT_CANCELLATION == operationType) && 
				((Evento.Estado.CANCELADO == Evento.Estado.valueOf(cancelDataJSON.estado)) ||
					(Evento.Estado.BORRADO_DE_SISTEMA == Evento.Estado.valueOf(cancelDataJSON.estado)))) {
				String requestURL = cancelDataJSON.accessControlURL
				String serverURL = grailsApplication.config.grails.serverURL
				while(requestURL.endsWith("/")) {
					requestURL = requestURL.substring(0, requestURL.length() - 1)
				}
				if(requestURL.equals(serverURL)) {
					status = ResponseVS.SC_OK
				} else {
					msg = messageSource.getMessage(
						'error.urlControlAccesoWrong', [serverURL, requestURL].toArray(), locale)
				}
			} else {
				msg = messageSource.getMessage(
					'evento.datosCancelacionError', null, locale)
			}
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('evento.datosCancelacionError', null, locale)
		}
		if(ResponseVS.SC_OK == status) typeRespuesta = TypeVS.EVENT_CANCELLATION
		else log.error("checkCancelEventJSONData - msg: ${msg} - data:${cancelDataJSON.toString()}")
		return new ResponseVS(statusCode:status, message:msg, type:typeRespuesta)
	}
	
	public ResponseVS cancelEvent(MessageSMIME messageSMIMEReq, Locale locale) {
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		Usuario signer = messageSMIMEReq.usuario
		Evento evento
		String msg
		try {
			log.debug("cancelEvent - message: ${smimeMessageReq.getSignedContent()}")
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			ResponseVS respuesta = checkCancelEventJSONData(messageJSON, locale)
			if(ResponseVS.SC_OK !=  respuesta.statusCode) return respuesta
			Evento.withTransaction {
				evento = Evento.findWhere(id:Long.valueOf(messageJSON.eventId))
			}
			if(!evento) {
				msg = messageSource.getMessage('eventNotFound',
					[messageJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					type:TypeVS.EVENT_CANCELLATION_ERROR, message:msg)
			} else if(evento.estado != Evento.Estado.ACTIVO) {
				msg = messageSource.getMessage('eventNotActiveMsg',
					[messageJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					type:TypeVS.EVENT_CANCELLATION_ERROR, message:msg)
			}
			if(evento.usuario?.nif.equals(signer.nif) || isUserAdmin(signer.nif)){
				log.debug("Usuario con privilegios para cancelar evento")
				switch(evento.estado) {
					case Evento.Estado.CANCELADO:
						 msg = messageSource.getMessage('evento.cancelado',
							 [messageJSON?.eventId].toArray(), locale)
						 break;
					 case Evento.Estado.BORRADO_DE_SISTEMA:
						 msg = messageSource.getMessage('evento.borrado',
							 [messageJSON?.eventId].toArray(), locale)
						 break;
				}
				//local receipt
				SMIMEMessageWrapper smimeMessageResp
				String fromUser = grailsApplication.config.VotingSystem.serverName
				String toUser = null
				String subject = messageSource.getMessage(
					'mime.asunto.cancelEventValidated', null, locale)
				if(evento instanceof EventoVotacion) {
					smimeMessageResp = firmaService.getMultiSignedMimeMessage(
						fromUser, toUser, smimeMessageReq, subject)
					String centroControlUrl = ((EventoVotacion)evento).getCentroControl().serverURL
					while(centroControlUrl.endsWith("/")) {
						centroControlUrl = centroControlUrl.substring(0, centroControlUrl.length() - 1)
					}
					toUser = ((EventoVotacion)evento).getCentroControl()?.nombre
					String cancelCentroCentrolEventURL = centroControlUrl + "/eventoVotacion/cancelled"
					ResponseVS respuestaCentroControl =	httpService.sendMessage(
							smimeMessageResp.getBytes(), org.votingsystem.model.ContentTypeVS.SIGNED, cancelCentroCentrolEventURL);
					log.debug("respuestaCentroControl - status: ${respuestaCentroControl.statusCode}")
					if(ResponseVS.SC_OK == respuestaCentroControl.statusCode ||
						ResponseVS.SC_CANCELLATION_REPEATED == respuestaCentroControl.statusCode) {
						msg = msg + " - " + messageSource.getMessage(
							'centroControl.notificado', [centroControlUrl].toArray(), locale)
					} else {
						msg = msg + " - " + messageSource.getMessage('controCenterCommunicationErrorMsg',
							[centroControlUrl].toArray(), locale)
						log.error("cancelEvent - msg: ${msg}")
						return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
							type:TypeVS.EVENT_CANCELLATION_ERROR, message:msg, eventVS:evento)
					}
				} else {
					toUser = signer.getNif()
					smimeMessageResp = firmaService.getMultiSignedMimeMessage(
						fromUser, toUser, smimeMessageReq, subject)
				}
				log.debug("cancel event - msg:${msg}")
				
				MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
					smimePadre:messageSMIMEReq, evento:evento, valido:true,
					contenido:smimeMessageResp.getBytes())
				MessageSMIME.withTransaction {
					if (!messageSMIMEResp.save()) {
						messageSMIMEResp.errors.each {
							log.error("cancel event - save messageSMIMEResp error - ${it}")}
					}
					
				}
				evento.estado = Evento.Estado.valueOf(messageJSON.estado)
				evento.dateCanceled = new Date(System.currentTimeMillis());
				log.debug("evento validated")
				EventoVotacion.withTransaction {
					if (!evento.save()) {
						evento.errors.each {
							log.error("cancel event error saving evento - ${it}")}
					}
				}
				log.debug("updated evento.id: ${evento.id}")
				return new ResponseVS(statusCode:ResponseVS.SC_OK,message:msg,
					type:TypeVS.EVENT_CANCELLATION, data:messageSMIMEResp,
					eventVS:evento)
			} else {
				msg = messageSource.getMessage('csr.usuarioNoAutorizado', null, locale)
				log.error("cancelEvent - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					type:TypeVS.EVENT_CANCELLATION_ERROR, message:msg, eventVS:evento)
			}	
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('evento.datosCancelacionError', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:msg, eventVS:evento, type:TypeVS.EVENT_CANCELLATION_ERROR)
		}
	}

	public Map optenerEventoMap(Evento eventoItem) {
		if(eventoItem instanceof EventoVotacion) 
			return optenerEventoVotacionMap(eventoItem)
		else if(eventoItem instanceof EventoFirma)
			return optenerEventoFirmaMap(eventoItem)
		else if(eventoItem instanceof EventoReclamacion)
			return optenerEventoReclamacionMap(eventoItem)
	}
	
	
	
	public Map optenerEventoVotacionMap(EventoVotacion eventoItem) {
		//log.debug("eventoItem: ${eventoItem.id} - estado ${eventoItem.estado}")
		def eventoMap = [id: eventoItem.id, fechaCreacion: eventoItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/evento/${eventoItem.id}",
			solicitudPublicacionURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/${eventoItem.id}/firmado",
			solicitudPublicacionValidadaURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/${eventoItem.id}/validado",
			asunto:eventoItem.asunto, contenido:eventoItem.contenido,
			cardinalidad:eventoItem.cardinalidadOpciones?.toString(),
			etiquetas:eventoItem.etiquetaSet?.collect {etiqueta ->
						return [id:etiqueta.id, contenido:etiqueta.nombre]},
			duracion:DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(
				eventoItem.getFechaInicio().getTime() - eventoItem.getFechaFin().getTime()),
			copiaSeguridadDisponible:eventoItem.copiaSeguridadDisponible,
			estado:eventoItem.estado.toString(),
			informacionVotosURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/${eventoItem.id}/informacionVotos",
			fechaInicio:eventoItem.getFechaInicio(),
			fechaFin:eventoItem.getFechaFin()]
		if(eventoItem.usuario) eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
		def controlAccesoMap = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.VotingSystem.serverName]
		eventoMap.controlAcceso = controlAccesoMap
		eventoMap.opciones = eventoItem.opciones?.collect {opcion ->
				return [id:opcion.id, contenido:opcion.contenido]}
		CentroControl centroControl = eventoItem.centroControl
		def centroControlMap = [id:centroControl.id, serverURL:centroControl.serverURL,
			nombre:centroControl.nombre,
			estadisticasEventoURL:"${centroControl.serverURL}/eventoVotacion/estadisticas?eventAccessControlURL=${grailsApplication.config.grails.serverURL}/eventoVotacion/${eventoItem.id}"]
		eventoMap.centroControl = centroControlMap
		eventoMap.certificadoCA_DeEvento = "${grailsApplication.config.grails.serverURL}/certificado/eventCA/${eventoItem.id}"
		return eventoMap
	}
	
	public Map optenerEventoFirmaMap(EventoFirma eventoItem) {
		//log.debug("eventoItem: ${eventoItem.id} - estado ${eventoItem.estado}")
		def eventoMap = [id: eventoItem.id, fechaCreacion: eventoItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/eventoFirma/${eventoItem.id}",
			urlPDF:"${grailsApplication.config.grails.serverURL}/eventoFirma/firmado/${eventoItem.id}",
			asunto:eventoItem.asunto, contenido: eventoItem.contenido,
			etiquetas:eventoItem.etiquetaSet?.collect {etiqueta ->
				return [id:etiqueta.id, contenido:etiqueta.nombre]},
			duracion:DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(
				eventoItem.getFechaInicio().getTime() - eventoItem.getFechaFin().getTime()),
			estado:eventoItem.estado.toString(),
			copiaSeguridadDisponible:eventoItem.copiaSeguridadDisponible,
			fechaInicio:eventoItem.getFechaInicio(),
			fechaFin:eventoItem.getFechaFin()]
		if(eventoItem.usuario) eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
		eventoMap.numeroFirmas = Documento.countByEventoAndEstado(eventoItem,
			Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
		return eventoMap
	}
	
	public Map optenerEventoReclamacionMap(EventoReclamacion eventoItem) {
		//log.debug("eventoItem: ${eventoItem.id} - estado ${eventoItem.estado}")
		def eventoMap = [id: eventoItem.id, fechaCreacion: eventoItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/evento/${eventoItem.id}",
			solicitudPublicacionURL:"${grailsApplication.config.grails.serverURL}/eventoReclamacion/${eventoItem.id}/firmado",
			solicitudPublicacionValidadaURL:"${grailsApplication.config.grails.serverURL}/eventoReclamacion/${eventoItem.id}/validado",
			asunto:eventoItem.asunto, contenido:eventoItem.contenido,
			cardinalidad:eventoItem.cardinalidadRepresentaciones?.toString(),
			etiquetas:eventoItem.etiquetaSet?.collect {etiqueta ->
						return [id:etiqueta.id, contenido:etiqueta.nombre]},
			copiaSeguridadDisponible:eventoItem.copiaSeguridadDisponible,
			duracion:DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(
				eventoItem.getFechaInicio().getTime() - eventoItem.getFechaFin().getTime()),
			estado:eventoItem.estado.toString(),
			fechaInicio:eventoItem.getFechaInicio(),
			fechaFin:eventoItem.getFechaFin()]
		if(eventoItem.usuario) eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
		Firma.withTransaction {
			eventoMap.numeroFirmas = Firma.countByEventoAndType(eventoItem, TypeVS.CLAIM_EVENT_SIGN)
		}
		def controlAccesoMap = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.VotingSystem.serverName]
		eventoMap.controlAcceso = controlAccesoMap
		eventoMap.campos = eventoItem.camposEvento?.collect {campoItem ->
				return [id:campoItem.id, contenido:campoItem.contenido]}
		return eventoMap
	}
	
	public Map getMetaInfMap(Evento event) {
		Map eventMap = [:];
		eventMap.put("id", event.id);
		eventMap.put("serverURL", "${grailsApplication.config.grails.serverURL}")
		eventMap.put("subject", event.asunto)
		eventMap.put("dateInit", DateUtils.getStringFromDate(event.getFechaInicio()))
		eventMap.put("dateFinish", DateUtils.getStringFromDate(event.getDateFinish()))
		if(event instanceof EventoVotacion) {
			eventMap.put("type", TypeVS.VOTING_EVENT.toString())
		} else if(event instanceof EventoReclamacion) {
			eventMap.put("type", TypeVS.CLAIM_EVENT.toString());
		} else if(event instanceof EventoFirma) {
			eventMap.put("type", TypeVS.SIGN_EVENT.toString());
		}
		log.debug("getMetaInfMap - Event type: ${eventMap.type?.toString()}")
		return eventMap
	}

}