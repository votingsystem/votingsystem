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
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
* */
class EventoVotacionController {

    def eventoVotacionService
	def subscripcionService
	def httpService
	def grailsApplication
	
	def index = {}
	
	def inicializarEvento = {
		Respuesta respuesta
		SMIMEMessageWrapper smimeMessageReq
		if (!(request instanceof MultipartHttpServletRequest)) {
				log.debug "------------- La petición no es instancia de MultipartHttpServletRequest ------------"
				flash.respuesta = new Respuesta(codigoEstado:400,
						tipo: Tipo.PETICION_SIN_ARCHIVO)
				forward controller: "error400", action: "procesar"
				return false
		}
		try {
			MultipartFile multipartFile = ((MultipartHttpServletRequest) request)?.getFile(
				grailsApplication.config.SistemaVotacion.nombreEntidadFirmada);
			//ValidationResult validationResult
			if (multipartFile?.getBytes() != null) {
				try {
					smimeMessageReq = SMIMEMessageWrapper.build(
							new ByteArrayInputStream(multipartFile?.getBytes()), null)
				} catch (Exception ex) {
					flash.respuesta = new Respuesta(mensaje:ex.getMessage(),
						codigoEstado:500, tipo: Tipo.PETICION_CON_ERRORES)
					forward controller: "error500", action: "procesar"   
					return false
				}
				if (smimeMessageReq && smimeMessageReq.isValidSignature) {
					log.debug "firma valida"
					String serverURL = smimeMessageReq.getHeader("serverURL")[0]
					ControlAcceso controlAcceso = subscripcionService.comprobarControlAcceso(serverURL)					
					//TODO validar con la cadena de certificacion -> validationResult = smimeMessageReq.verify(pkixParams);
					respuesta = eventoVotacionService.salvarEvento(
						smimeMessageReq, controlAcceso, request.getLocale())
				} else {
					log.debug "firma erronea"
					flash.respuesta = new Respuesta(codigoEstado:400,
						tipo: Tipo.FIRMA_EVENTO_CON_ERRORES)
					forward controller: "error400", action: "procesar"
					return false
				}
			} else {
				log.debug ("params.archivoFirmado: ${params.archivoFirmado}" )
				log.debug "Peticion sin archivo"
				flash.respuesta = new Respuesta(codigoEstado:400,
						tipo: Tipo.PETICION_SIN_ARCHIVO)
				forward controller: "error400", action: "procesar"   
				return false
			}
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			flash.respuesta = new Respuesta(mensaje:ex.getMessage(),
			codigoEstado:500, tipo: Tipo.PETICION_CON_ERRORES)
			forward controller: "error500", action: "procesar"   
			return false
		}
		render respuesta.getMap() as JSON
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
					render message(code: 'eventoVotacion.eventoNotFound', args:[params.ids])
					return
			}
		} else {
			params.sort = "fechaInicio"
			log.debug " -Params: " + params
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
			eventosMap.offset = params.offset
		}
        eventosMap.numeroTotalEventosVotacionEnSistema = EventoVotacion.countByEstadoOrEstadoOrEstadoOrEstado(
			EventoVotacion.Estado.ACTIVO, EventoVotacion.Estado.CANCELADO, 
			EventoVotacion.Estado.FINALIZADO, EventoVotacion.Estado.PENDIENTE_COMIENZO)
		eventosMap.numeroEventosVotacionEnPeticion = eventoList.size()
		eventoList.collect {eventoItem ->
				eventosMap.eventos.votaciones.add(eventoVotacionService.
					optenerEventoVotacionJSONMap(eventoItem))
		}
		response.setContentType("application/json")
		render eventosMap as JSON
        return false
	}

    def obtenerVotos = {
        if (params.long('eventoVotacionId') && params.controlAccesoServerURL) {
			ControlAcceso controlAcceso = ActorConIP.findWhere(serverURL:params.controlAccesoServerURL)
			if (!controlAcceso) {
				response.status = 404
				render message(code: 'eventoVotacion.controlAccesoNotFound', args:[params.controlAccesoServerURL])
				return false
			}
            def eventoVotacion
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.findWhere(eventoVotacionId:params.eventoVotacionId,
					controlAcceso:controlAcceso)
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
                votos.collect {voto ->
                    String hashCertificadoVotoHex = hexConverter.marshal(
                        voto.certificado.hashCertificadoVotoBase64.getBytes()); 
                    def votoMap = [id:voto.id, 
                        hashCertificadoVotoBase64:voto.certificado.hashCertificadoVotoBase64,
                        opcionDeEventoId:voto.opcionDeEvento.opcionDeEventoId,
                        eventoVotacionId:voto.eventoVotacion.eventoVotacionId,                        
                        estado:voto.estado.toString(),	
						certificadoURL:"${grailsApplication.config.grails.serverURL}/certificado/certificadoDeVoto?hashCertificadoVotoHex=${hashCertificadoVotoHex}",
                        votoSMIMEURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${voto.mensajeSMIME.id}"]
					if(Voto.Estado.ANULADO.equals(voto.estado)) {
						AnuladorVoto anuladorVoto = AnuladorVoto.findWhere(voto:voto)
						votoMap.anuladorURL="${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${anuladorVoto?.mensajeSMIME?.id}"
					}
					votosMap.votos.add(votoMap)
                }
                eventoVotacion.opciones.collect {opcion ->
					def numeroVotos = Voto.findAllWhere(opcionDeEvento:opcion, estado:Voto.Estado.OK).size()
                    def opcionMap = [opcionDeEventoId:opcion.opcionDeEventoId,
                        contenido:opcion.contenido, numeroVotos:numeroVotos]
                    votosMap.opciones.add(opcionMap)
                    
                }
                response.status = 200
				response.setContentType("application/json")
				render votosMap as JSON
                return false
            }
            response.status = 404
            render message(code: 'eventoVotacion.eventoNotFound', args:[params.id])
            return false
        }
        response.status = 400
		render (view: 'index')
        return false
    }
	

    def estadisticas = {
		EventoVotacion eventoVotacion
		if (params.long('id')) {
            EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.get(params.id)
			} 
		} else if(params.long('eventoVotacionId') && params.controlAccesoServerURL) {
			ControlAcceso controlAcceso = ActorConIP.findWhere(serverURL:params.controlAccesoServerURL)
			if (!controlAcceso) {
				response.status = 404
				render message(code: 'eventoVotacion.controlAccesoNotFound', args:[params.controlAccesoServerURL])
				return false
			}
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.findWhere(eventoVotacionId:params.eventoVotacionId,
					controlAcceso:controlAcceso)
			}
		} else {
			response.status = 400
			render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
			return false
		}
        if (eventoVotacion) {
            response.status = 200
            def estadisticasMap = new HashMap()
			estadisticasMap.opciones = []
            estadisticasMap.id = eventoVotacion.id
			estadisticasMap.numeroVotos = Voto.countByEventoVotacion(eventoVotacion)
			estadisticasMap.numeroVotosOK = Voto.countByEventoVotacionAndEstado(
					eventoVotacion, Voto.Estado.OK)
			estadisticasMap.numeroVotosANULADOS = Voto.countByEventoVotacionAndEstado(
				eventoVotacion, Voto.Estado.ANULADO)								
			eventoVotacion.opciones.collect { opcion ->
				def numeroVotos = Voto.countByOpcionDeEventoAndEstado(
					opcion, Voto.Estado.OK)
				def opcionMap = [id:opcion.id, contenido:opcion.contenido,
					numeroVotos:numeroVotos, opcionDeEventoId:opcion.opcionDeEventoId]
				estadisticasMap.opciones.add(opcionMap)
			}
			estadisticasMap.informacionVotosURL="${grailsApplication.config.grails.serverURL}/eventoVotacion/obtenerVotos?eventoVotacionId=${eventoVotacion.eventoVotacionId}&controlAccesoServerURL=${eventoVotacion.controlAcceso?.serverURL}"
			if (params.callback) render "${params.callback}(${estadisticasMap as JSON})"
			else render estadisticasMap as JSON
            return false
        }
        response.status = 404
        render message(code: 'evento.eventoNotFound', args:[params.eventoVotacionId])
        return false
    }

    def comprobarFechas = {
		EventoVotacion eventoVotacion
		if (params.long('id')) {
            EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.get(params.id)
			} 
		}
		if(!eventoVotacion) {
			response.status = 404 //Not Found
			render message(code: 'eventoVotacion.eventoNotFound', args:[params.id])
			return
		}
		Respuesta respuesta = eventoVotacionService.comprobarFechasEvento(
			eventoVotacion, request.getLocale())
		response.status = respuesta.codigoEstado
		render respuesta?.evento?.estado?.toString()
		return false
	}
}
