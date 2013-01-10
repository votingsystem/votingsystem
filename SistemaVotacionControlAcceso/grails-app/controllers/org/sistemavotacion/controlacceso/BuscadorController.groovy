package org.sistemavotacion.controlacceso;

import java.util.Date;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField
import org.sistemavotacion.busqueda.SearchHelper
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.hibernate.search.FullTextQuery
import org.hibernate.search.FullTextSession
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.smime.SMIMEMessageWrapper
import org.sistemavotacion.busqueda.SearchHelper;
import grails.converters.JSON;
import java.lang.RuntimeException;
import org.hibernate.search.Search;
import org.sistemavotacion.util.*;
import org.apache.lucene.search.Sort;

class BuscadorController {

   SearchHelper searchHelper;
   def sessionFactory
   def subscripcionService
   def eventoService
   
   def grailsApplication

   def index() { }
   
   
   def reindex = {
	   log.debug "Usuario en la lista de administradores, reindexando"
	   FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
	   fullTextSession.createIndexer().startAndWait()
	   response.status = 200
	   render "Reindexación OK"
	   return false
   }
   
	def guardarReindex = { 
		SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
		List<String> administradores = Arrays.asList(
			grailsApplication.config.SistemaVotacion.adminsDNI.split(","))
		Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(
			params.smimeMessageReq, request.getLocale())
		if(200 != respuestaUsuario.codigoEstado) {
			response.status = respuestaUsuario.codigoEstado
			render respuestaUsuario.mensaje
			return false
		}
		Usuario usuario = respuestaUsuario.usuario
		if (administradores.contains(usuario.nif)) {
			log.debug "Usuario en la lista de administradores, reindexando"
			FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
			fullTextSession.createIndexer().startAndWait()
			response.status = 200
			render "Reindexación OK"
		} else {
			log.debug "Usuario no esta en la lista de administradoreas, petición denegada"
			respuesta = new Respuesta(codigoEstado:400,
				mensaje:message(code: 'error.UsuarioNoAdministrador'), tipo: Tipo.ERROR)
			response.status = 400
			render "ERROR"
		}
		return false
	}
	
