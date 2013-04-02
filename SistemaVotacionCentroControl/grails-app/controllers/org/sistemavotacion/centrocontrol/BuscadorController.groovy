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

/**
 * @infoController Búsquedas
 * @descController Servicios de búsqueda sobre los datos generados por la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 * */
class BuscadorController {

   SearchHelper searchHelper;
   def subscripcionService
   def sessionFactory
   
   /**
	* @httpMethod GET
	* @return Información sobre los servicios que tienen como url base '/buscador'
	*/
	def index() { 
		redirect action: "restDoc"
	}
   
   /**
    * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
    * Servicio que reindexa el motor de búsqueda
	* @httpMethod GET
	*/
   def reindex () {
		log.debug "Usuario en la lista de administradoreas, reindexando"
		FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
		fullTextSession.createIndexer().startAndWait()
		response.status = Respuesta.SC_OK
		render message(code: 'reindexOKMsg')
		return false
	}
   
   /**
	* Servicio que reindexa los datos del motor de búsqueda
	* @param archivoFirmado Obligatorio. Solicitud firmada por un administrador de sistema.
	* @httpMethod POST
	*/
	def guardarReindex () { 
		SMIMEMessageWrapper smimeMessageReq = params.smimeMessageReq
		List<String> administradores = Arrays.asList(
			grailsApplication.config.SistemaVotacion.adminsDNI.split(","))
		Usuario usuario
		Respuesta comprobacionUsuario = subscripcionService.comprobarUsuario(
			params.smimeMessageReq?.getFirmante(), request.getLocale())
		if(Respuesta.SC_OK== comprobacionUsuario.codigoEstado) usuario = comprobacionUsuario.usuario
		if (administradores.contains(usuario?.nif)) {
			log.debug "Usuario en la lista de administradoreas, reindexando"
			FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
			fullTextSession.createIndexer().startAndWait()
			response.status = Respuesta.SC_OK
			render message(code: 'reindexOKMsg') 
		} else {
			log.debug message(code: 'error.UsuarioNoAdministrador')
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.UsuarioNoAdministrador')
		}
		return false
	}
	
	/**
	 * Servicio que busca la cadena de texto recibida entre las votaciones publicadas.
	 * 
	 * @param consultaTexto Obligatorio. Texto de la búsqueda.
	 * @param max Opcional (por defecto 20). Número máximo de documentos que 
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @httpMethod GET
	 */
    def evento () {
        def eventosVotacionMap = new HashMap()
		if (!params.consultaTexto) {
			render(view: "index")
			return
		}
        List<EventoVotacion> eventos = searchHelper.findByFullText(EventoVotacion.class,
            ['asunto', 'contenido']  as String[], params.consultaTexto, params.offset, params.max);
        log.debug("eventos votacion: ${eventos.size()}")
		eventosVotacionMap.eventos = eventos.collect {evento ->
                return [id: evento.id, asunto: evento.asunto, 
					contenido:evento.contenido,
					URL:"${grailsApplication.config.grails.serverURL}/eventoVotacion?id=${evento.id}"]
        }
        render eventosVotacionMap as JSON
    }
	
	/**
	 * @httpMethod POST
	 * @param consulta Obligatorio. Documento JSON con los parámetros de la consulta:<br/><code>
	 * 		  {conReclamaciones:true, conVotaciones:true, textQuery:ipsum, conManifiestos:true}</code>
	 * @return Documento JSON con la lista de votaciones que cumplen el criterio de la búsqueda.
	 */
	def consultaJSON() {
		//String consulta = StringUtils.getStringFromInputStream(request.getInputStream())
		String consulta = params.jsonRequest
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
		if (Tipo.EVENTO_VOTACION == Tipo.valueOf(mensajeJSON.tipo)) {
			Date fechaInicioDesde
			Date fechaInicioHasta
			Date fechaFinDesde
			Date fechaFinHasta
			List<EventoVotacion.Estado> estadosEvento
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
					estadosEvento = new ArrayList<EventoVotacion.Estado>();
					EventoVotacion.Estado estadoEvento = EventoVotacion.Estado.valueOf(mensajeJSON.estadoEvento)
					estadosEvento.add(estadoEvento);
					if(EventoVotacion.Estado.FINALIZADO == estadoEvento) estadosEvento.add(EventoVotacion.Estado.CANCELADO)
				}
			} catch(Exception ex){
				log.error(ex.getMessage(), ex)
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
		eventosVotacion.collect {eventoItem ->
			def eventoItemId = eventoItem.id
			def eventoMap = [id: eventoItem.id, fechaCreacion: eventoItem.dateCreated,
					URL:"${grailsApplication.config.grails.serverURL}/evento?id=${eventoItem.id}",
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
			centroControlMap.informacionVotosURL = "${grailsApplication.config.grails.serverURL}/eventoVotacion/obtenerVotos?eventoVotacionId=${eventoItem.eventoVotacionId}&controlAccesoServerURL=${eventoItem.controlAcceso.serverURL}"
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
	 * @httpMethod GET
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
					URL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/obtener?id=${etiquetaEvento.eventoVotacion.id}",
					asunto:etiquetaEvento.eventoVotacion.asunto, 
					contenido: etiquetaEvento?.eventoVotacion?.contenido]
			} 
		}
		render eventosMap as JSON
	}

}
