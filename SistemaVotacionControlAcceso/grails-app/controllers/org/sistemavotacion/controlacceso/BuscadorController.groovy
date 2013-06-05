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
import org.sistemavotacion.busqueda.SearchHelper;
import grails.converters.JSON;
import java.lang.RuntimeException;
import org.hibernate.search.Search;
import org.sistemavotacion.util.*;
import org.apache.lucene.search.Sort;

/**
 * @infoController Búsquedas
 * @descController Servicios de búsqueda sobre los datos generados por la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class BuscadorController {

   SearchHelper searchHelper;
   def sessionFactory
   def eventoService   
   def grailsApplication

   
   /**
    * ==================================================
	* (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
	* ==================================================
	* Servicio que reindexa el motor de búsqueda
	* @httpMethod [GET]
	*/
   def reindexTest () {
	   log.debug "==== Usuario en la lista de administradores, reindexando"
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
			List<String> administradores = Arrays.asList(
				grailsApplication.config.SistemaVotacion.adminsDNI.split(","))
			Usuario usuario = mensajeSMIME.getUsuario()
			if (administradores.contains(usuario.nif)) {
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
	 * Servicio que busca la cadena de texto recibida entre los eventos publicados.
	 *
	 * @httpMethod [GET]
	 * @param [consultaTexto] Opcional. Texto de la búsqueda.
	 * @param [max] Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @responseContentType [application/json]
	 */
    def evento () {
        def eventosMap = new HashMap()
		if (params.consultaTexto) {
	        List<Evento> eventos = searchHelper.findByFullText(Evento.class,
	            ['asunto', 'contenido']  as String[], params.consultaTexto, params.offset, params.max);
	        log.debug("eventos: ${eventos.size()}")
			eventosMap.eventos = eventos.collect {evento ->
	                return [id: evento.id, asunto: evento.asunto, contenido:evento.contenido,
						URL:"${grailsApplication.config.grails.serverURL}/evento/${evento.id}"]
	        }
		}
        render eventosMap as JSON
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
		log.debug("consulta: ${consulta} - offset:${params.offset} - max: ${params.max}")
		if (!consulta) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.PeticionIncorrectaHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
			return false
		}
		def mensajeJSON = JSON.parse(consulta)
		Tipo tipoConsultaEVento = Tipo.valueOf(mensajeJSON.tipo)
		Class<?> entityClass
		if(tipoConsultaEVento == Tipo.EVENTO_VOTACION) entityClass = EventoVotacion.class
		if(tipoConsultaEVento == Tipo.EVENTO_FIRMA) entityClass = EventoFirma.class
		if(tipoConsultaEVento == Tipo.EVENTO_RECLAMACION) entityClass = EventoReclamacion.class
		if(!entityClass) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.PeticionIncorrectaHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
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
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR
			render ex.getMessage()
			return false
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
	
	/**
	 * Servicio que busca los eventos que tienen la etiqueta que se
	 * pasa como parámetro.
	 * @param etiqueta Obligatorio. Texto de la etiqueta.
	 * @param max Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 */
	def eventoPorEtiqueta () {
		def eventosMap = new HashMap()
		if (!params.etiqueta) {
			render message(code: 'busqueda.faltaParametroEtiqueta')
			return
		}
		def etiqueta = Etiqueta.findByNombre(params.etiqueta)
		if (etiqueta) {
			eventosMap.eventos = EtiquetaEvento.findAllByEtiqueta(etiqueta,
				[max: params.max, offset: params.offset]).collect { etiquetaEvento ->
				return [id: etiquetaEvento.evento.id, 
					URL:"${grailsApplication.config.grails.serverURL}/evento/${etiquetaEvento.evento.id}",
					asunto:etiquetaEvento?.evento?.asunto, 
					contenido:etiquetaEvento?.evento?.contenido]
			} 
		}
		render eventosMap as JSON
	}

}
