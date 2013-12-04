package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.hibernate.search.FullTextQuery
import org.hibernate.search.FullTextSession
import org.hibernate.search.Search
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVSClaim
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.EventVSManifest
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.SubSystemVS
import org.votingsystem.model.TagVS
import org.votingsystem.model.TagVSEventVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.search.SearchHelper
import org.votingsystem.util.DateUtils
/**
 * @infoController Búsquedas
 * @descController Servicios de búsqueda sobre los datos generados por la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class SearchController {

   SearchHelper searchHelper;
   def sessionFactory
   def eventVSService
    def grailsApplication
   def userVSService

   
   /**
    * ==================================================\n
	* (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
	* ==================================================\n
	* Servicio que reindexa el motor de búsqueda
	* @httpMethod [GET]
	*/
   def reindexTest () {
	   if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
		   def msg = message(code: "serviceDevelopmentModeMsg")
		   log.error msg
		   response.status = ResponseVS.SC_ERROR_REQUEST
		   render msg
		   return false
	   }
	   log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
	   FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
	   fullTextSession.createIndexer().startAndWait()
	   response.status = ResponseVS.SC_OK
	   render "OK"
	   return false
   }
   
   /**
	* Servicio que reindexa los datos del motor de búsqueda.
	* 
	* @httpMethod [POST]
	* @requestContentType [application/x-pkcs7-signature] Obligatorio. PDFDocumentVS firmado
	*             en formato SMIME con los datos de la solicitud de reindexación.
	*/
	def reindex () { 
		try {
			MessageSMIME messageSMIME = params.messageSMIMEReq
			if(!messageSMIME) {
				String msg = message(code:'requestWithoutFile')
				log.error msg
				response.status = ResponseVS.SC_ERROR_REQUEST
				render msg
				return false
			}
			def requestJSON = JSON.parse(messageSMIME.getSmimeMessage().getSignedContent())
			TypeVS operacion = TypeVS.valueOf(requestJSON.operacion)
			String msg
			if(TypeVS.INDEX_REQUEST != operacion) {
				msg = message(code:'operationErrorMsg',
					args:[operacion.toString()])
				response.status = ResponseVS.SC_ERROR_REQUEST
				log.debug("ERROR - ${msg}")
				render msg
				return false
			}
			UserVS userVS = messageSMIME.getUserVS()
			if (userVSService.isUserAdmin(userVS.nif)) {
				log.debug "UserVS en la lista de administradores, reindexando"
				FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
				fullTextSession.createIndexer().startAndWait()
				params.responseVS = new ResponseVS(type:TypeVS.INDEX_REQUEST,
					statusCode:ResponseVS.SC_ERROR_REQUEST)
				response.status = ResponseVS.SC_OK
				render "OK"
			} else {
				log.debug "UserVS no esta en la lista de administradores, petición denegada"
				msg = message(code: 'adminIdentificationErrorMsg', [userVS.nif])
				params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					type:TypeVS.INDEX_REQUEST_ERROR, message:msg)
				response.status = ResponseVS.SC_ERROR_REQUEST
				render msg
			}
			return false
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				type:TypeVS.INDEX_REQUEST_ERROR, message:ex.getMessage())
			response.status = ResponseVS.SC_ERROR_REQUEST
			render ex.getMessage()
			return false
		}
	}
	
	/**
	 * Servicio que busca la cadena de texto recibida entre los eventsVS publicados.
	 *
	 * @httpMethod [GET]
	 * @param [consultaTexto] Opcional. Texto de la búsqueda.
	 * @param [max] Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @responseContentType [application/json]
	 */
    def eventVS () {
        def eventsVSMap = new HashMap()
		if (params.consultaTexto) {
	        List<EventVS> eventsVS = searchHelper.findByFullText(EventVS.class,
	            ['subject', 'content']  as String[], params.consultaTexto, params.offset, params.max);
	        log.debug("eventsVS: ${eventsVS.size()}")
			eventsVSMap.eventsVS = eventsVS.collect {eventVS ->
	                return [id: eventVS.id, subject: eventVS.subject, content:eventVS.content,
						URL:"${grailsApplication.config.grails.serverURL}/eventVS/${eventVS.id}"]
	        }
		}
        render eventsVSMap as JSON
		return false
    }

	/**
	 * @httpMethod [POST]
	 * @requestContentType [application/json] PDFDocumentVS JSON con los parámetros de la consulta:<br/><code>
	 * 		  {conReclamaciones:true, conVotaciones:true, textQuery:ipsum, conManifiestos:true}</code>
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con la lista de eventos que cumplen el criterio de la búsqueda.
	 */
	def find() {
		String consulta = "${request.getInputStream()}"
		log.debug("consulta: ${consulta} - offset:${params.offset} - max: ${params.max}")
		if (!consulta) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'requestWithErrorsHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
			return false
		}
		def messageJSON = JSON.parse(consulta)
		SubSystemVS targetSubsystem = SubSystemVS.valueOf(messageJSON.subsystem)
		Class<?> entityClass
		if(targetSubsystem == SubSystemVS.VOTES) entityClass = EventVSElection.class
		if(targetSubsystem == SubSystemVS.MANIFESTS) entityClass = EventVSManifest.class
		if(targetSubsystem == SubSystemVS.CLAIMS) entityClass = EventVSClaim.class
		if(!entityClass) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'requestWithErrorsHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
			return false
		}
		def eventsVSMap = new HashMap()
		eventsVSMap.eventsVS = new HashMap()
		eventsVSMap.eventsVS.manifests = []
		eventsVSMap.eventsVS.elections = []
		eventsVSMap.eventsVS.claims = []
		int numEventVSInRequest = 0;
		int numEventsVSElectionInSystem = 0;
		int numEventsVSManifestInSystem = 0;
		int numEventsVSClaimInSystem = 0;

		Date dateBeginFrom
		Date dateBeginTo
		Date dateFinishFrom
		Date dateFinishTo
		List<EventVS.State> eventVSStates
		try{
			if(messageJSON.dateBeginFrom)
				dateBeginFrom = DateUtils.getDateFromString(messageJSON.dateBeginFrom)
			if(messageJSON.dateBeginTo)
				dateBeginTo = DateUtils.getDateFromString(messageJSON.dateBeginTo)
			if(messageJSON.dateFinishFrom)
				dateFinishFrom = DateUtils.getDateFromString(messageJSON.dateFinishFrom)
			if(messageJSON.dateFinishTo)
				dateFinishTo = DateUtils.getDateFromString(messageJSON.dateFinishTo)
			if(messageJSON.eventState &&  !"".equals(messageJSON.eventState.trim())) {
				eventVSStates = new ArrayList<EventVS.State>();
				EventVS.State eventVSState = EventVS.State.valueOf(messageJSON.eventState)
				eventVSStates.add(eventVSState);
				if(EventVS.State.TERMINATED == eventVSState) eventVSStates.add(EventVS.State.CANCELLED)
			}
		} catch(Exception ex){
			log.error (ex.getMessage(), ex)
			response.status = ResponseVS.SC_ERROR
			render ex.getMessage()
			return false
		}
		FullTextQuery fullTextQuery =  searchHelper.getCombinedQuery(entityClass,
				['subject', 'content']  as String[], messageJSON.textQuery?.toString(),
				dateBeginFrom, dateBeginTo, dateFinishFrom, dateFinishTo, eventVSStates)
		if(fullTextQuery) {
			switch(targetSubsystem) {
				case SubSystemVS.VOTES:
					numEventsVSElectionInSystem = fullTextQuery?.getResultSize()
					List<EventVSElection> eventsVSElection
					EventVSElection.withTransaction {
						fullTextQuery.setSort(new Sort(new SortField("id", SortField.LONG)));
						eventsVSElection = fullTextQuery.setFirstResult(params.int('offset')).
							setMaxResults(params.int('max')).list();
						eventsVSElection.each {eventVSItem ->
							eventsVSMap.eventsVS.elections.add(eventVSService.getEventVSElectionMap(eventVSItem))
						}
						if(eventsVSElection) numEventVSInRequest = eventsVSElection.size()
						eventsVSMap.numEventsVSElection = numEventVSInRequest
					}
					break;
				case SubSystemVS.MANIFESTS:
					numEventsVSManifestInSystem = fullTextQuery?.getResultSize()
					List<EventVSManifest> eventsVSManifest
					EventVSManifest.withTransaction {
						fullTextQuery.setSort(new Sort(new SortField("id", SortField.LONG)));
						eventsVSManifest = fullTextQuery.setFirstResult(params.int('offset')).
							setMaxResults(params.int('max')).list();
						eventsVSManifest.each {eventVSItem ->
							eventsVSMap.eventsVS.manifests.add(eventVSService.getEventVSManifestMap(eventVSItem))
						}
						if(eventsVSManifest) numEventVSInRequest = eventsVSManifest.size()
						eventsVSMap.numEventsVSManifest = numEventVSInRequest
					}
					break;
				case SubSystemVS.CLAIMS:
					numEventsVSClaimInSystem = fullTextQuery?.getResultSize()
					List<EventVSClaim> eventsVSClaim
					EventVSClaim.withTransaction {
						fullTextQuery.setSort(new Sort(new SortField("id", SortField.LONG)));
						eventsVSClaim = fullTextQuery.setFirstResult(params.int('offset')).
							setMaxResults(params.int('max')).list();
						eventsVSClaim.each {eventVSItem ->
							eventsVSMap.eventsVS.claims.add(eventVSService.getEventVSClaimMap(eventVSItem))
						}
						if(eventsVSClaim) numEventVSInRequest = eventsVSClaim.size()
						eventsVSMap.numEventsVSClaim = numEventVSInRequest
					}
					break;
			}
		}
		eventsVSMap.numEventsVSElectionInSystem = numEventsVSElectionInSystem
		eventsVSMap.numEventsVSManifestInSystem = numEventsVSManifestInSystem
		eventsVSMap.numEventsVSClaimInSystem = numEventsVSClaimInSystem
		eventsVSMap.offset = params.int('offset')
		response.setContentType(ContentTypeVS.JSON)
		render eventsVSMap as JSON
		return false
	}
	
	/**
	 * Servicio que busca los eventos que tienen la tagVS que se
	 * pasa como parámetro.
	 * @param tagVS Obligatorio. Texto de la tagVS.
	 * @param max Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 */
	def eventvsByTag () {
		def eventsVSMap = new HashMap()
		if (!params.tag) {
			render message(code: 'searchMissingParamTag')
			return
		}
		def tag = TagVS.findByName(params.tag)
		if (tag) {
			eventsVSMap.eventsVS = TagVSEventVS.findAllByEtiqueta(tag,
				[max: params.max, offset: params.offset]).collect { eventVSTag ->
				return [id: eventVSTag.eventVS.id,
					URL:"${grailsApplication.config.grails.serverURL}/eventVS/${eventVSTag.eventVS.id}",
					subject:eventVSTag?.eventVS?.subject, content:eventVSTag?.eventVS?.content]
			} 
		}
		render eventsVSMap as JSON
	}

}
