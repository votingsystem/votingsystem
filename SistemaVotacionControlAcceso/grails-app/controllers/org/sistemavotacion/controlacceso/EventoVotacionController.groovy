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
import javax.mail.Header;
import org.sistemavotacion.util.*
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
/**
 * @infoController Votaciones
 * @descController Servicios relacionados con la publicación de votaciones.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventoVotacionController {

    def eventoVotacionService
    def eventoService

	/**
	 * @httpMethod [GET]
     * @serviceURL [/eventoVotacion/$id]	 
	 * @param [id] Opcional. El identificador de la votación en la base de datos. Si no se pasa ningún id
	 *        la consulta se hará entre todos las votaciones.
	 * @param [max] Opcional (por defecto 20). Número máximo de votaciones que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param [order] Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @responseContentType [application/json]
	 * @return Documento JSON con las votaciones que cumplen con el criterio de búsqueda.
	 */
	def index() {
		try {
			def eventoList = []
			def eventosMap = new HashMap()
			eventosMap.eventos = new HashMap()
			eventosMap.eventos.votaciones = []
			if (params.long('id')) {
				EventoVotacion evento = null
				EventoVotacion.withTransaction {
					evento = EventoVotacion.get(params.long('id'))
				}
				if(!evento) {
					response.status = Respuesta.SC_NOT_FOUND
					render message(code: 'eventNotFound', args:[params.id])
					return false
				} else {
					render eventoService.optenerEventoJSONMap(evento) as JSON
					return false
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
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_PETICION
			render ex.getMessage()
			return false
		}
	}
	
	/**
	 * Servicio para publicar votaciones.
	 *
	 * @serviceURL [/eventoVotacion]
	 * @httpMethod [POST]
	 * 
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. Documento firmado
	 *                     en formato SMIME con los datos de la votación que se desea publicar
	 * @responseContentType [application/x-pkcs7-signature] Obligatorio. Recibo firmado por el sistema.
	 * 
	 * @return Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.
	 */
    def save () {
		MensajeSMIME mensajeSMIME = params.mensajeSMIMEReq
		if(!mensajeSMIME) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
        Respuesta respuesta = eventoVotacionService.saveEvent(
			mensajeSMIME, request.getLocale())
		params.respuesta = respuesta
		if (Respuesta.SC_OK == respuesta.codigoEstado) {
			response.contentType = grailsApplication.config.pkcs7SignedContentType
		}
    }

	/**
	 * Servicio que devuelve estadísticas asociadas a una votación.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoVotacion/$id/estadisticas]
	 * @param [id] Obligatorio. Identificador en la base de datos de la votación que se desea consultar.
	 * @responseContentType [application/json]
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
	 * @httpMethod [GET]
	 * @serviceURL [/eventoVotacion/${id}/validado]
	 * @param [id] Obligatorio. El identificador de la votación en la base de datos.
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
					List results = MensajeSMIME.withCriteria {
						createAlias("smimePadre", "smimePadre")
						eq("smimePadre.evento", evento)
						eq("smimePadre.tipo", Tipo.EVENTO_VOTACION)
					}
					mensajeSMIME = results.iterator().next()
				}
				
				log.debug("mensajeSMIME.id: ${mensajeSMIME.id}")
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
                render message(code: 'eventNotFound', args:[params.id])
                return false
            }
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
        return false
    }
	
	/**
	 * Servicio que proporciona una copia de la votación publicada.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoVotacion/${id}/firmado]
	 * @param [id] Obligatorio. El identificador de la votación en la base de datos.
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
			render message(code: 'eventNotFound', args:[params.id])
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

	/**
	 * Servicio que devuelve información sobre la actividad de una votación
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoVotacion/$id/informacionVotos]
	 * @param [id] Obligatorio. El identificador de la votación en la base de datos.
	 * @responseContentType [application/json]
	 * @return Documento JSON con información sobre los votos y solicitudes de acceso de una votación.
	 */
	def informacionVotos () {
		if (params.long('id')) {
			Evento evento
			Evento.withTransaction {
				evento = Evento.get(params.id)
			}
			if (!evento || !(evento instanceof EventoVotacion)) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'eventNotFound', args:[params.id])
				return false
			}
			def informacionVotosMap = new HashMap()
			def solicitudesOk, solicitudesAnuladas;
			SolicitudAcceso.withTransaction {
				solicitudesOk = SolicitudAcceso.findAllWhere(
					estado:Tipo.OK, eventoVotacion:evento)
				solicitudesAnuladas = SolicitudAcceso.findAllWhere(
					estado:Tipo.ANULADO, eventoVotacion:evento)
			}
			def solicitudesCSROk, solicitudesCSRAnuladas;
			SolicitudCSRVoto.withTransaction {
				solicitudesCSROk = SolicitudCSRVoto.findAllWhere(
					estado:SolicitudCSRVoto.Estado.OK, eventoVotacion:evento)
				solicitudesCSRAnuladas = SolicitudCSRVoto.findAllWhere(
					estado:SolicitudCSRVoto.Estado.ANULADA, eventoVotacion:evento)
			}
			informacionVotosMap.numeroTotalSolicitudes = evento.solicitudesAcceso.size()
			informacionVotosMap.numeroSolicitudes_OK = solicitudesOk.size()
			informacionVotosMap.numeroSolicitudes_ANULADAS = solicitudesAnuladas.size()
			informacionVotosMap.numeroTotalCSRs = evento.solicitudesCSR.size()
			informacionVotosMap.numeroSolicitudesCSR_OK = solicitudesCSROk.size()
			informacionVotosMap.solicitudesCSRAnuladas = solicitudesCSRAnuladas.size()
			informacionVotosMap.certificadoRaizEvento = "${grailsApplication.config.grails.serverURL}" +
				"/certificado/eventCA/${params.id}"
			informacionVotosMap.solicitudesAcceso = []
			informacionVotosMap.votos = []
			informacionVotosMap.opciones = []
			evento.solicitudesAcceso.collect { solicitud ->
				def solicitudMap = [id:solicitud.id, fechaCreacion:solicitud.dateCreated,
				estado:solicitud.estado.toString(),
				hashSolicitudAccesoBase64:solicitud.hashSolicitudAccesoBase64,
				usuario:solicitud.usuario.nif,
				solicitudAccesoURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME" +
					"/obtener?id=${solicitud?.mensajeSMIME?.id}"]
				if(SolicitudAcceso.Estado.ANULADO.equals(solicitud.estado)) {
					AnuladorVoto anuladorVoto = AnuladorVoto.findWhere(solicitudAcceso:solicitud)
					solicitudMap.anuladorURL="${grailsApplication.config.grails.serverURL}" +
						"/mensajeSMIME/${anuladorVoto?.mensajeSMIME?.id}"
				}
				informacionVotosMap.solicitudesAcceso.add(solicitudMap)
			}
			evento.opciones.collect { opcion ->
				def numeroVotos = Voto.findAllWhere(opcionDeEvento:opcion, estado:Voto.Estado.OK).size()
				def opcionMap = [id:opcion.id, contenido:opcion.contenido,
					numeroVotos:numeroVotos]
				informacionVotosMap.opciones.add(opcionMap)
			}
			
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			evento.votos.collect { voto ->
				def hashCertificadoVotoHex = hexConverter.marshal(
					voto.certificado.hashCertificadoVotoBase64.getBytes() )
				def votoMap = [id:voto.id, opcionSeleccionadaId:voto.opcionDeEvento.id,
					estado:voto.estado.toString(),
					hashCertificadoVotoBase64:voto.certificado.hashCertificadoVotoBase64,
					certificadoURL:"${grailsApplication.config.grails.serverURL}/certificado" +
						"/voto/${hashCertificadoVotoHex}",
					votoURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME" +
						"/${voto.mensajeSMIME.id}"]
				if(Voto.Estado.ANULADO.equals(voto.estado)) {
					AnuladorVoto anuladorVoto = AnuladorVoto.findWhere(voto:voto)
					votoMap.anuladorURL="${grailsApplication.config.grails.serverURL}" +
						"/mensajeSMIME/${anuladorVoto?.mensajeSMIME?.id}"
				}
				informacionVotosMap.votos.add(votoMap)
			}
			response.status = Respuesta.SC_OK
			response.setContentType("application/json")
			render informacionVotosMap as JSON
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML',
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
		return false
	}
}