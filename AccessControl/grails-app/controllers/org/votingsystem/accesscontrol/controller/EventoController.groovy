package org.votingsystem.accesscontrol.controller

import org.votingsystem.util.StringUtils;
import org.votingsystem.util.DateUtils;

import grails.converters.JSON

import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.util.*
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.*

/**
 * @infoController Eventos
 * @descController Servicios relacionados con los eventos del sistema.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventoController {

    def eventoVotacionService
    def eventoService

	/**
	 * @httpMethod [GET]
	 * @serviceURL [/evento/$id?]
	 * @param [id] Opcional. El identificador del evento en la base de datos. Si no se pasa ningún id 
	 *        la consulta se hará entre todos los eventos.
	 * @param [max] Opcional (por defecto 20). Número máximo de documentos que 
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param [order] Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de creación.
	 * @responseContentType [application/json]
	 * @return Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.
	 */
	def index() { 
        def eventoList = []
        def eventosMap = new HashMap()
        eventosMap.eventos = new HashMap()
        eventosMap.eventos.firmas = []
        eventosMap.eventos.votaciones = []
        eventosMap.eventos.reclamaciones = []
        if (params.long('id')) {
			Evento evento = null
			Evento.withTransaction {
				evento = Evento.get(params.long('id'))
			}
			if(!evento) {
				response.status = ResponseVS.SC_NOT_FOUND
				render message(code: 'eventNotFound', args:[params.id])
				return false
			} else {
				render eventoService.optenerEventoMap(evento) as JSON 
				return false
			} 
        } else {
			Evento.withTransaction {
				eventoList =  Evento.findAllByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO, params)
			}
            eventosMap.offset = params.long('offset')
        }
		log.debug "index - params: ${params}"
        eventosMap.numeroTotalEventosEnSistema = Evento.
				   countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO)
        eventosMap.numeroTotalEventosFirmaEnSistema = EventoFirma.
				   countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO)
        eventosMap.numeroTotalEventosReclamacionEnSistema = EventoReclamacion.
				   countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO)
        eventosMap.numeroTotalEventosVotacionEnSistema = EventoVotacion.
				   countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO)
        eventosMap.numeroEventosEnPeticion = eventoList.size()
        eventoList.each {eventoItem ->
                if (eventoItem instanceof EventoVotacion) {
					eventosMap.eventos.votaciones.add(eventoService.optenerEventoVotacionMap(eventoItem))
                } else if (eventoItem instanceof EventoFirma) {
					if(eventoItem.estado == Evento.Estado.PENDIENTE_DE_FIRMA ) return
                    eventosMap.eventos.firmas.add(eventoService.optenerEventoFirmaMap(eventoItem))
                } else if (eventoItem instanceof EventoReclamacion) {
                    eventosMap.eventos.reclamaciones.add(eventoService.optenerEventoReclamacionMap(eventoItem))
                }
        }
		eventosMap.numeroEventosVotacionEnPeticion = eventosMap.eventos.votaciones.size()
		eventosMap.numeroEventosFirmaEnPeticion = eventosMap.eventos.firmas.size()
		eventosMap.numeroEventosReclamacionEnPeticion = eventosMap.eventos.reclamaciones.size()
        render eventosMap as JSON
	}
	    
	/**
	 * Servicio que devuelve estadísticas asociadas a un evento.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/evento/$id/estadisticas]
	 * @param [id] Identificador en la base de datos del evento que se desea consultar.
	 * @return Documento JSON con estadísticas asociadas al evento consultado.
	 */
    def estadisticas () {
		Evento evento
		Evento.withTransaction {
			evento = Evento.get(params.id)
		}
		if (evento) {
			params.evento = evento
			if (evento instanceof EventoFirma) forward(controller:"eventoFirma",action:"estadisticas")
			if (evento instanceof EventoReclamacion) forward(controller:"eventoReclamacion",action:"estadisticas")
			if (evento instanceof EventoVotacion) forward(controller:"eventoVotacion",action:"estadisticas")
			return false
		}
		response.status = ResponseVS.SC_NOT_FOUND
		render message(code: 'eventNotFound', args:[params.id])
		return false
    }
  
	/**
	 * Servicio que cancela eventos
	 *
	 * @httpMethod [POST]
	 * @requestContentType application/x-pkcs7-signature Obligatorio. Archivo con los datos del evento que se desea 
	 * 				cancelar firmado por el usuario que publicó o un administrador de sistema.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
   def cancelled() {
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		ResponseVS respuesta = eventoService.cancelEvent(
			messageSMIMEReq, request.getLocale());
		if(ResponseVS.SC_OK == respuesta.statusCode) {
			response.status = ResponseVS.SC_OK
			response.setContentType(org.votingsystem.model.ContentTypeVS.SIGNED)
	    }
	    params.respuesta = respuesta
   }

   /**
	* Servicio que comprueba las fechas de un evento
	*
	* @param id  Obligatorio. El identificador del evento en la base de datos.
	* @httpMethod [GET]
	* @serviceURL [/evento/$id/comprobarFechas]
	* @return Si todo va bien devuelve un código de estado HTTP 200.
	*/
   def comprobarFechas () {
	   Evento evento
	   if (params.long('id')) {
		   Evento.withTransaction {
			   evento = Evento.get(params.id)
		   }
	   }
	   if(!evento) {
		   response.status = ResponseVS.SC_NOT_FOUND
		   render message(code: 'eventNotFound', args:[params.id])
		   return false
	   }
	   ResponseVS respuesta = eventoService.comprobarFechasEvento(evento, request.getLocale())
	   response.status = respuesta.statusCode
	   render respuesta?.evento?.estado?.toString()
	   return false
   }
	   
}