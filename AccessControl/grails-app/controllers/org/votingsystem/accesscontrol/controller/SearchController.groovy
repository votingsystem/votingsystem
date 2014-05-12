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
import org.votingsystem.model.UserVS
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.search.SearchHelper
import org.votingsystem.util.DateUtils

import static org.votingsystem.model.UserVS.*

/**
 * @infoController Búsquedas mediante
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
   def representativeService
   
   /**
    * ==================================================\n
	* (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
	* ==================================================\n
	* Servicio que reindexa el motor de búsqueda
	* @httpMethod [GET]
	*/
   def reindexTest () {
	   if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
           return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
	   }
	   log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
	   FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
	   fullTextSession.createIndexer().startAndWait()
       return [responseVS : new ResponseVS(ResponseVS.SC_OK, "OK")]
   }
   
   /**
	* Servicio que reindexa los datos del motor de búsqueda.
	* 
	* @httpMethod [POST]
	* @requestContentType [application/x-pkcs7-signature] Obligatorio. documento firmado
	*             en formato SMIME con los datos de la solicitud de reindexación.
	*/
	def reindex () {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
        }
        def requestJSON = JSON.parse(messageSMIME.getSmimeMessage().getSignedContent())
        TypeVS operation = TypeVS.valueOf(requestJSON.operation)
        if(TypeVS.INDEX_REQUEST != operation) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code:'operationErrorMsg', args:[operation.toString()]))]
        } else {
            UserVS userVS = messageSMIME.getUserVS()
            if (userVSService.isUserAdmin(userVS.nif)) {
                FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
                fullTextSession.createIndexer().startAndWait()
                return [responseVS : new ResponseVS(type:TypeVS.INDEX_REQUEST, statusCode:ResponseVS.SC_OK)]
            } else {
                return [responseVS : new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                        type:TypeVS.INDEX_REQUEST_ERROR, message:message(code: 'adminIdentificationErrorMsg',
                        args:[userVS.nif]))]
            }
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
    def eventVS_HS () {
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
    }

    /**
     * Servicio que busca entre los representantes la cadena de texto solicitada.
     *
     * @httpMethod [GET]
     * @param [searchText] Opcional. Texto de la búsqueda.
     * @param [max] Opcional (por defecto 20). Número máximo de documentos que
     * 		  devuelve la consulta (tamaño de la página).
     * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
     * @responseContentType [application/json]
     */
    def representative () {
        List<UserVS> representativeList = null
        def representativeMap = [:]
        representativeMap.representatives = []
        UserVS.withTransaction {
            representativeList = UserVS.createCriteria().list(max: params.max, offset: params.offset,
                    sort:params.sort, order:params.order) {
                or {
                    ilike('firstName', "%${params.searchText}%")
                    ilike('lastName', "%${params.searchText}%")
                    ilike('description', "%${params.searchText}%")
                }
                and {
                    eq('type', UserVS.Type.REPRESENTATIVE)
                }
            }
            representativeMap.offset = params.long('offset')
            representativeMap.numTotalRepresentatives = representativeList.totalCount
            representativeMap.numRepresentatives = representativeList.totalCount
            representativeList.each {representative ->
                representativeMap.representatives.add(representativeService.getRepresentativeMap(representative))
            }
        }
        render representativeMap as JSON
    }


    /**
     * Servicio que busca entre los eventos publicados una cadena de texto en el rango de fechas solicitado.
     *
     * @httpMethod [GET]
     * @param [searchText] Opcional. Texto de la búsqueda.
     * @param [dateBeginFrom] Opcional. Fecha límite inferior de comienzo del evento.
     * @param [dateBeginTo] Opcional. Fecha límite superior de comienzo del evento.
     * @param [max] Opcional (por defecto 20). Número máximo de documentos que
     * 		  devuelve la consulta (tamaño de la página).
     * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
     * @responseContentType [application/json]
     */
    def eventVS () {
        List<EventVS> eventvsList = null
        def resultList = []
        int totalEventvs = 0;
        EventVS.withTransaction {
            Date dateBeginFrom = null
            Date dateBeginTo = null
            EventVS.Type eventvsType = null
            EventVS.State eventVSState
            try {eventVSState = EventVS.State.valueOf(params.eventVSState)} catch(Exception ex) {}
            if(params.dateBeginFrom) try {dateBeginFrom = DateUtils.getDateFromString(params.dateBeginFrom)} catch(Exception ex) {}
            if(params.dateBeginTo) try {dateBeginTo = DateUtils.getDateFromString(params.dateBeginTo)} catch(Exception ex) {}
            if(params.eventvsType) try {eventvsType = EventVS.Type.valueOf(params.eventvsType)} catch(Exception ex) {}
            def criteria
            switch(eventvsType) {
                case EventVS.Type.MANIFEST:
                    criteria = EventVSManifest.createCriteria();
                    break;
                case EventVS.Type.CLAIM:
                    criteria = EventVSClaim.createCriteria();
                    break;
                case EventVS.Type.ELECTION:
                    criteria = EventVSElection.createCriteria();
                    break;
            }

            eventvsList = criteria.list(max: params.max, offset: params.offset, sort:params.sort, order:params.order) {
                or {
                    ilike('subject', "%${params.searchText}%")
                    ilike('content', "%${params.searchText}%")
                }
                and {
                    if(dateBeginFrom && dateBeginTo) {between("dateBegin", dateBeginFrom, dateBeginTo)}
                    else if(dateBeginFrom) {ge("dateBegin", dateBeginFrom)}
                    else if(dateBeginTo) {le("dateBegin", dateBeginTo)}
                }
                if(eventVSState == EventVS.State.TERMINATED) {
                    or{
                        eq("state", EventVS.State.TERMINATED)
                        eq("state", EventVS.State.CANCELLED)
                    }
                } else if(eventVSState) {
                    eq("state", eventVSState)
                } else {
                    or{
                        eq("state", EventVS.State.ACTIVE)
                        eq("state", EventVS.State.AWAITING)
                        eq("state", EventVS.State.TERMINATED)
                        eq("state", EventVS.State.CANCELLED)
                    }
                }
            }
            totalEventvs = eventvsList.totalCount
            eventvsList.each {eventvsItem ->
                switch(eventvsType) {
                    case EventVS.Type.MANIFEST:
                        resultList.add(eventVSService.getEventVSMap(eventvsItem))
                        break;
                    case EventVS.Type.CLAIM:
                        resultList.add(eventVSService.getEventVSClaimMap(eventvsItem))
                        break;
                    case EventVS.Type.ELECTION:
                        resultList.add(eventVSService.getEventVSElectionMap(eventvsItem))
                        break;
                }
            }
        }
        def resultMap = [eventVS:resultList, totalEventVS: totalEventvs]
        render resultMap as JSON
    }

	/**
	 * @httpMethod [POST]
	 * @requestContentType [application/json] documento JSON con los parámetros de la consulta:<br/><code>
	 * 		  {conReclamaciones:true, conVotaciones:true, textQuery:ipsum, conManifiestos:true}</code>
	 * @responseContentType [application/json]
	 * @return documento JSON con la lista de eventos que cumplen el criterio de la búsqueda.
	 */
	def find() {
		String requestStr = "${request.getInputStream()}"
		log.debug("requestStr: ${requestStr} - offset:${params.offset} - max: ${params.max}")
		if (!requestStr) {
            return [responseVS : new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                    args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))]
		}
		def messageJSON = JSON.parse(requestStr)
		SubSystemVS targetSubsystem = SubSystemVS.valueOf(messageJSON.subsystem)
		Class<?> entityClass
		if(targetSubsystem == SubSystemVS.VOTES) entityClass = EventVSElection.class
		if(targetSubsystem == SubSystemVS.MANIFESTS) entityClass = EventVSManifest.class
		if(targetSubsystem == SubSystemVS.CLAIMS) entityClass = EventVSClaim.class
		if(!entityClass) {
            return [responseVS : new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                    args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))]
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
        if(messageJSON.dateBeginFrom)
            dateBeginFrom = DateUtils.getDateFromString(messageJSON.dateBeginFrom)
        if(messageJSON.dateBeginTo)
            dateBeginTo = DateUtils.getDateFromString(messageJSON.dateBeginTo)
        if(messageJSON.dateFinishFrom)
            dateFinishFrom = DateUtils.getDateFromString(messageJSON.dateFinishFrom)
        if(messageJSON.dateFinishTo)
            dateFinishTo = DateUtils.getDateFromString(messageJSON.dateFinishTo)
        if(messageJSON.eventState &&  !messageJSON.eventState.trim().isEmpty()) {
            eventVSStates = new ArrayList<EventVS.State>();
            EventVS.State eventVSState = EventVS.State.valueOf(messageJSON.eventState)
            eventVSStates.add(eventVSState);
            if(EventVS.State.TERMINATED == eventVSState) eventVSStates.add(EventVS.State.CANCELLED)
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
		render eventsVSMap as JSON
	}
	
	/**
	 * Servicio que busca los eventos que tienen la etiqueta que se pasa como parámetro.
	 * @param tag Obligatorio. Texto de la etiqueta.
	 * @param max Opcional (por defecto 20). Número máximo de documentos que devuelve la consulta (tamaño de la página).
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 */
	def eventvsByTag () {
		if (!params.tag) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR, message(code: 'searchMissingParamTag'))]
		} else {
            def result
            EventVS.withTransaction {
                result = EventVS.createCriteria().listDistinct() {
                    tagVSSet {
                        eq('name', params.tag)
                    }
                }
            }
            def eventVSList = []
            result.each {eventVS ->
                eventVSList.add([id: eventVS.id,URL:"${grailsApplication.config.grails.serverURL}/eventVS/${eventVS.id}",
                                 subject:eventVS?.subject, content:eventVS?.content])
            }
            def resultMap = [eventsVS:eventVSList]
            render resultMap as JSON
        }
	}

}
