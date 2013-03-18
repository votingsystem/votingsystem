package org.sistemavotacion.controlacceso

import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.util.DateUtils;
import grails.converters.JSON
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.*
import org.sistemavotacion.smime.*
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
/**
 * @infoController Eventos
 * @descController Servicios relacionados con los eventos del sistema.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class EventoController {

    def eventoVotacionService
    def eventoService

	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/evento'.
	 */
	def index() { }
	
	/**
	 * @httpMethod GET
	 * @param id Opcional. El identificador del evento en la base de datos. Si no se pasa ningún id 
	 *        la consulta se hará entre todos los eventos.
	 * @param max Opcional (por defecto 20). Número máximo de documentos que 
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param order Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de creación.
	 * @return Página con manifiestos en formato JSON que cumplen con el criterio de búsqueda.
	 */
    def obtener () {
        def eventoList = []
        def eventosMap = new HashMap()
        eventosMap.eventos = new HashMap()
        eventosMap.eventos.firmas = []
        eventosMap.eventos.votaciones = []
        eventosMap.eventos.reclamaciones = []
        if (params.ids?.size() > 0) {
			Evento.withTransaction {	
				Evento.getAll(params.ids).collect {evento ->
					if (evento) eventoList << evento;
				}
			}
            if (eventoList.size() == 0) {
                    response.status = Respuesta.SC_NOT_FOUND
                    render message(code: 'eventNotFound', args:[params.ids])
                    return
            }
        } else {
			Evento.withTransaction {
				eventoList =  Evento.findAllByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO, params)
			}
            eventosMap.offset = params.long('offset')
        }
		log.debug "params: ${params}"
		
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
        eventoList.collect {eventoItem ->
                if (eventoItem instanceof EventoVotacion) {
					eventosMap.eventos.votaciones.add(eventoService.optenerEventoVotacionJSONMap(eventoItem))
                } else if (eventoItem instanceof EventoFirma) {
					if(eventoItem.estado == Evento.Estado.PENDIENTE_DE_FIRMA ) return
                    eventosMap.eventos.firmas.add(eventoService.optenerEventoFirmaJSONMap(eventoItem))
                } else if (eventoItem instanceof EventoReclamacion) {
                    eventosMap.eventos.reclamaciones.add(eventoService.optenerEventoReclamacionJSONMap(eventoItem))
                }
        }
		eventosMap.numeroEventosVotacionEnPeticion = eventosMap.eventos.votaciones.size()
		eventosMap.numeroEventosFirmaEnPeticion = eventosMap.eventos.firmas.size()
		eventosMap.numeroEventosReclamacionEnPeticion = eventosMap.eventos.reclamaciones.size()
		response.setContentType("application/json")
        render eventosMap as JSON
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
        try {
            params.respuesta = eventoVotacionService.guardarEvento(
				params.smimeMessageReq, request.getLocale())
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_EJECUCION
			render ex.getMessage()
			return false       
        }
    }
    
	/**
	 * Servicio que devuelve estadísticas asociadas a un evento.
	 *
	 * @httpMethod GET
	 * @param id Identificador en la base de datos del evento que se desea consultar.
	 * @return Estadísticas asociadas al evento que se desea consultar en formato JSON.
	 */
    def estadisticas () {
		if (params.long('id')) {
			Evento evento
			Evento.withTransaction {
				evento = Evento.get(params.id)
			}
			if (evento) {
				params.evento = evento
				if (evento instanceof EventoFirma) forward(controller:"eventoFirma",action:"estadisticas")
				if (evento instanceof EventoReclamacion) forward(controller:"eventoReclamacion",action:"estadisticas")
				if (evento instanceof EventoVotacion) forward(controller:"eventoVotacion",action:"estadisticas")
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
	 * Servicio que devuelve los archivos relacionados con un evento.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Archivo firmado en formato SMIME con los datos del
	 * 		  evento origen de la copia de seguridad.
	 * @return Archivo zip con los archivos relacionados con un evento.
	 */
    def guardarSolicitudCopiaRespaldo () {
		SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
        def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
            if (params.long('eventoId')) {
                Evento evento = Evento.get(mensajeJSON.eventoId)
                if (evento) {
                    params.evento = evento
                    if (evento instanceof EventoFirma) forward(
						controller:"eventoFirma",action:"guardarSolicitudCopiaRespaldo")
                    if (evento instanceof EventoReclamacion) forward(
						controller:"eventoReclamacion",action:"guardarSolicitudCopiaRespaldo")
                    if (evento instanceof EventoVotacion) forward(
						controller:"eventoVotacion",action:"guardarSolicitudCopiaRespaldo")
                    return false
                }
                response.status = Respuesta.SC_NOT_FOUND
                render message(code: 'eventNotFound', args:[mensajeJSON.eventoId])
                return false
            }
            response.status = Respuesta.SC_ERROR_PETICION
            render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
            return false
    }
    
	/**
	 * Servicio que devuelve información sobre la actividad de una votación
	 *
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador de la votación en la base de datos.
	 * @return Información sobre los votos y solicitudes de acceso de una votación en formato JSON.
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
				"/certificado/certificadoCA_DeEvento?idEvento=${params.id}"
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
						"/mensajeSMIME/obtener?id=${anuladorVoto?.mensajeSMIME?.id}"
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
						"/certificadoDeVoto?hashCertificadoVotoHex=${hashCertificadoVotoHex}",
					votoURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME" + 
						"/obtener?id=${voto.mensajeSMIME.id}"]
				if(Voto.Estado.ANULADO.equals(voto.estado)) {
					AnuladorVoto anuladorVoto = AnuladorVoto.findWhere(voto:voto)
					votoMap.anuladorURL="${grailsApplication.config.grails.serverURL}" + 
						"/mensajeSMIME/obtener?id=${anuladorVoto?.mensajeSMIME?.id}"
				}
				informacionVotosMap.votos.add(votoMap)
			}
            response.status = Respuesta.SC_OK
            response.setContentType("application/json")
            render informacionVotosMap as JSON
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', 
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false  
    }
	
	/**
	 * Servicio que devuelve información sobre la actividad de una acción de recogida de firmas
	 *
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador del manifiesto en la base de datos.
	 * @return Información sobre las firmas recibidas en formato JSON.
	 */
    def informacionFirmas () {
        if (params.long('id')) {
            EventoFirma evento = EventoFirma.get(params.id)
            if (!evento) {
                response.status = Respuesta.SC_NOT_FOUND
                render message(code: 'eventNotFound', args:[params.id])
                return false
            }
            def informacionFirmasMap = new HashMap()
			def firmas
			Documento.withTransaction {
				firmas = Documento.findAllWhere(evento:evento,
					estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
			}
            informacionFirmasMap.numeroFirmas = firmas.size()
            informacionFirmasMap.asuntoEvento = evento.asunto
            informacionFirmasMap.eventoURL = 
				"${grailsApplication.config.grails.serverURL}/evento/obtener?id=${evento.id}"
            informacionFirmasMap.firmas = []
            firmas.collect { firma ->
                def firmaMap = [id:firma.id, fechaCreacion:firma.dateCreated,
                usuario:firma.usuario.nif,
                firmaURL:"${grailsApplication.config.grails.serverURL}/documento" + 
					"/obtenerFirmaManifiesto?id=${firma.id}"]
                informacionFirmasMap.firmas.add(firmaMap)
            }
            response.status = Respuesta.SC_OK
            response.setContentType("application/json")
            render informacionFirmasMap as JSON
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', 
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false  
    } 
    
	/**
	 * Servicio que devuelve información sobre la actividad de una acción de reclamación
	 *
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador de la reclamación la base de datos.
	 * @return Información sobre las reclamaciones recibidas en formato JSON.
	 */
    def informacionFirmasReclamacion () {
        if (params.long('id')) {
            EventoReclamacion evento = EventoReclamacion.get(params.id)
            if (!evento) {
                response.status = Respuesta.SC_NOT_FOUND
                render message(code: 'eventNotFound', args:[params.id])
                return false
            }
            def informacionReclamacionMap = new HashMap()
			List<Firma> firmas
			Firma.withTransaction {
				firmas = Firma.findAllWhere(evento:evento, tipo:Tipo.FIRMA_EVENTO_RECLAMACION)
			}
			log.debug("count: " + Firma.findAllWhere(
				evento:evento, tipo:Tipo.FIRMA_EVENTO_RECLAMACION).size())
            informacionReclamacionMap.numeroFirmas = firmas.size()
            informacionReclamacionMap.asuntoEvento = evento.asunto
            informacionReclamacionMap.eventoURL = 
				"${grailsApplication.config.grails.serverURL}/evento/obtener?id=${evento.id}"
            informacionReclamacionMap.firmas = []
            firmas.collect { firma ->
                def firmaMap = [id:firma.id, fechaCreacion:firma.dateCreated,
                usuario:firma.usuario.nif,
                firmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME" + 
					"/obtener?id=${firma.mensajeSMIME.id}",
                reciboFirmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME" +
					"/obtenerReciboReclamacion?id=${firma.mensajeSMIME?.id}"]
                def valoresCampos = ValorCampoDeEvento.findAllWhere(firma:firma)
                firmaMap.campos = []
                valoresCampos.collect { valorCampo ->
                    firmaMap.campos.add([campo:valorCampo.campoDeEvento.contenido, valor:valorCampo.valor])
                }
                informacionReclamacionMap.firmas.add(firmaMap)
            }
            response.status = Respuesta.SC_OK
            response.setContentType("application/json")
            render informacionReclamacionMap as JSON
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', 
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false  
    }
  
	/**
	 * Servicio que cancela eventos
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Obligatorio. Archivo con los datos del evento que se desea 
	 * cancelar firmado por el usuario que publicó o un administrador de sistema.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
   def guardarCancelacion() {
	   SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq;
	   if(params.smimeMessageReq) {
		   Respuesta respuesta = eventoService.cancelarEvento(
			   params.smimeMessageReq, request.getLocale());
		   response.status = respuesta.codigoEstado;
		   log.debug("statuscode: ${respuesta.codigoEstado} - mensaje: ${respuesta.mensaje}")
		   render respuesta.mensaje;
		   return false
	   }
	   response.status = Respuesta.SC_ERROR_PETICION;
	   render message(code: 'error.PeticionIncorrectaHTML', 
		   args:["${grailsApplication.config.grails.serverURL}/${params.controller}"]);
	   return false;
   }

   /**
	* Servicio que comprueba las fechas de un evento
	*
	* @param id  Obligatorio. El identificador del evento en la base de datos.
	* @httpMethod GET
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
		   response.status = Respuesta.SC_NOT_FOUND
		   render message(code: 'eventNotFound', args:[params.id])
		   return
	   }
	   Respuesta respuesta = eventoService.comprobarFechasEvento(evento, request.getLocale())
	   response.status = respuesta.codigoEstado
	   render respuesta?.evento?.estado?.toString()
	   return false
   }
	   
}