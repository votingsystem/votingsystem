package org.sistemavotacion.centrocontrol;

import java.util.Date;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField
import org.sistemavotacion.busqueda.SearchHelper
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.hibernate.search.FullTextQuery
import org.hibernate.search.FullTextSession
import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.smime.SMIMEMessageWrapper
import org.sistemavotacion.busqueda.SearchHelper;
import grails.converters.JSON;
import java.lang.RuntimeException;
import java.util.Date;
import org.hibernate.search.Search;
import org.sistemavotacion.util.*;
import org.apache.lucene.search.Sort;
import org.sistemavotacion.utils.*

/**
 * @infoController Búsquedas
 * @descController Servicios de búsqueda sobre los datos generados por la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class BuscadorController {

   SearchHelper searchHelper;
   def subscripcionService
   def sessionFactory
   def usuarioService
   
   /**
    * ==================================================
	* (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
	* ==================================================
	* Servicio que reindexa el motor de búsqueda
	* @httpMethod [GET]
	*/
   def reindexTest () {
	   if(!VotingSystemApplicationContex.Environment.DEVELOPMENT.equals(
		   VotingSystemApplicationContex.instance.environment)) {
		   def msg = message(code: "serviceDevelopmentModeMsg")
		   log.error msg
		   response.status = Respuesta.SC_ERROR_PETICION
		   render msg
		   return false
	   }
	   log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
	   FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
	   fullTextSession.createIndexer().startAndWait()
	   response.status = Respuesta.SC_OK
	   render "OK"
	   return false
   }
   
   /**
	* Servicio que reindexa los datos del motor de búsqueda.
	* 
	* @httpMethod [POST]
	* @requestContentType [application/x-pkcs7-signature] Obligatorio. Documento firmado 
	*             en formato SMIME con los datos de la solicitud de reindexación.
	*/
	def reindex () { 
		try {
			MensajeSMIME mensajeSMIME = params.mensajeSMIMEReq
			if(!mensajeSMIME) {
				String msg = message(code:'evento.peticionSinArchivo')
				log.error msg
				response.status = Respuesta.SC_ERROR_PETICION
				render msg
				return false
			}
			def requestJSON = JSON.parse(mensajeSMIME.getSmimeMessage().getSignedContent())
			Usuario usuario = mensajeSMIME.getUsuario()
			Tipo operacion = Tipo.valueOf(requestJSON.operacion)
			String msg
			if(Tipo.SOLICITUD_INDEXACION != operacion) {
				msg = message(code:'operationErrorMsg',
					args:[operacion.toString()])
				response.status = Respuesta.SC_ERROR_PETICION
				log.debug("ERROR - ${msg}")
				render msg
				return false
			}
			if(usuarioService.isUserAdmin(usuario.nif)) {
				log.debug "Usuario en la lista de administradores, reindexando"
				FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
				fullTextSession.createIndexer().startAndWait()
				params.respuesta = new Respuesta(tipo:Tipo.SOLICITUD_INDEXACION,
					codigoEstado:Respuesta.Respuesta.SC_ERROR_PETICION)
				response.status = Respuesta.SC_OK
				render "OK"
			} else {
				log.debug "Usuario no esta en la lista de administradores, petición denegada"
				msg = message(code: 'adminIdentificationErrorMsg', [usuario.nif])
				params.respuesta = new Respuesta(codigoEstado:Respuesta.Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.SOLICITUD_INDEXACION_ERROR, mensaje:msg)
				response.status = Respuesta.SC_ERROR_PETICION
				render msg
			}
			return false
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			params.respuesta = new Respuesta(codigoEstado:Respuesta.Respuesta.SC_ERROR_PETICION,
				tipo:Tipo.SOLICITUD_INDEXACION_ERROR, mensaje:ex.getMessage())
			response.status = Respuesta.SC_ERROR_PETICION
			render ex.getMessage()
			return false
		}
	}
	
	/**
	 * Servicio que busca la cadena de texto recibida entre las votaciones publicadas.
	 *
	 * @httpMethod [GET]
	 * @param [consultaTexto] Opcional. Texto de la búsqueda.
	 * @param [max] Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @responseContentType [application/json]
	 */
    def evento () {
        def eventosVotacionMap = new HashMap()
		if (params.consultaTexto) {
			List<EventoVotacion> eventos = searchHelper.findByFullText(EventoVotacion.class,
				['asunto', 'contenido']  as String[], params.consultaTexto, params.offset, params.max);
			log.debug("eventos votacion: ${eventos.size()}")
			eventosVotacionMap.eventos = eventos.collect {evento ->
					return [id: evento.id, asunto: evento.asunto,
						contenido:evento.contenido,
						URL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/${evento.id}"]
			}
        }
        render eventosVotacionMap as JSON
		return false
    }
	
	/**
	 * @httpMethod [POST]
	 * @requestContentType [application/json] Documento JSON con los parámetros de la consulta:<br/><code>
	 * 		  {conReclamaciones:true, conVotaciones:true, textQuery:ipsum, conManifiestos:true}</code>
	 * @responseContentType [application/json]
	 * @return Documento JSON con la lista de eventos que cumplen el criterio de la búsqueda.
	 */
	def consultaJSON() {
		String consulta = "${request.getInputStream()}"
		log.debug("consulta: ${consulta}")
		if (!consulta) {
			render(view:"index")
			return false
		}
		def mensajeJSON = JSON.parse(consulta)
		def eventosMap = new HashMap()
		eventosMap.eventos = new HashMap()
		eventosMap.eventos.votaciones = []
		int numeroEventosEnPeticion = 0;
		int numeroTotalEventosVotacionEnSistema = 0;
		List<EventoVotacion> eventosVotacion
		if (Subsystem.VOTES == Subsystem.valueOf(mensajeJSON.subsystem)) {
			Date fechaInicioDesde
			Date fechaInicioHasta
			Date fechaFinDesde
			Date fechaFinHasta
			List<EventoVotacion.Estado> estadosEvento
			try{
				if(mensajeJSON.dateBeginFrom)
					fechaInicioDesde = DateUtils.getDateFromString(mensajeJSON.dateBeginFrom)
				if(mensajeJSON.dateBeginTo)
					fechaInicioHasta = DateUtils.getDateFromString(mensajeJSON.dateBeginTo)
				if(mensajeJSON.dateFinishFrom)
					fechaFinDesde = DateUtils.getDateFromString(mensajeJSON.dateFinishFrom)
				if(mensajeJSON.dateFinishTo)
					fechaFinHasta = DateUtils.getDateFromString(mensajeJSON.dateFinishTo)
				if(mensajeJSON.eventState &&  !"".equals(mensajeJSON.eventState.trim())) {
					estadosEvento = new ArrayList<EventoVotacion.Estado>();
					EventoVotacion.Estado estadoEvento = Evento.Estado.valueOf(mensajeJSON.eventState)
					estadosEvento.add(estadoEvento);
					if(EventoVotacion.Estado.FINALIZADO == estadoEvento) estadosEvento.add(EventoVotacion.Estado.CANCELADO)
				}
			} catch(Exception ex){
				log.error (ex.getMessage(), ex)
				response.status = Respuesta.SC_ERROR
				render ex.getMessage()
				return false
			}
			FullTextQuery fullTextQuery =  searchHelper.getCombinedQuery(EventoVotacion.class,
				['asunto', 'contenido']  as String[], mensajeJSON.textQuery?.toString(),
				fechaInicioDesde, fechaInicioHasta, fechaFinDesde, fechaFinHasta, estadosEvento)
			if(fullTextQuery) {
				numeroTotalEventosVotacionEnSistema = fullTextQuery?.getResultSize()
				EventoVotacion.withTransaction {
					fullTextQuery.setSort(new Sort(new SortField("id", SortField.LONG)));
					eventosVotacion = fullTextQuery.setFirstResult(params.int('offset')).
						setMaxResults(params.int('max')).list();
					if(eventosVotacion) numeroEventosEnPeticion = eventosVotacion.size()
				}
			}
		}
		eventosMap.numeroEventosEnPeticion = numeroEventosEnPeticion
		eventosMap.numeroEventosVotacionEnPeticion = numeroEventosEnPeticion
		eventosMap.numeroTotalEventosVotacionEnSistema = numeroTotalEventosVotacionEnSistema
		eventosMap.offset = params.int('offset')
		eventosVotacion.each {eventoItem ->
			def eventoItemId = eventoItem.id
			def eventoMap = [id: eventoItem.id, fechaCreacion: eventoItem.dateCreated,
					URL:"${grailsApplication.config.grails.serverURL}/evento/${eventoItem.id}",
					asunto:eventoItem.asunto, contenido:eventoItem.contenido,
					etiquetas:eventoItem.etiquetaSet?.collect {etiquetaItem ->
							return [id:etiquetaItem.id, contenido:etiquetaItem.nombre]},
					duracion:DateUtils.getElapsedTime(eventoItem.getFechaInicio(),
					eventoItem.getFechaFin()),
					estado:eventoItem.estado.toString(),
					fechaInicio:eventoItem.getFechaInicio(),
					fechaFin:eventoItem.getFechaFin()]
			if (eventoItem.usuario)
				eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
			def controlAccesoMap = [serverURL:eventoItem.controlAcceso.serverURL,
							nombre:eventoItem.controlAcceso.nombre]
			eventoMap.controlAcceso = controlAccesoMap
			eventoMap.opciones = eventoItem.opciones?.collect {opcion ->
							return [id:opcion.id, contenido:opcion.contenido]}
			def centroControlMap = [serverURL:grailsApplication.config.grails.serverURL,
					nombre:grailsApplication.config.SistemaVotacion.serverName]
			centroControlMap.informacionVotosURL = "${grailsApplication.config.grails.serverURL}/eventoVotacion/votes?eventAccessControlURL=${eventoItem.url}"
			eventoMap.centroControl = centroControlMap
			eventoMap.url = eventoItem.url
			eventosMap.eventos.votaciones.add(eventoMap)
		}
		response.setContentType("application/json")
		render eventosMap as JSON
		return false
	}
	
	/**
	 * Servicio que busca los eventos que tienen la etiqueta que se
	 * pasa como parámetro.
	 * @param etiqueta Obligatorio. Texto de la etiqueta.
	 * @param max Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @responseContentType [application/json]
	 * @httpMethod [GET]
	 */
	def eventoPorEtiqueta () {
		def eventosMap = new HashMap()
		if (!params.etiqueta) {
			render(view:"index")
			return false	
		}
		def etiqueta = Etiqueta.findByNombre(params.etiqueta)
		if (etiqueta) {
			eventosMap.eventos = EtiquetaEvento.findAllByEtiqueta(etiqueta,
				 [max: params.max, offset: params.offset]).collect { etiquetaEvento ->
				return [id: etiquetaEvento.eventoVotacion.id, 
					URL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/${etiquetaEvento.eventoVotacion.id}",
					asunto:etiquetaEvento.eventoVotacion.asunto, 
					contenido: etiquetaEvento?.eventoVotacion?.contenido]
			} 
		}
		render eventosMap as JSON
	}

}
