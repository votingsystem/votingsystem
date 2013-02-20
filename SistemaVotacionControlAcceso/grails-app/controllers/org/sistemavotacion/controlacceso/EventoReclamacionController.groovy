package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*

import grails.converters.JSON
import org.sistemavotacion.util.*
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class EventoReclamacionController {

    def eventoReclamacionService
    def eventoService

	def index() { }
	
    def obtener = {
        def eventoList = []
        def eventosMap = new HashMap()
        eventosMap.eventos = new HashMap()
        eventosMap.eventos.reclamaciones = []
        if (params.ids?.size() > 0) {
             EventoReclamacion.getAll(params.ids).collect {evento ->
                    if (evento) eventoList << evento;
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
			if(estadoEvento) {
				if(estadoEvento == Evento.Estado.FINALIZADO) {
					eventoList =  EventoReclamacion.findAllByEstadoOrEstado(
						Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, params)
					eventosMap.numeroTotalEventosReclamacionEnSistema = EventoReclamacion.countByEstadoOrEstado(
						Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO)
				} else {
					eventoList =  EventoReclamacion.findAllByEstado(estadoEvento, params)
					eventosMap.numeroTotalEventosReclamacionEnSistema = EventoReclamacion.countByEstado(estadoEvento)
				}
			} else {
				eventoList =  EventoReclamacion.findAllByEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
					   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, params)
				eventosMap.numeroTotalEventosReclamacionEnSistema = EventoReclamacion.countByEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
					Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO)
			}
            eventosMap.offset = params.long('offset')
        }
		eventosMap.numeroEventosReclamacionEnPeticion = eventoList.size()
        eventoList.collect {eventoItem ->
                eventosMap.eventos.reclamaciones.add(eventoService.optenerEventoReclamacionJSONMap(eventoItem))
        }
        response.setContentType("application/json")
        render eventosMap as JSON
    }
    
    def validado = {
        if (params.int('id')) {
            def evento = Evento.get(params.id)
            MensajeSMIME mensajeSMIME
            if (evento) {
				MensajeSMIME.withTransaction {
					mensajeSMIME = MensajeSMIME.findWhere(evento:evento, 
						tipo: Tipo.EVENTO_RECLAMACION_VALIDADO)
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
        render message(code: 'error.PeticionIncorrectaHTML', 
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
    
	def firmado = {
		if (params.int('id')) {
			def evento = Evento.get(params.id)
			if (evento) {
				MensajeSMIME mensajeSMIME
				MensajeSMIME.withTransaction {
					mensajeSMIME = MensajeSMIME.findWhere(evento:evento, tipo:
						Tipo.EVENTO_RECLAMACION)
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
		render message(code: 'error.PeticionIncorrectaHTML', 
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
    
    def guardarAdjuntandoValidacion = {
        try {
            flash.respuesta = eventoReclamacionService.guardarEvento(
				params.smimeMessageReq, request.getLocale())
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
            flash.respuesta = new Respuesta(mensaje:ex.getMessage(),
                codigoEstado:500, tipo: Tipo.ERROR_DE_SISTEMA)
			forward controller: "error500", action: "procesar"       
        }
    }
	
    def estadisticas = {
        if (params.int('id')) {
            EventoReclamacion eventoReclamacion
            if (!params.evento) eventoReclamacion = EventoReclamacion.get(params.id)
            else eventoReclamacion = params.evento
            if (eventoReclamacion) {
                response.status = 200
                def estadisticasMap = new HashMap()
                estadisticasMap.id = eventoReclamacion.id
                estadisticasMap.tipo = Tipo.EVENTO_RECLAMACION.toString()
                estadisticasMap.numeroFirmas = eventoReclamacion.firmas.size()
                estadisticasMap.estado =  eventoReclamacion.estado.toString()
                estadisticasMap.usuario = eventoReclamacion.usuario.getNif()
                estadisticasMap.fechaInicio = eventoReclamacion.getFechaInicio()
                estadisticasMap.fechaFin = eventoReclamacion.getFechaFin()
                estadisticasMap.solicitudPublicacionURL = "${grailsApplication.config.grails.serverURL}/eventoReclamacion/firmado?id=${eventoReclamacion.id}"
                estadisticasMap.solicitudPublicacionValidadaURL = "${grailsApplication.config.grails.serverURL}/eventoVotacion/validado?id=${eventoReclamacion.id}"
				estadisticasMap.informacionFirmasReclamacionURL = "${grailsApplication.config.grails.serverURL}/evento/informacionFirmasReclamacion?id=${eventoReclamacion.id}"
				estadisticasMap.URL = "${grailsApplication.config.grails.serverURL}/evento/obtener?id=${eventoReclamacion.id}"
				render estadisticasMap as JSON
                return false
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
        EventoReclamacion eventoReclamacion
        if (!params.evento) {
            def mensajeJSON = JSON.parse(params.smimeMessageReq.getSignedContent())
            if (params.int('eventoId')) {
                eventoReclamacion = EventoReclamacion.get(mensajeJSON.eventoId)
                if (!eventoReclamacion) {
                    response.status = 404
                    render message(code: 'evento.eventoNotFound', args:[mensajeJSON.eventoId])
                    return false
                }  
            } else {
                response.status = 400
                render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
                return false
            }
        } else eventoReclamacion = params.evento
        File copiaRespaldo = eventoReclamacionService.generarCopiaRespaldo(
			eventoReclamacion, request.getLocale())
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
            Respuesta respuesta = new Respuesta(tipo: Tipo.OK,
                mensaje:message(code: 'error.SinCopiaRespaldo'),codigoEstado:200)
            response.setContentType("application/json")
            render respuesta.getMap() as JSON
            return false
        }
    }
    
}
