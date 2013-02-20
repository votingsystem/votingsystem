package org.sistemavotacion.controlacceso

import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.util.DateUtils;
import grails.converters.JSON
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.*
import org.sistemavotacion.smime.*
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class EventoController {

    def eventoVotacionService
    def eventoService

	def index() { }
	
    def obtener = {
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
                    response.status = 404 //Not Found
                    render message(code: 'evento.eventoNotFound', args:[params.ids])
                    return
            }
        } else {
			Evento.withTransaction {
				eventoList =  Evento.findAllByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO, params)
			}
            eventosMap.offset = params.long('offset')
        }
        eventosMap.numeroTotalEventosEnSistema = Evento.countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO)
        eventosMap.numeroTotalEventosFirmaEnSistema = EventoFirma.countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO)
        eventosMap.numeroTotalEventosReclamacionEnSistema = EventoReclamacion.countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
				   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO)
        eventosMap.numeroTotalEventosVotacionEnSistema = EventoVotacion.countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
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
    
    
    def guardarAdjuntandoValidacion = {
        try {
            params.respuesta = eventoVotacionService.guardarEvento(
				params.smimeMessageReq, request.getLocale())
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
            flash.respuesta = new Respuesta(mensaje:ex.getMessage(),
                codigoEstado:500, tipo: Tipo.ERROR_DE_SISTEMA)
    		forward controller: "error500", action: "procesar"        
        }
    }
    
    def estadisticas = {
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
			response.status = 404
			render message(code: 'evento.eventoNotFound', args:[params.id])
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false  
    }
    
    def guardarSolicitudCopiaRespaldo = {
		SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
        def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
            if (params.long('eventoId')) {
                Evento evento = Evento.get(mensajeJSON.eventoId)
                if (evento) {
                    params.evento = evento
                    if (evento instanceof EventoFirma) forward(controller:"eventoFirma",action:"guardarSolicitudCopiaRespaldo")
                    if (evento instanceof EventoReclamacion) forward(controller:"eventoReclamacion",action:"guardarSolicitudCopiaRespaldo")
                    if (evento instanceof EventoVotacion) forward(controller:"eventoVotacion",action:"guardarSolicitudCopiaRespaldo")
                    return false
                }
                response.status = 404
                render message(code: 'evento.eventoNotFound', args:[mensajeJSON.eventoId])
                return false
            }
            response.status = 400
            render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
            return false
    }
    
    def informacionVotos = {
        if (params.long('id')) {
			Evento evento
			Evento.withTransaction {
				evento = Evento.get(params.id)
			}
            if (!evento || !(evento instanceof EventoVotacion)) {
                response.status = 404
                render message(code: 'evento.eventoNotFound', args:[params.id])
                return false
            }
            def informacionVotosMap = new HashMap()
			def solicitudesOk, solicitudesAnuladas;
			SolicitudAcceso.withTransaction {
				solicitudesOk = SolicitudAcceso.findAllWhere(estado:Tipo.OK, eventoVotacion:evento)
				solicitudesAnuladas = SolicitudAcceso.findAllWhere(estado:Tipo.ANULADO, eventoVotacion:evento)
			}
			def solicitudesCSROk, solicitudesCSRAnuladas;
			SolicitudCSRVoto.withTransaction {
				solicitudesCSROk = SolicitudCSRVoto.findAllWhere(estado:SolicitudCSRVoto.Estado.OK, eventoVotacion:evento)
				solicitudesCSRAnuladas = SolicitudCSRVoto.findAllWhere(estado:SolicitudCSRVoto.Estado.ANULADA, eventoVotacion:evento)
			}
            informacionVotosMap.numeroTotalSolicitudes = evento.solicitudesAcceso.size()
            informacionVotosMap.numeroSolicitudes_OK = solicitudesOk.size()
			informacionVotosMap.numeroSolicitudes_ANULADAS = solicitudesAnuladas.size()
            informacionVotosMap.numeroTotalCSRs = evento.solicitudesCSR.size()
            informacionVotosMap.numeroSolicitudesCSR_OK = solicitudesCSROk.size()
			informacionVotosMap.solicitudesCSRAnuladas = solicitudesCSRAnuladas.size()
			informacionVotosMap.certificadoRaizEvento = "${grailsApplication.config.grails.serverURL}/certificado/certificadoCA_DeEvento?idEvento=${params.id}"
            informacionVotosMap.solicitudesAcceso = []
			informacionVotosMap.votos = []
			informacionVotosMap.opciones = []
            evento.solicitudesAcceso.collect { solicitud ->
                def solicitudMap = [id:solicitud.id, fechaCreacion:solicitud.dateCreated,
                estado:solicitud.estado.toString(), 
                hashSolicitudAccesoBase64:solicitud.hashSolicitudAccesoBase64,
                usuario:solicitud.usuario.nif,
                solicitudAccesoURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${solicitud?.mensajeSMIME?.id}"]
				if(SolicitudAcceso.Estado.ANULADO.equals(solicitud.estado)) {
					AnuladorVoto anuladorVoto = AnuladorVoto.findWhere(solicitudAcceso:solicitud)
					solicitudMap.anuladorURL="${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${anuladorVoto?.mensajeSMIME?.id}"
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
					certificadoURL:"${grailsApplication.config.grails.serverURL}/certificado/certificadoDeVoto?hashCertificadoVotoHex=${hashCertificadoVotoHex}",
					votoURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${voto.mensajeSMIME.id}"]
				if(Voto.Estado.ANULADO.equals(voto.estado)) {
					AnuladorVoto anuladorVoto = AnuladorVoto.findWhere(voto:voto)
					votoMap.anuladorURL="${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${anuladorVoto?.mensajeSMIME?.id}"
				}
				informacionVotosMap.votos.add(votoMap)
			}
            response.status = 200
            response.setContentType("application/json")
            render informacionVotosMap as JSON
        }
        response.status = 400
        render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false  
    }
    
   def informacionFirmas = {
        if (params.long('id')) {
            EventoFirma evento = EventoFirma.get(params.id)
            if (!evento) {
                response.status = 404
                render message(code: 'evento.eventoNotFound', args:[params.id])
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
            informacionFirmasMap.eventoURL = "${grailsApplication.config.grails.serverURL}/evento/obtener?id=${evento.id}"
            informacionFirmasMap.firmas = []
            firmas.collect { firma ->
                def firmaMap = [id:firma.id, fechaCreacion:firma.dateCreated,
                usuario:firma.usuario.nif,
                firmaURL:"${grailsApplication.config.grails.serverURL}/documento/obtenerFirmaManifiesto?id=${firma.id}"]
                informacionFirmasMap.firmas.add(firmaMap)
            }
            response.status = 200
            response.setContentType("application/json")
            render informacionFirmasMap as JSON
        }
        response.status = 400
        render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false  
    } 
    
   def informacionFirmasReclamacion = {
        if (params.long('id')) {
            EventoReclamacion evento = EventoReclamacion.get(params.id)
            if (!evento) {
                response.status = 404
                render message(code: 'evento.eventoNotFound', args:[params.id])
                return false
            }
            def informacionReclamacionMap = new HashMap()
			List<Firma> firmas
			Firma.withTransaction {
				firmas = Firma.findAllWhere(evento:evento, tipo:Tipo.FIRMA_EVENTO_RECLAMACION)
			}
			log.debug("count: " + Firma.findAllWhere(evento:evento, tipo:Tipo.FIRMA_EVENTO_RECLAMACION).size())
            informacionReclamacionMap.numeroFirmas = firmas.size()
            informacionReclamacionMap.asuntoEvento = evento.asunto
            informacionReclamacionMap.eventoURL = "${grailsApplication.config.grails.serverURL}/evento/obtener?id=${evento.id}"
            informacionReclamacionMap.firmas = []
            firmas.collect { firma ->
                def firmaMap = [id:firma.id, fechaCreacion:firma.dateCreated,
                usuario:firma.usuario.nif,
                firmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtener?id=${firma.mensajeSMIME.id}",
                reciboFirmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/mensajeSMIME/obtenerReciboReclamacion?id=${firma.mensajeSMIME?.id}"]
                def valoresCampos = ValorCampoDeEvento.findAllWhere(firma:firma)
                firmaMap.campos = []
                valoresCampos.collect { valorCampo ->
                    firmaMap.campos.add([campo:valorCampo.campoDeEvento.contenido, valor:valorCampo.valor])
                }
                informacionReclamacionMap.firmas.add(firmaMap)
            }
            response.status = 200
            response.setContentType("application/json")
            render informacionReclamacionMap as JSON
        }
        response.status = 400
        render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false  
    }
  
  
   def guardarCancelacion= {
	   SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq;
	   if(params.smimeMessageReq) {
		   Respuesta respuesta = eventoService.cancelarEvento(
			   params.smimeMessageReq, request.getLocale());
		   response.status = respuesta.codigoEstado;
		   log.debug("statuscode: ${respuesta.codigoEstado} - mensaje: ${respuesta.mensaje}")
		   render respuesta.mensaje;
		   return false
	   }
	   response.status = 400;
	   render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"]);
	   return false;
   }

   def comprobarFechas = {
	   Evento evento
	   if (params.long('id')) {
		   Evento.withTransaction {
			   evento = Evento.get(params.id)
		   }
	   }
	   if(!evento) {
		   response.status = 404 //Not Found
		   render message(code: 'evento.eventoNotFound', args:[params.id])
		   return
	   }
	   Respuesta respuesta = eventoService.comprobarFechasEvento(evento, request.getLocale())
	   response.status = respuesta.codigoEstado
	   render respuesta?.evento?.estado?.toString()
	   return false
   }
	   
}