package org.sistemavotacion.controlacceso

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import org.sistemavotacion.controlacceso.modelo.*
import grails.converters.JSON
import java.security.cert.X509Certificate;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper
import org.sistemavotacion.util.*

/**
 * @infoController Votaciones
 * @descController Servicios relacionados con la publicación de votaciones.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class EventoVotacionController {

    def eventoVotacionService
    def eventoService
	def almacenClavesService
	def notificacionService
	def httpService
	def encryptionService
    
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/eventoVotacion'.
	 */
	def index() { 
		redirect action: "restDoc"
	}
	
	/**
	 * Servicio para publicar votaciones.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Archivo firmado en formato SMIME en cuyo contenido se
	 *        encuentra la votación que se desea publicar en formato HTML.
	 * @return Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.
	 */
    def guardarAdjuntandoValidacion () {
        flash.respuesta = eventoVotacionService.guardarEvento(
			params.smimeMessageReq, request.getLocale())
		if (Respuesta.SC_OK == flash.respuesta.codigoEstado) {
			String initCentroControlEventURL = "${flash.respuesta.evento.centroControl.serverURL}" + 
				"${grailsApplication.config.SistemaVotacion.sufijoURLInicializacionEvento}"
			byte[] cadenaCertificacionCentroControlBytes = 
				flash.respuesta.evento.cadenaCertificacionCentroControl
			X509Certificate controlCenterCert = CertUtil.fromPEMToX509CertCollection(
				cadenaCertificacionCentroControlBytes).iterator().next()
			if(!controlCenterCert) {
				String msg = "${message(code: 'controlCenterNullCert')}"
				log.error msg
				setEventToPending(flash.respuesta.evento);
				flash.respuesta = new Respuesta(mensaje:msg,
					codigoEstado:Respuesta.SC_ERROR_PETICION)
				return false
			}
			Respuesta respuesta = encryptionService.encryptSMIMEMessage(
					flash.respuesta.mensajeSMIMEValidado.contenido, 
					controlCenterCert, request.getLocale())
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				String msg = ${message(code: 'error.encryptErrorMsg')}
				log.error msg
				setEventToPending(flash.respuesta.evento);
				flash.respuesta = new Respuesta(mensaje:msg,
					codigoEstado:Respuesta.SC_ERROR_PETICION)
				return false
			}
			Respuesta respuestaNotificacion = httpService.sendSignedMessage(
				initCentroControlEventURL, respuesta.messageBytes)
			if(Respuesta.SC_OK != respuestaNotificacion.codigoEstado) {
				log.debug("Problemas notificando evento '${flash.respuesta.evento.id}' al Centro de " + 
					"Control - codigo estado:${respuestaNotificacion.codigoEstado}" +
					" - mensaje: ${respuestaNotificacion.mensaje}")	
				setEventToPending(flash.respuesta.evento);
				String msg = "${message(code: 'http.errorConectandoCentroControl')} - " + 
					"${respuestaNotificacion.mensaje}"
				flash.respuesta = new Respuesta(mensaje:msg, 
					codigoEstado:Respuesta.SC_ERROR_PETICION)
			} 
		}
		return false 
    }
	
	private void setEventToPending(Evento evento) {
		evento.estado = Evento.Estado.ACTORES_PENDIENTES_NOTIFICACION
		Evento.withTransaction { evento.save() }
	}
    
	/**
	 * @httpMethod GET
	 * @param id Opcional. El identificador de la votación en la base de datos. Si no se pasa ningún id
	 *        la consulta se hará entre todos las votaciones.
	 * @param max Opcional (por defecto 20). Número máximo de votaciones que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param order Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @return Documento JSON con las votaciones que cumplen con el criterio de búsqueda.
	 */
    def obtener () {
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
                    response.status = Respuesta.SC_NOT_FOUND
                    render message(code: 'eventNotFound', args:[params.ids])
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
						log.debug " -estadoEvento: " + estadoEvento
						eventosMap.numeroTotalEventosVotacionEnSistema = EventoVotacion.countByEstado(estadoEvento)
					}
				} else {
					eventoList =  EventoVotacion.findAllByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
						   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO, params)
					eventosMap.numeroTotalEventosVotacionEnSistema = 
							EventoVotacion.countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
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

	/**
	 * Servicio que devuelve estadísticas asociadas a una votación.
	 *
	 * @httpMethod GET
	 * @param id Obligatorio. Identificador en la base de datos de la votación que se desea consultar.
	 * @return Documento JSON con las estadísticas asociadas a la votación solicitada.
	 */
    def estadisticas () {
        if (params.long('id')) {
            EventoVotacion eventoVotacion
            if (!params.evento) {
				EventoVotacion.withTransaction {
					eventoVotacion = EventoVotacion.get(params.id)
				}
			} 
            else eventoVotacion = params.evento
            if (eventoVotacion) {
                response.status = Respuesta.SC_OK
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
            response.status = Respuesta.SC_NOT_FOUND
            render message(code: 'eventNotFound', args:[params.id])
            return false
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
    
	/**
	 * Servicio que proporciona una copia de la votación publicada con la firma
	 * añadida del servidor.
	 *
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador de la votación en la base de datos.
	 * @return Archivo SMIME de la publicación de la votación firmada por el usuario que
	 *         la publicó y el servidor.
	 */
    def validado () {
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
        render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
	
	/**
	 * Servicio que proporciona una copia de la votación publicada.
	 *
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador de la votación en la base de datos.
	 * @return Archivo SMIME de la publicación de la votación firmada por el usuario
	 *         que la publicó.
	 */
	def firmado () {
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
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * Servicio que devuelve los votos y las solicitudes de acceso recibidas en una votación.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Archivo firmado en formato SMIME con los datos de la
	 * 		  votación origen de la copia de seguridad.
	 * @return Archivo zip con los archivos relacionados con la votación.
	 */
	def guardarSolicitudCopiaRespaldo () {
		EventoVotacion eventoVotacion
		if (!params.evento) {
			SMIMEMessageWrapper smimeMessage= params.smimeMessageReq
			def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			if (params.long('eventoId')) {
				eventoVotacion = EventoVotacion.get(mensajeJSON.eventoId)
				if (!eventoVotacion) {
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
			log.error (message(code: 'error.SinCopiaRespaldo'))
			response.status = Respuesta.SC_ERROR_EJECUCION
			render message(code: 'error.SinCopiaRespaldo')
			return false
		}
	}

}