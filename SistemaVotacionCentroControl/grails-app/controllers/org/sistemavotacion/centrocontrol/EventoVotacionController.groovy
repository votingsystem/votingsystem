package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.smime.*
import org.sistemavotacion.utils.*
import org.sistemavotacion.util.StringUtils
import grails.converters.JSON
import org.sistemavotacion.util.DateUtils;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;

/**
 * @infoController Votaciones
 * @descController Servicios relacionados con las votaciones publicadas en el servidor.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class EventoVotacionController {

    def eventoVotacionService
	def subscripcionService
	def httpService
	def grailsApplication

	/**
	 * Servicio de consulta de las votaciones publicadas.
	 *
	 * @param [id]  Opcional. El identificador en la base de datos del documento que se
	 * 			  desee consultar.
	 * @param [max]	Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset]	Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @httpMethod [GET]
	 * @serviceURL [/eventoVotacion/$id?]
	 * @responseContentType [application/json]
	 * @return Documento JSON con las votaciones que cumplen el criterio de búsqueda.
	 */
	def index() {
		List eventoList = []
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
				render message(code: 'eventoVotacion.eventoNotFound', args:[params.id])
				return false
			} else {
				render eventoVotacionService.
					optenerEventoVotacionJSONMap(evento) as JSON
				return false
			}
		} else {
			params.sort = "fechaInicio"
			EventoVotacion.Estado estadoEvento
			if(params.estadoEvento) estadoEvento = EventoVotacion.Estado.valueOf(params.estadoEvento)
			EventoVotacion.withTransaction {
				if(estadoEvento) {
					if(estadoEvento == EventoVotacion.Estado.FINALIZADO) {
						eventoList =  EventoVotacion.findAllByEstadoOrEstado(
							EventoVotacion.Estado.CANCELADO, EventoVotacion.Estado.FINALIZADO, params)
					} else {
						eventoList =  EventoVotacion.findAllByEstado(estadoEvento, params)
					}
				} else {
					eventoList =  EventoVotacion.findAllByEstadoOrEstadoOrEstadoOrEstado(EventoVotacion.Estado.ACTIVO,
						   EventoVotacion.Estado.CANCELADO, EventoVotacion.Estado.FINALIZADO,
						   EventoVotacion.Estado.PENDIENTE_COMIENZO, params)
				}
			}
			eventosMap.offset = params.long('offset')
		}
		eventosMap.numeroTotalEventosVotacionEnSistema = EventoVotacion.countByEstadoOrEstadoOrEstadoOrEstado(
			EventoVotacion.Estado.ACTIVO, EventoVotacion.Estado.CANCELADO,
			EventoVotacion.Estado.FINALIZADO, EventoVotacion.Estado.PENDIENTE_COMIENZO)
		eventosMap.numeroEventosVotacionEnPeticion = eventoList.size()
		eventoList.each {eventoItem ->
				eventosMap.eventos.votaciones.add(eventoVotacionService.
					optenerEventoVotacionJSONMap(eventoItem))
		}
		response.setContentType("application/json")
		render eventosMap as JSON
		return false
	}
	
	
	/**
	 * Servicio que da de alta las votaciones.
	 * 
	 * @httpMethod [POST]
	 * @serviceURL [/eventoVotacion]	 
	 * @contentType [application/x-pkcs7-signature] Obligatorio. El archivo con los datos de la votación firmado
	 * 		  por el usuario que la publica y el Control de Acceso en el que se publica.
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
		params.respuesta = eventoVotacionService.saveEvent(mensajeSMIME, request.getLocale())
		if(Respuesta.SC_OK == params.respuesta.codigoEstado) {
			response.status = Respuesta.SC_OK
			render params.respuesta.mensaje
		}
	}

	/**
	 * Servicio de consulta de los votos
	 *
	 * @param [eventAccessControlURL] Obligatorio. URL del evento en el Control de Acceso.
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 * @return Documento JSON con la lista de votos recibidos por la votación solicitada.
	 */
    def votes () {
        if (params.eventAccessControlURL) {
            def eventoVotacion
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.findWhere(url:params.eventAccessControlURL)
			}
            if (eventoVotacion) {
                def votosMap = new HashMap()
                votosMap.votos = []
                votosMap.opciones = []
				def votos
				Voto.withTransaction {
					votos = Voto.findAllWhere(eventoVotacion:eventoVotacion)
				}
				def votosAnulados = Voto.findAllWhere(eventoVotacion:eventoVotacion, estado:Voto.Estado.ANULADO)
				def votosOk = Voto.findAllWhere(eventoVotacion:eventoVotacion, estado:Voto.Estado.OK)
                votosMap.numeroVotos = votos.size()
				votosMap.numeroVotosOK = votosOk.size()
				votosMap.numeroVotosANULADOS = votosAnulados.size()
                votosMap.controlAccesoURL=eventoVotacion.controlAcceso.serverURL
				votosMap.eventoVotacionURL=eventoVotacion.url
                HexBinaryAdapter hexConverter = new HexBinaryAdapter();
                votos.each {voto ->
                    String hashCertificadoVotoHex = hexConverter.marshal(
                        voto.certificado.hashCertificadoVotoBase64.getBytes()); 
                    def votoMap = [id:voto.id, 
                        hashCertificadoVotoBase64:voto.certificado.hashCertificadoVotoBase64,
                        opcionDeEventoId:voto.opcionDeEvento.opcionDeEventoId,
                        eventoVotacionId:voto.eventoVotacion.eventoVotacionId,                        
                        estado:voto.estado.toString(),	
						certificadoURL:"${grailsApplication.config.grails.serverURL}/certificado/voto/hashHex/${hashCertificadoVotoHex}",
                        votoSMIMEURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/${voto.mensajeSMIME.id}"]
					if(Voto.Estado.ANULADO.equals(voto.estado)) {
						AnuladorVoto anuladorVoto
						AnuladorVoto.withTransaction {
							anuladorVoto = AnuladorVoto.findWhere(voto:voto)
						}
						votoMap.anuladorURL="${grailsApplication.config.grails.serverURL}/mensajeSMIME/${anuladorVoto?.mensajeSMIME?.id}"
					}
					votosMap.votos.add(votoMap)
                }
                eventoVotacion.opciones.each {opcion ->
					def numeroVotos = Voto.findAllWhere(opcionDeEvento:opcion, estado:Voto.Estado.OK).size()
                    def opcionMap = [opcionDeEventoId:opcion.opcionDeEventoId,
                        contenido:opcion.contenido, numeroVotos:numeroVotos]
                    votosMap.opciones.add(opcionMap)
                    
                }
                response.status = Respuesta.SC_OK
				response.setContentType("application/json")
				render votosMap as JSON
                return false
            }
            response.status = Respuesta.SC_NOT_FOUND
            render message(code: 'eventoUrlNotFound', args:[params.eventAccessControlURL])
            return false
        }
        response.status = Respuesta.SC_ERROR_PETICION
	    render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]);
        return false
    }

	/**
	 * Servicio que ofrece datos de recuento de una votación.
	 * 
	 * @param [eventAccessControlURL] Obligatorio. URL del evento en el Control de Acceso.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoVotacion/$id/estadisticas] 
	 * @param [id] Opcional. El identificador en la base de datos del evento consultado.
     * @param [eventAccessControlURL] Opcional. La url del evento en el Control de Asceso 
     *         que se publicó
	 * @responseContentType [application/json]
	 * @return Documento JSON con estadísticas de la votación solicitada.
	 */
    def estadisticas () {
		EventoVotacion eventoVotacion
		if (params.long('id')) {
            EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.get(params.long('id'))
			} 
			if (!eventoVotacion) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'evento.eventoNotFound', args:[params.id])
				return false
			}
		} else if(params.eventAccessControlURL) {
			log.debug("params.eventAccessControlURL: ${params.eventAccessControlURL}")
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.findByUrl(params.eventAccessControlURL.trim())
			}
			if (!eventoVotacion) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'evento.eventoNotFound', args:[params.eventAccessControlURL])
				return false
			}
		}
        if (eventoVotacion) {
            response.status = Respuesta.SC_OK
            def estadisticasMap = new HashMap()
			estadisticasMap.opciones = []
            estadisticasMap.id = eventoVotacion.id
			estadisticasMap.numeroVotos = Voto.countByEventoVotacion(eventoVotacion)
			estadisticasMap.numeroVotosOK = Voto.countByEventoVotacionAndEstado(
					eventoVotacion, Voto.Estado.OK)
			estadisticasMap.numeroVotosANULADOS = Voto.countByEventoVotacionAndEstado(
				eventoVotacion, Voto.Estado.ANULADO)								
			eventoVotacion.opciones.each { opcion ->
				def numeroVotos = Voto.countByOpcionDeEventoAndEstado(
					opcion, Voto.Estado.OK)
				def opcionMap = [id:opcion.id, contenido:opcion.contenido,
					numeroVotos:numeroVotos, opcionDeEventoId:opcion.opcionDeEventoId]
				estadisticasMap.opciones.add(opcionMap)
			}
			estadisticasMap.informacionVotosURL="${grailsApplication.config.grails.serverURL}/eventoVotacion/votes?eventAccessControlURL=${eventoVotacion.url}"
			if (params.callback) render "${params.callback}(${estadisticasMap as JSON})"
			else render estadisticasMap as JSON
            return false
        } else {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.PeticionIncorrectaHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
			return false
		}
    }

	/**
	 * Servicio que comprueba las fechas de una votación
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoVotacion/$id/comprobarFechas]
	 * @param [id] Obligatorio. El identificador de la votación en la base de datos.	
	 */
    def comprobarFechas () {
		EventoVotacion eventoVotacion
		if (params.long('id')) {
            EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.get(params.id)
			} 
		}
		if(!eventoVotacion) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'eventoVotacion.eventoNotFound', args:[params.id])
			return
		}
		Respuesta respuesta = eventoVotacionService.comprobarFechasEvento(
			eventoVotacion, request.getLocale())
		response.status = respuesta.codigoEstado
		render respuesta?.evento?.estado?.toString()
		return false
	}
	
	/**
	 * Servicio de cancelación de votaciones 
	 *
	 * @contentType [application/x-pkcs7-signature] Obligatorio. Archivo con los datos de la votación que se 
	 * 			desea cancelar firmado por el Control de Acceso que publicó la votación y por el usuario que 
	 *          la publicó o un administrador de sistema.
	 * @httpMethod [POST]
	 */
	def cancelled() {
		MensajeSMIME mensajeSMIMEReq = params.mensajeSMIMEReq
		if(!mensajeSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		Respuesta respuesta = eventoVotacionService.cancelEvent(
			mensajeSMIMEReq, request.getLocale());
		if(Respuesta.SC_OK == respuesta.codigoEstado) {
			response.status = Respuesta.SC_OK
			response.setContentType("${grailsApplication.config.pkcs7SignedContentType}")
		}
		params.respuesta = respuesta
	}
	
	
	/**
	 * Servicio que devuelve un archivo zip con los errores que se han producido
	 * en una votación
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoVotacion/$id/votingErrors]
	 * @param [id] Obligatorio. Identificador del evento en la base de datos
	 * @return Archivo zip con los mensajes con errores
	 */
	def votingErrors() {
		EventoVotacion event
		EventoVotacion.withTransaction {
			event = EventoVotacion.get(params.long('id'))
		}
		if (!event) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'eventNotFound', args:[params.id])
			return false
		}
		def errors
		MensajeSMIME.withTransaction {
			errors = MensajeSMIME.findAllWhere (
				tipo:Tipo.VOTO_CON_ERRORES,  evento:event)
		}
		
		if(errors.size == 0){
			response.status = Respuesta.SC_OK
			render message(code: 'votingWithoutErrorsMsg',
				args:[event.id, event.asunto])
		} else {
			String datePathPart = DateUtils.getShortStringFromDate(event.getDateFinish())
			String baseDirPath = "${grailsApplication.config.SistemaVotacion.errorsBaseDir}" +
				"/${datePathPart}/Event_${event.id}"
			errors.each { mensajeSMIME ->
				File errorFile = new File("${baseDirPath}/MensajeSMIME_${mensajeSMIME.id}")
				errorFile.setBytes(mensajeSMIME.contenido)
			}
			File zipResult = new File("${baseDirPath}.zip")
			def ant = new AntBuilder()
			ant.zip(destfile: zipResult, basedir: "${baseDirPath}")
			
			response.setContentType("application/zip")
		}
	}
}