    def evento = {
        def eventosMap = new HashMap()
		if (!params.consultaTexto) {
			render message(code: 'busqueda.faltaParametroConsultaTexto')
			return
		}
        List<Evento> eventos = searchHelper.findByFullText(Evento.class,
            ['asunto', 'contenido']  as String[], params.consultaTexto, params.offset, params.max);
        log.debug("eventos: ${eventos.size()}")
		eventosMap.eventos = eventos.collect {evento ->
                return [id: evento.id, asunto: evento.asunto, contenido:evento.contenido,
					URL:"${grailsApplication.config.grails.serverURL}/evento?id=${evento.id}"]
        }
        render eventosMap as JSON
    }

	
	def consultaJSON() {
		String consulta = StringUtils.getStringFromInputStream(request.getInputStream())
		log.debug("consulta: ${consulta} - offset:${params.offset} - max: ${params.max}")
		if (!consulta) {
			render(view:"index")
			return false
		}
		def mensajeJSON = JSON.parse(consulta)
		Tipo tipoConsultaEVento = Tipo.valueOf(mensajeJSON.tipo)
		Class<?> entityClass
		if(tipoConsultaEVento == Tipo.EVENTO_VOTACION) entityClass = EventoVotacion.class
		if(tipoConsultaEVento == Tipo.EVENTO_FIRMA) entityClass = EventoFirma.class
		if(tipoConsultaEVento == Tipo.EVENTO_RECLAMACION) entityClass = EventoReclamacion.class
		if(!entityClass) {
			response.status = 400
			render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
			return false
		}
		def eventosMap = new HashMap()
		eventosMap.eventos = new HashMap()
		eventosMap.eventos.firmas = []
		eventosMap.eventos.votaciones = []
		eventosMap.eventos.reclamaciones = []
		int numeroEventosEnPeticion = 0;
		int numeroTotalEventosVotacionEnSistema = 0;
		int numeroTotalEventosFirmaEnSistema = 0;
		int numeroTotalEventosReclamacionEnSistema = 0;

		Date fechaInicioDesde
		Date fechaInicioHasta
		Date fechaFinDesde
		Date fechaFinHasta
		List<Evento.Estado> estadosEvento
		try{
			if(mensajeJSON.fechaInicioDesde)
				fechaInicioDesde = DateUtils.getDateFromString(mensajeJSON.fechaInicioDesde)
			if(mensajeJSON.fechaInicioHasta)
				fechaInicioHasta = DateUtils.getDateFromString(mensajeJSON.fechaInicioHasta)
			if(mensajeJSON.fechaFinDesde)
				fechaFinDesde = DateUtils.getDateFromString(mensajeJSON.fechaFinDesde)
			if(mensajeJSON.fechaFinHasta)
				fechaFinHasta = DateUtils.getDateFromString(mensajeJSON.fechaFinHasta)
			if(mensajeJSON.estadoEvento) {
				estadosEvento = new ArrayList<Evento.Estado>();
				Evento.Estado estadoEvento = Evento.Estado.valueOf(mensajeJSON.estadoEvento)
				estadosEvento.add(estadoEvento);
				if(Evento.Estado.FINALIZADO == estadoEvento) estadosEvento.add(Evento.Estado.CANCELADO)
			}
		} catch(Exception ex){
			log.error(ex.getMessage(), ex)
		}
		FullTextQuery fullTextQuery =  searchHelper.getCombinedQuery(entityClass,
				['asunto', 'contenido']  as String[], mensajeJSON.textQuery?.toString(),
				fechaInicioDesde, fechaInicioHasta, fechaFinDesde, fechaFinHasta, estadosEvento)
		if(fullTextQuery) {
			switch(tipoConsultaEVento) {
				case Tipo.EVENTO_VOTACION:	
					numeroTotalEventosVotacionEnSistema = fullTextQuery?.getResultSize()
					List<EventoVotacion> eventosVotacion
					EventoVotacion.withTransaction {
						fullTextQuery.setSort(new Sort(new SortField("id", SortField.LONG)));
						eventosVotacion = fullTextQuery.setFirstResult(params.int('offset')).
							setMaxResults(params.int('max')).list();
						eventosVotacion.collect {eventoItem ->
							eventosMap.eventos.votaciones.add(eventoService.optenerEventoVotacionJSONMap(eventoItem))
						}
						if(eventosVotacion) numeroEventosEnPeticion = eventosVotacion.size()
						eventosMap.numeroEventosVotacionEnPeticion = numeroEventosEnPeticion
					}
					break;
				case Tipo.EVENTO_FIRMA:
					numeroTotalEventosFirmaEnSistema = fullTextQuery?.getResultSize()
					List<EventoFirma> eventosFirma
					EventoFirma.withTransaction {
						fullTextQuery.setSort(new Sort(new SortField("id", SortField.LONG)));
						eventosFirma = fullTextQuery.setFirstResult(params.int('offset')).
							setMaxResults(params.int('max')).list();
						eventosFirma.collect {eventoItem ->
							eventosMap.eventos.firmas.add(eventoService.optenerEventoFirmaJSONMap(eventoItem))
						}
						if(eventosFirma) numeroEventosEnPeticion = eventosFirma.size()
						eventosMap.numeroEventosFirmaEnPeticion = numeroEventosEnPeticion
					}
					break;
				case Tipo.EVENTO_RECLAMACION:
					numeroTotalEventosReclamacionEnSistema = fullTextQuery?.getResultSize()
					List<EventoReclamacion> eventosReclamacion
					EventoReclamacion.withTransaction {
						fullTextQuery.setSort(new Sort(new SortField("id", SortField.LONG)));
						eventosReclamacion = fullTextQuery.setFirstResult(params.int('offset')).
							setMaxResults(params.int('max')).list();
						eventosReclamacion.collect {eventoItem ->
							eventosMap.eventos.reclamaciones.add(eventoService.optenerEventoReclamacionJSONMap(eventoItem))
						}
						if(eventosReclamacion) numeroEventosEnPeticion = eventosReclamacion.size()
						eventosMap.numeroEventosReclamacionEnPeticion = numeroEventosEnPeticion
					}
					break;
			}
		}
		eventosMap.numeroTotalEventosVotacionEnSistema = numeroTotalEventosVotacionEnSistema
		eventosMap.numeroTotalEventosFirmaEnSistema = numeroTotalEventosFirmaEnSistema
		eventosMap.numeroTotalEventosReclamacionEnSistema = numeroTotalEventosReclamacionEnSistema
		eventosMap.offset = params.int('offset')
		response.setContentType("application/json")	
		render eventosMap as JSON
		return false
	}
	
	def eventoPorEtiqueta = {
		def eventosMap = new HashMap()
		if (!params.etiqueta) {
			render message(code: 'busqueda.faltaParametroEtiqueta')
			return
		}
		def etiqueta = Etiqueta.findByNombre(params.etiqueta)
		if (etiqueta) {
			eventosMap.eventos = EtiquetaEvento.findAllByEtiqueta(etiqueta).collect { etiquetaEvento ->
				return [id: etiquetaEvento.evento.id, 
					URL:"${grailsApplication.config.grails.serverURL}/evento?id=${etiquetaEvento.evento.id}",
					asunto:etiquetaEvento?.evento?.asunto, 
					contenido:etiquetaEvento?.evento?.contenido]
			} 
		}
		render eventosMap as JSON
	}

}
