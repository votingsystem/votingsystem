package org.sistemavotacion.controlacceso

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import org.sistemavotacion.controlacceso.modelo.*
import grails.converters.JSON
import org.sistemavotacion.smime.SMIMEMessageWrapper
import org.sistemavotacion.util.*
/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class EventoVotacionController {

    def eventoVotacionService
    def eventoService
	def almacenClavesService
	def notificacionService
	def httpService
    
	def index() { }
	
    def guardarAdjuntandoValidacion = {
        flash.respuesta = eventoVotacionService.guardarEvento(
			params.smimeMessageReq, request.getLocale())
		if (200 == flash.respuesta.codigoEstado) {
			Respuesta respuestaNotificacion = httpService.notificarInicializacionDeEvento(
				flash.respuesta.evento.centroControl, flash.respuesta.mensajeSMIMEValidado.contenido)
			if(200 != respuestaNotificacion.codigoEstado) {
				log.debug("Problemas notificando evento '${flash.respuesta.evento.id}' al Centro de Control - codigo estado:${respuestaNotificacion.codigoEstado} - mensaje: ${respuestaNotificacion.mensaje}")	
				Evento evento = flash.respuesta.evento;
				evento.estado = Evento.Estado.ACTORES_PENDIENTES_NOTIFICACION
				Evento.withTransaction { evento.save() }
				flash.respuesta = new Respuesta(codigoEstado:400, 
					mensaje:message(code: 'http.errorConectandoCentroControl'))
			} 
		}
		return false 
    }
    
    def obtener = {
        def eventoList = []
        def eventosMap = new HashMap()
        eventosMap.eventos = new HashMap()
        eventosMap.eventos.votaciones = []
        if (params.ids?.size() > 0) {
				EventoVotacion.withTransaction {
					EventoVotacion.getAll(params.ids).collect {evento ->
						if (evento) eventoList << evento;
				}
            }
            if (eventoList.size() == 0) {
                    response.status = 404 //Not Found
                    render message(code: 'evento.eventoNotFound', args:[params.ids])
                    return
            }
        } else {
			params.sort = "fechaInicio"
			log.debug " -Params: " + params
			Evento.Estado estadoEvento
			if(params.estadoEvento) estadoEvento = Evento.Estado.valueOf(params.estadoEvento)
			EventoVotacion.withTransaction {
				if(estadoEvento) {
					if(estadoEvento == Evento.Estado.FINALIZADO) {
						eventoList =  EventoVotacion.findAllByEstadoOrEstado(
							Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, params)
						eventosMap.numeroTotalEventosVotacionEnSistema = EventoVotacion.countByEstadoOrEstado(
							Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO)
					} else {
						eventoList =  EventoVotacion.findAllByEstado(estadoEvento, params)
						eventosMap.numeroTotalEventosVotacionEnSistema = EventoVotacion.countByEstado(estadoEvento)
					}
				} else {
					eventoList =  EventoVotacion.findAllByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
						   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO, params)
					eventosMap.numeroTotalEventosVotacionEnSistema = EventoVotacion.countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
						Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO)
				}
			}
            eventosMap.offset = params.long('offset')
        }
        eventosMap.numeroEventosVotacionEnPeticion = eventoList.size()
        eventoList.collect {eventoItem ->
				eventosMap.eventos.votaciones.add(eventoService.optenerEventoVotacionJSONMap(eventoItem))
        }
        response.setContentType("application/json")
        render eventosMap as JSON
    }

    def estadisticas = {
        if (params.long('id')) {
            EventoVotacion eventoVotacion
            if (!params.evento) {
				EventoVotacion.withTransaction {
					eventoVotacion = EventoVotacion.get(params.id)
				}
			} 
            else eventoVotacion = params.evento
            if (eventoVotacion) {
                response.status = 200
                def estadisticasMap = new HashMap()
				estadisticasMap.opciones = []
                estadisticasMap.id = eventoVotacion.id
				estadisticasMap.numeroSolicitudesDeAcceso = SolicitudAcceso.countByEventoVotacion(eventoVotacion)
				estadisticasMap.numeroSolicitudesDeAccesoOK = SolicitudAcceso.countByEventoVotacionAndEstado(
						eventoVotacion, SolicitudAcceso.Estado.OK)
				estadisticasMap.numeroSolicitudesDeAccesoANULADAS =   SolicitudAcceso.countByEventoVotacionAndEstado(
						eventoVotacion, SolicitudAcceso.Estado.ANULADO)
				estadisticasMap.numeroVotos = Voto.countByEventoVotacion(eventoVotacion)
				estadisticasMap.numeroVotosOK = Voto.countByEventoVotacionAndEstado(
						eventoVotacion, Voto.Estado.OK)
				estadisticasMap.numeroVotosANULADOS = Voto.countByEventoVotacionAndEstado(
					eventoVotacion, Voto.Estado.ANULADO)								
    			eventoVotacion.opciones.collect { opcion ->
					def numeroVotos = Voto.countByOpcionDeEventoAndEstado(
						opcion, Voto.Estado.OK)
					def opcionMap = [id:opcion.id, contenido:opcion.contenido,
						numeroVotos:numeroVotos]
					estadisticasMap.opciones.add(opcionMap)
				}
				if (params.callback) render "${params.callback}(${estadisticasMap as JSON})"
	            else render estadisticasMap as JSON
                return false
            }
            response.status = 404
            render message(code: 'evento.eventoNotFound', args:[params.id])
            return false
        }
        response.status = 400
        render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
    
    def validado = {
        if (params.long('id')) {
			def evento
			Evento.withTransaction {
				evento = Evento.get(params.id)
			}
            MensajeSMIME mensajeSMIME
            if (evento) {
				MensajeSMIME.withTransaction {
					mensajeSMIME = MensajeSMIME.findWhere(evento:evento, tipo: Tipo.EVENTO_VOTACION_VALIDADO)
				}
                if (mensajeSMIME) {
                        response.status = 200
                        response.contentLength = mensajeSMIME.contenido.length
                        //response.setContentType("text/plain")
                        response.outputStream <<  mensajeSMIME.contenido
                        response.outputStream.flush()
                        return false
                }
            }
            if (!evento || !mensajeSMIME) {
                response.status = 404
                render message(code: 'evento.eventoNotFound', args:[params.ids])
                return false
            }
        }
        response.status = 400
        render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
	
	def firmado = {
		if (params.long('id')) {
			def evento
			Evento.withTransaction {
				evento = Evento.get(params.id)
			}
			if (evento) {
				MensajeSMIME mensajeSMIME
				MensajeSMIME.withTransaction {
					mensajeSMIME = MensajeSMIME.findWhere(evento:evento, tipo: Tipo.EVENTO_VOTACION)
				}
				if (mensajeSMIME) {
					response.status = 200
					response.contentLength = mensajeSMIME.contenido.length
					response.setContentType("text/plain")
					response.outputStream <<  mensajeSMIME.contenido
					response.outputStream.flush()
					return false
				}
			}
			response.status = 404
			render message(code: 'evento.eventoNotFound', args:[params.ids])
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def guardarSolicitudCopiaRespaldo = {
		EventoVotacion eventoVotacion
		if (!params.evento) {
			SMIMEMessageWrapper smimeMessage= params.smimeMessageReq
			def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			if (params.long('eventoId')) {
				eventoVotacion = EventoVotacion.get(mensajeJSON.eventoId)
				if (!eventoVotacion) {
					response.status = 404
					render message(code: 'evento.eventoNotFound', args:[mensajeJSON.eventoId])
					return false
				}
			} else {
				response.status = 400
				render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
				return false
			}
		} else eventoVotacion = params.evento
		File copiaRespaldo = eventoVotacionService.generarCopiaRespaldo(
			eventoVotacion, request.getLocale())
		if (copiaRespaldo != null) {
			def bytesCopiaRespaldo = FileUtils.getBytesFromFile(copiaRespaldo)
			response.contentLength = bytesCopiaRespaldo.length
			response.setHeader("Content-disposition", "filename=${copiaRespaldo.getName()}")
			response.setHeader("NombreArchivo", "${copiaRespaldo.getName()}")
			response.setContentType("application/octet-stream")
			response.outputStream << bytesCopiaRespaldo
			response.outputStream.flush()
			return false
		} else {
			flash.respuesta = new Respuesta(tipo: Tipo.ERROR_DE_SISTEMA,
				mensaje:message(code: 'error.SinCopiaRespaldo'),codigoEstado:500)
			forward controller: "error500", action: "procesar"
		}
	}

}