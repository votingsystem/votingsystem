package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*

import grails.converters.JSON
import org.sistemavotacion.util.*
/**
 * @infoController Reclamaciones
 * @descController Servicios relacionados con la publicación de reclamaciones.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class EventoReclamacionController {

    def eventoReclamacionService
    def eventoService
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/eventoReclamacion'.
	 */
	def index() { 
		redirect action: "restDoc"
	}
	
	/**
	 * @httpMethod GET
	 * @param id Opcional. El identificador de la reclamación en la base de datos. Si no se pasa ningún 
	 *        id la consulta se hará entre todos las reclamaciones.
	 * @param max Opcional (por defecto 20). Número máximo de reclamaciones que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param order Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @return Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.
	 */
    def obtener () {
        def eventoList = []
        def eventosMap = new HashMap()
        eventosMap.eventos = new HashMap()
        eventosMap.eventos.reclamaciones = []
        if (params.ids?.size() > 0) {
             EventoReclamacion.getAll(params.ids).collect {evento ->
                    if (evento) eventoList << evento;
            }
            if (eventoList.size() == 0) {
                    response.status = Respuesta.SC_NOT_FOUND
                    render message(code: 'eventNotFound', args:[params.ids])
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
					eventosMap.numeroTotalEventosReclamacionEnSistema = 
							EventoReclamacion.countByEstadoOrEstado(
							Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO)
				} else {
					eventoList =  EventoReclamacion.findAllByEstado(estadoEvento, params)
					eventosMap.numeroTotalEventosReclamacionEnSistema = 
						EventoReclamacion.countByEstado(estadoEvento)
				}
			} else {
				eventoList =  EventoReclamacion.findAllByEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
					   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, params)
				eventosMap.numeroTotalEventosReclamacionEnSistema = 
						EventoReclamacion.countByEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
						Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO)
			}
            eventosMap.offset = params.long('offset')
        }
		eventosMap.numeroEventosReclamacionEnPeticion = eventoList.size()
        eventoList.collect {eventoItem ->
                eventosMap.eventos.reclamaciones.add(
				eventoService.optenerEventoReclamacionJSONMap(eventoItem))
        }
        response.setContentType("application/json")
        render eventosMap as JSON
    }
    
	/**
	 * Servicio que proporciona una copia de la reclamación publicada con la firma
	 * añadida del servidor.
	 * 
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador de la reclamación en la base de datos.
	 * @return Archivo SMIME de la publicación de la reclamación firmada por el usuario que
	 *         la publicó y el servidor.
	 */
    def validado () {
        if (params.int('id')) {
            def evento = Evento.get(params.id)
            MensajeSMIME mensajeSMIME
            if (evento) {
				MensajeSMIME.withTransaction {
					mensajeSMIME = MensajeSMIME.findWhere(evento:evento, 
						tipo: Tipo.EVENTO_RECLAMACION_VALIDADO)
				}
                if (mensajeSMIME) {
                        response.status = Respuesta.SC_OK
                        response.contentLength = mensajeSMIME.contenido.length
                        //response.setContentType("text/plain")
                        response.outputStream <<  mensajeSMIME.contenido
                        response.outputStream.flush()
                        return false
                }
            }
            if (!evento || !mensajeSMIME) {
                response.status = Respuesta.SC_NOT_FOUND
                render message(code: 'eventNotFound', args:[params.ids])
                return false
            }
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', 
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
    
	/**
	 * Servicio que proporciona una copia de la reclamación publicada.
	 *
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador de la reclamación en la base de datos.
	 * @return Archivo SMIME de la publicación de la reclamación firmada por el usuario 
	 *         que la publicó.
	 */
	def firmado () {
		if (params.int('id')) {
			def evento = Evento.get(params.id)
			if (evento) {
				MensajeSMIME mensajeSMIME
				MensajeSMIME.withTransaction {
					mensajeSMIME = MensajeSMIME.findWhere(evento:evento, tipo:
						Tipo.EVENTO_RECLAMACION)
				}
				if (mensajeSMIME) {
					response.status = Respuesta.SC_OK
					response.contentLength = mensajeSMIME.contenido.length
					response.setContentType("text/plain")
					response.outputStream <<  mensajeSMIME.contenido
					response.outputStream.flush()
					return false
				}
			}
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'eventNotFound', args:[params.ids])
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', 
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
    
	/**
	 * Servicio para publicar votaciones.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Archivo firmado en formato SMIME en cuyo contenido se
	 *        encuentra la reclamación que se desea publicar en formato HTML.
	 * @return Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.
	 */
    def guardarAdjuntandoValidacion () {
        try {
            flash.respuesta = eventoReclamacionService.guardarEvento(
				params.smimeMessageReq, request.getLocale())
        } catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_EJECUCION
			render ex.getMessage()
			return false      
        }
    }
	
	/**
	 * Servicio que devuelve estadísticas asociadas a una reclamción.
	 *
	 * @httpMethod GET
	 * @param id Obligatorio. Identificador en la base de datos de la reclamación que se desea consultar.
	 * @return Documento JSON con las estadísticas asociadas a la reclamación solicitada.
	 */
    def estadisticas () {
        if (params.int('id')) {
            EventoReclamacion eventoReclamacion
            if (!params.evento) eventoReclamacion = EventoReclamacion.get(params.id)
            else eventoReclamacion = params.evento
            if (eventoReclamacion) {
                response.status = Respuesta.SC_OK
                def estadisticasMap = new HashMap()
                estadisticasMap.id = eventoReclamacion.id
                estadisticasMap.tipo = Tipo.EVENTO_RECLAMACION.toString()
                estadisticasMap.numeroFirmas = eventoReclamacion.firmas.size()
                estadisticasMap.estado =  eventoReclamacion.estado.toString()
                estadisticasMap.usuario = eventoReclamacion.usuario.getNif()
                estadisticasMap.fechaInicio = eventoReclamacion.getFechaInicio()
                estadisticasMap.fechaFin = eventoReclamacion.getFechaFin()
                estadisticasMap.solicitudPublicacionURL = "${grailsApplication.config.grails.serverURL}" +
					"/eventoReclamacion/firmado?id=${eventoReclamacion.id}"
                estadisticasMap.solicitudPublicacionValidadaURL = "${grailsApplication.config.grails.serverURL}" + 
					"/eventoVotacion/validado?id=${eventoReclamacion.id}"
				estadisticasMap.informacionFirmasReclamacionURL = "${grailsApplication.config.grails.serverURL}" + 
					"/evento/informacionFirmasReclamacion?id=${eventoReclamacion.id}"
				estadisticasMap.URL = "${grailsApplication.config.grails.serverURL}/evento" + 
					"/obtener?id=${eventoReclamacion.id}"
				render estadisticasMap as JSON
                return false
            }
            response.status = Respuesta.SC_NOT_FOUND
            render message(code: 'eventNotFound', args:[params.ids])
            return false
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
    
	/**
	 * Servicio que devuelve las firmas recibidas por una reclamación.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Archivo firmado en formato SMIME con los datos de la
	 * 		  reclamación origen de la copia de seguridad.
	 * @return Archivo zip con los archivos relacionados con la reclamación.
	 */
    def guardarSolicitudCopiaRespaldo () {
        EventoReclamacion eventoReclamacion
        if (!params.evento) {
            def mensajeJSON = JSON.parse(params.smimeMessageReq.getSignedContent())
            if (params.int('eventoId')) {
                eventoReclamacion = EventoReclamacion.get(mensajeJSON.eventoId)
                if (!eventoReclamacion) {
                    response.status = Respuesta.SC_NOT_FOUND
                    render message(code: 'eventNotFound', args:[mensajeJSON.eventoId])
                    return false
                }  
            } else {
                response.status = Respuesta.SC_ERROR_PETICION
                render message(code: 'error.PeticionIncorrectaHTML', args:[
					"${grailsApplication.config.grails.serverURL}/${params.controller}"])
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
                mensaje:message(code: 'error.SinCopiaRespaldo'),codigoEstado:Respuesta.SC_OK)
            response.setContentType("application/json")
            render respuesta.getMap() as JSON
            return false
        }
    }
    
}
