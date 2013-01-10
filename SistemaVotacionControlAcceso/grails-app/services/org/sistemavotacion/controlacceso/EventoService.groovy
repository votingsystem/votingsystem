package org.sistemavotacion.controlacceso

import java.util.Date;
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.*;
import grails.converters.JSON
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class EventoService {	
		
    static transactional = true
	
	List<String> administradoresSistema
	def messageSource
	def subscripcionService
	def grailsApplication


	
	Respuesta comprobarFechasEvento (Evento evento, Locale locale) {
		log.debug("comprobarFechasEvento")
		if(evento.fechaInicio.after(evento.fechaFin)) {
			return new Respuesta(codigoEstado:400, mensaje:messageSource.getMessage(
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
		return new Respuesta(codigoEstado:200, evento:evento)
	}
    
   Evento.Estado obtenerEstadoEvento (Evento evento) {
        Evento.Estado estado
        Date fecha = DateUtils.getTodayDate()
        if (fecha.after(evento.fechaFin)) estado = Evento.Estado.FINALIZADO
        if (fecha.after(evento.fechaInicio) && fecha.before(evento.fechaFin))   
            estado = Evento.Estado.ACTIVO
        if (fecha.before(evento.fechaInicio))  estado = Evento.Estado.PENDIENTE_COMIENZO
        log.debug("obtenerEstadoEvento - estado ${estado.toString()}")
        return estado
    }
	
	boolean isUserAdmin(String nif) {
		if(!administradoresSistema) {
			administradoresSistema = Arrays.asList(
			"${grailsApplication.config.SistemaVotacion.adminsDNI}".split(","))
		}
		return administradoresSistema.contains(nif)
	}
   
	public Respuesta cancelarEvento(SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug("cancelarEvento - mensaje: ${smimeMessage.getSignedContent()}")
		def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
		Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
		Usuario usuario = respuestaUsuario.usuario
		MensajeSMIME mensajeSMIME
		Evento evento
		Respuesta respuesta
		if (mensajeJSON.eventoId &&
				mensajeJSON.estado && ((Evento.Estado.CANCELADO ==
					Evento.Estado.valueOf(mensajeJSON.estado)) ||
					(Evento.Estado.BORRADO_DE_SISTEMA ==
					Evento.Estado.valueOf(mensajeJSON.estado)))) {
				Evento.withTransaction {
					evento = Evento.findWhere(id:mensajeJSON.eventoId?.longValue(), 
						estado:Evento.Estado.ACTIVO)
					if (evento) {
						 if(evento.usuario?.nif.equals(smimeMessage.firmante?.nif) ||
							isUserAdmin(smimeMessage.firmante?.nif)){
								log.debug("Usuario con privilegios para cancelar evento")
								evento.estado = Evento.Estado.valueOf(mensajeJSON.estado)
								evento.save()
								mensajeSMIME = new MensajeSMIME(usuario:usuario,
									tipo:Tipo.getTipoEnFuncionEstado(evento.estado),
									valido:smimeMessage.isValidSignature(),
									contenido:smimeMessage.getBytes(), evento:evento)
								mensajeSMIME.save();
								String mensajeRespuesta;
								switch(evento.estado) {
									case Evento.Estado.CANCELADO:
									 	mensajeRespuesta = messageSource.getMessage('evento.cancelado', 
											 [mensajeJSON?.eventoId].toArray(), locale)
									 	break;
									 case Evento.Estado.BORRADO_DE_SISTEMA:
										 mensajeRespuesta = messageSource.getMessage('evento.borrado',
											 [mensajeJSON?.eventoId].toArray(), locale)
										 break;
									 
								 }
								 respuesta = new Respuesta(codigoEstado:200, mensaje: mensajeRespuesta)
								 return
						 } else {
							 respuesta = new Respuesta(codigoEstado:400, mensaje: messageSource.getMessage(
								 'csr.usuarioNoAutorizado', null, locale))
						 }
					}
					mensajeSMIME = new MensajeSMIME(tipo:Tipo.EVENTO_CANCELADO_ERROR,
						usuario:usuario, valido:smimeMessage.isValidSignature(),
						contenido:smimeMessage.getBytes(), evento:evento)
					mensajeSMIME.save();
				}
		}
		if(!respuesta) {
			respuesta = new Respuesta(codigoEstado:400, mensaje:messageSource.getMessage(
				'evento.datosCancelacionError', [mensajeJSON?.eventoId,	mensajeJSON.estado].toArray(), locale))
		}
		return respuesta
	}

	public Map optenerEventoVotacionJSONMap(EventoVotacion eventoItem) {
		def eventoMap = [id: eventoItem.id, fechaCreacion: eventoItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/evento/obtener?id=${eventoItem.id}",
			solicitudPublicacionURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/firmado?id=${eventoItem.id}",
			solicitudPublicacionValidadaURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/validado?id=${eventoItem.id}",
			asunto:eventoItem.asunto, contenido:eventoItem.contenido,
			etiquetas:eventoItem.etiquetaSet?.collect {etiqueta ->
						return [id:etiqueta.id, contenido:etiqueta.nombre]},
			duracion:DateUtils.getElapsedTime(eventoItem.getFechaInicio(),
			eventoItem.getFechaFin()),
			estado:eventoItem.estado.toString(),
			informacionVotosURL:"${grailsApplication.config.grails.serverURL}/evento/informacionVotos?id=${eventoItem.id}",
			fechaInicio:eventoItem.getFechaInicio(),
			fechaFin:eventoItem.getFechaFin()]
		if(eventoItem.usuario) eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
		def controlAccesoMap = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.SistemaVotacion.NombreControlAcceso]
		eventoMap.controlAcceso = controlAccesoMap
		eventoMap.opciones = eventoItem.opciones?.collect {opcion ->
				return [id:opcion.id, contenido:opcion.contenido]}
		CentroControl centroControl = eventoItem.centroControl
		def centroControlMap = [id:centroControl.id, serverURL:centroControl.serverURL,
			nombre:centroControl.nombre,
			estadisticasEventoURL:"${centroControl.serverURL}/eventoVotacion/estadisticas?eventoVotacionId=${eventoItem.id}&controlAccesoServerURL=${grailsApplication.config.grails.serverURL}"]
		eventoMap.centroControl = centroControlMap
		eventoMap.certificadoCA_DeEvento = "${grailsApplication.config.grails.serverURL}/certificado/certificadoCA_DeEvento?idEvento=${eventoItem.id}"
		return eventoMap
	}
	
	public Map optenerEventoFirmaJSONMap(EventoFirma eventoItem) {
		def eventoMap = [id: eventoItem.id, fechaCreacion: eventoItem.dateCreated,
			urlPDF:"${grailsApplication.config.grails.serverURL}/documento/obtenerManifiesto?id=${eventoItem.id}",
			asunto:eventoItem.asunto, contenido: eventoItem.contenido,
			etiquetas:eventoItem.etiquetaSet?.collect {etiqueta ->
				return [id:etiqueta.id, contenido:etiqueta.nombre]},
			duracion:DateUtils.getElapsedTime(eventoItem.getFechaInicio(),
			eventoItem.getFechaFin()),
			estado:eventoItem.estado.toString(),
			fechaInicio:eventoItem.getFechaInicio(),
			fechaFin:eventoItem.getFechaFin()]
		if(eventoItem.usuario) eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
		eventoMap.numeroFirmas = Documento.countByEventoAndEstado(eventoItem,
			Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
		return eventoMap
	}
	
	public Map optenerEventoReclamacionJSONMap(EventoReclamacion eventoItem) {
		def eventoMap = [id: eventoItem.id, fechaCreacion: eventoItem.dateCreated,
			URL:"${grailsApplication.config.grails.serverURL}/evento/obtener?id=${eventoItem.id}",
			solicitudPublicacionURL:"${grailsApplication.config.grails.serverURL}/eventoReclamacion/firmado?id=${eventoItem.id}",
			solicitudPublicacionValidadaURL:"${grailsApplication.config.grails.serverURL}/eventoReclamacion/validado?id=${eventoItem.id}",
			asunto:eventoItem.asunto, contenido:eventoItem.contenido,
			etiquetas:eventoItem.etiquetaSet?.collect {etiqueta ->
						return [id:etiqueta.id, contenido:etiqueta.nombre]},
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
				nombre:grailsApplication.config.SistemaVotacion.NombreControlAcceso]
		eventoMap.controlAcceso = controlAccesoMap
		eventoMap.campos = eventoItem.camposEvento?.collect {campoItem ->
				return [id:campoItem.id, contenido:campoItem.contenido]}
		return eventoMap
	}
	
}