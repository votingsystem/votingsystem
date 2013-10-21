package org.sistemavotacion.controlacceso

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.sistemavotacion.controlacceso.modelo.Evento;
import org.sistemavotacion.controlacceso.modelo.EventoFirma;
import org.sistemavotacion.controlacceso.modelo.EventoReclamacion;
import org.sistemavotacion.controlacceso.modelo.EventoVotacion;
import org.sistemavotacion.controlacceso.modelo.OpcionDeEvento;
import org.sistemavotacion.controlacceso.modelo.Respuesta;
import org.sistemavotacion.controlacceso.modelo.Tipo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.*;
import org.sistemavotacion.utils.VotingSystemApplicationContex;

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

	
	Respuesta comprobarFechasEvento (Evento evento, Locale locale) {
		log.debug("comprobarFechasEvento")
		if(evento.estado && evento.estado == Evento.Estado.CANCELADO) {
			return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento)
		}
		if(evento.fechaInicio.after(evento.fechaFin)) {
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:messageSource.getMessage(
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
		return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento)
	}
	
   Respuesta setEventDatesState (Evento evento, Locale locale) {
		Evento.Estado estado
		if(evento.fechaInicio.after(evento.fechaFin)) {
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:messageSource.getMessage('dateRangeErrorMsg', 
					[evento.fechaInicio, evento.fechaFin].toArray(), locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(evento.fechaFin)) evento.setEstado(Evento.Estado.FINALIZADO)
		if (fecha.after(evento.fechaInicio) && fecha.before(evento.fechaFin))
			evento.setEstado(Evento.Estado.ACTIVO)
		if (fecha.before(evento.fechaInicio)) evento.setEstado(Evento.Estado.PENDIENTE_COMIENZO)
		log.debug("setEventdatesState - estado ${evento.estado.toString()}")
		return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento)
	}
	
	boolean isUserAdmin(String nif) {
		if(!administradoresSistema) {
			administradoresSistema = Arrays.asList(
			"${grailsApplication.config.SistemaVotacion.adminsDNI}".split(","))
		}
		return administradoresSistema.contains(nif)
	}
   
	//{"operation":"CANCELAR_EVENTO","accessControlURL":"...","eventId":"..","estado":"CANCELADO","UUID":"..."}
	private Respuesta checkCancelEventJSONData(JSONObject cancelDataJSON, Locale locale) {
		int status = Respuesta.SC_ERROR_PETICION
		Tipo tipoRespuesta = Tipo.CANCELAR_EVENTO_ERROR
		String msg
		try {
			Tipo operationType = Tipo.valueOf(cancelDataJSON.operation)
			if (cancelDataJSON.accessControlURL && cancelDataJSON.eventId && 
				cancelDataJSON.estado && (Tipo.CANCELAR_EVENTO == operationType) && 
				((Evento.Estado.CANCELADO == Evento.Estado.valueOf(cancelDataJSON.estado)) ||
					(Evento.Estado.BORRADO_DE_SISTEMA == Evento.Estado.valueOf(cancelDataJSON.estado)))) {
				String requestURL = cancelDataJSON.accessControlURL
				String serverURL = grailsApplication.config.grails.serverURL
				while(requestURL.endsWith("/")) {
					requestURL = requestURL.substring(0, requestURL.length() - 1)
				}
				if(requestURL.equals(serverURL)) {
					status = Respuesta.SC_OK
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
		if(Respuesta.SC_OK == status) tipoRespuesta = Tipo.CANCELAR_EVENTO
		else log.error("checkCancelEventJSONData - msg: ${msg} - data:${cancelDataJSON.toString()}")
		return new Respuesta(codigoEstado:status, mensaje:msg, tipo:tipoRespuesta)
	}
	
	public Respuesta cancelEvent(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()
		Usuario signer = mensajeSMIMEReq.usuario
		Evento evento
		String msg
		try {
			log.debug("cancelEvent - mensaje: ${smimeMessageReq.getSignedContent()}")
			def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
			Respuesta respuesta = checkCancelEventJSONData(mensajeJSON, locale)
			if(Respuesta.SC_OK !=  respuesta.codigoEstado) return respuesta
			Evento.withTransaction {
				evento = Evento.findWhere(id:Long.valueOf(mensajeJSON.eventId))
			}
			if(!evento) {
				msg = messageSource.getMessage('eventNotFound',
					[mensajeJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.CANCELAR_EVENTO_ERROR, mensaje:msg)
			} else if(evento.estado != Evento.Estado.ACTIVO) {
				msg = messageSource.getMessage('eventNotActiveMsg',
					[mensajeJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.CANCELAR_EVENTO_ERROR, mensaje:msg)
			}
			if(evento.usuario?.nif.equals(signer.nif) || isUserAdmin(signer.nif)){
				log.debug("Usuario con privilegios para cancelar evento")
				switch(evento.estado) {
					case Evento.Estado.CANCELADO:
						 msg = messageSource.getMessage('evento.cancelado',
							 [mensajeJSON?.eventId].toArray(), locale)
						 break;
					 case Evento.Estado.BORRADO_DE_SISTEMA:
						 msg = messageSource.getMessage('evento.borrado',
							 [mensajeJSON?.eventId].toArray(), locale)
						 break;
				}
				//local receipt
				SMIMEMessageWrapper smimeMessageResp
				String fromUser = grailsApplication.config.SistemaVotacion.serverName
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
					String contentType = "${grailsApplication.config.pkcs7SignedContentType}"
					Respuesta respuestaCentroControl =	httpService.sendMessage(
							smimeMessageResp.getBytes(), contentType, cancelCentroCentrolEventURL);
					log.debug("respuestaCentroControl - status: ${respuestaCentroControl.codigoEstado}")
					if(Respuesta.SC_OK == respuestaCentroControl.codigoEstado ||
						Respuesta.SC_ANULACION_REPETIDA == respuestaCentroControl.codigoEstado) {
						msg = msg + " - " + messageSource.getMessage(
							'centroControl.notificado', [centroControlUrl].toArray(), locale)
					} else {
						msg = msg + " - " + messageSource.getMessage('controCenterCommunicationErrorMsg',
							[centroControlUrl].toArray(), locale)
						log.error("cancelEvent - msg: ${msg}")
						return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
							tipo:Tipo.CANCELAR_EVENTO_ERROR, mensaje:msg, evento:evento)
					}
				} else {
					toUser = signer.getNif()
					smimeMessageResp = firmaService.getMultiSignedMimeMessage(
						fromUser, toUser, smimeMessageReq, subject)
				}
				log.debug("cancel event - msg:${msg}")
				
				MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(tipo:Tipo.RECIBO,
					smimePadre:mensajeSMIMEReq, evento:evento, valido:true,
					contenido:smimeMessageResp.getBytes())
				MensajeSMIME.withTransaction {
					if (!mensajeSMIMEResp.save()) {
						mensajeSMIMEResp.errors.each {
							log.error("cancel event - save mensajeSMIMEResp error - ${it}")}
					}
					
				}
				evento.estado = Evento.Estado.valueOf(mensajeJSON.estado)
				evento.dateCanceled = new Date(System.currentTimeMillis());
				log.debug("evento validated")
				EventoVotacion.withTransaction {
					if (!evento.save()) {
						evento.errors.each {
							log.error("cancel event error saving evento - ${it}")}
					}
				}
				log.debug("updated evento.id: ${evento.id}")
				return new Respuesta(codigoEstado:Respuesta.SC_OK,mensaje:msg,
					tipo:Tipo.CANCELAR_EVENTO, mensajeSMIME:mensajeSMIMEResp,
					evento:evento)
			} else {
				msg = messageSource.getMessage('csr.usuarioNoAutorizado', null, locale)
				log.error("cancelEvent - msg: ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.CANCELAR_EVENTO_ERROR, mensaje:msg, evento:evento)
			}	
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('evento.datosCancelacionError', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:msg, evento:evento, tipo:Tipo.CANCELAR_EVENTO_ERROR)
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
			duracion:DateUtils.getElapsedTime(eventoItem.getFechaInicio(),
				eventoItem.getFechaFin()),
			copiaSeguridadDisponible:eventoItem.copiaSeguridadDisponible,
			estado:eventoItem.estado.toString(),
			informacionVotosURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/${eventoItem.id}/informacionVotos",
			fechaInicio:eventoItem.getFechaInicio(),
			fechaFin:eventoItem.getFechaFin()]
		if(eventoItem.usuario) eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
		def controlAccesoMap = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.SistemaVotacion.serverName]
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
			URL:"${grailsApplication.config.grails.serverURL}/evento/${eventoItem.id}",
			urlPDF:"${grailsApplication.config.grails.serverURL}/eventoFirma/firmado/${eventoItem.id}",
			asunto:eventoItem.asunto, contenido: eventoItem.contenido,
			etiquetas:eventoItem.etiquetaSet?.collect {etiqueta ->
				return [id:etiqueta.id, contenido:etiqueta.nombre]},
			duracion:DateUtils.getElapsedTime(eventoItem.getFechaInicio(),
			eventoItem.getFechaFin()),
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
			duracion:DateUtils.getElapsedTime(eventoItem.getFechaInicio(),
			eventoItem.getFechaFin()),
			estado:eventoItem.estado.toString(),
			fechaInicio:eventoItem.getFechaInicio(),
			fechaFin:eventoItem.getFechaFin()]
		if(eventoItem.usuario) eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
		Firma.withTransaction {
			eventoMap.numeroFirmas = Firma.countByEventoAndTipo(eventoItem, Tipo.FIRMA_EVENTO_RECLAMACION)
		}
		def controlAccesoMap = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.SistemaVotacion.serverName]
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
			eventMap.put("type", Tipo.EVENTO_VOTACION.toString())
		} else if(event instanceof EventoReclamacion) {
			eventMap.put("type", Tipo.EVENTO_RECLAMACION.toString());
		} else if(event instanceof EventoFirma) {
			eventMap.put("type", Tipo.EVENTO_FIRMA.toString());
		}
		log.debug("getMetaInfMap - Event type: ${eventMap.type?.toString()}")
		return eventMap
	}

}