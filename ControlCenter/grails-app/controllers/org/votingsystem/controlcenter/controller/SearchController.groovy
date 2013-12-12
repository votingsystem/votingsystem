package org.votingsystem.controlcenter.controller

import org.apache.lucene.search.SortField
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVS
import org.hibernate.search.FullTextQuery
import org.hibernate.search.FullTextSession
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubSystemVS
import org.votingsystem.model.TagVS
import org.votingsystem.model.TagVSEventVS;
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.search.SearchHelper;

import grails.converters.JSON
import org.hibernate.search.Search
import org.apache.lucene.search.Sort
/**
 * @infoController Búsquedas
 * @descController Servicios de búsqueda sobre los datos generados por la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class SearchController {

   SearchHelper searchHelper;
   def subscriptionVSService
   def sessionFactory
   def userVSService
   
   /**
    * ==================================================
	* (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS). 
	* ==================================================
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
	* @requestContentType [application/x-pkcs7-signature] Obligatorio. Documento firmado 
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
	 * Servicio que busca la cadena de texto recibida entre las votaciones publicadas.
	 *
	 * @httpMethod [GET]
	 * @param [consultaTexto] Opcional. Texto de la búsqueda.
	 * @param [max] Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @responseContentType [application/json]
	 */
    def eventVS () {
        def eventVSElectionMap = new HashMap()
		if (params.consultaTexto) {
			List<EventVS> eventsVS = searchHelper.findByFullText(EventVS.class,
				['subject', 'content']  as String[], params.consultaTexto, params.offset, params.max);
			log.debug("eventsVS votacion: ${eventsVS.size()}")
			eventVSElectionMap.eventsVSElection = eventsVS.collect {evento ->
					return [id: evento.id, subject: evento.subject, content:evento.content,
						URL:"${grailsApplication.config.grails.serverURL}/eventVS/${evento.id}"]
			}
        }
        render eventVSElectionMap as JSON
    }
	
	/**
	 * @httpMethod [POST]
	 * @requestContentType [application/json] Documento JSON con los parámetros de la consulta:<br/><code>
	 * 		  {conReclamaciones:true, conVotaciones:true, textQuery:ipsum, conManifiestos:true}</code>
	 * @responseContentType [application/json]
	 * @return Documento JSON con la lista de eventsVS que cumplen el criterio de la búsqueda.
	 */
	def find() {
		String requestStr = "${request.getInputStream()}"
		log.debug("requestStr: ${requestStr}")
		if (!requestStr) {
            return [responseVS = new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                    args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))]
		}
		def messageJSON = JSON.parse(requestStr)
		def eventsVSMap = new HashMap()
		eventsVSMap.eventsVS = new HashMap()
		eventsVSMap.eventsVS.elections = []
		int numEventVSInRequest = 0;
		int numEventsVSElectionInSystem = 0;
		List<EventVS> eventsVSVotacion
		if (SubSystemVS.VOTES == SubSystemVS.valueOf(messageJSON.subsystem)) {
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
            if(messageJSON.eventState &&  !"".equals(messageJSON.eventState.trim())) {
                eventVSStates = new ArrayList<EventVS.State>();
                EventVS.State eventVSState = EventVS.State.valueOf(messageJSON.eventState)
                eventVSStates.add(eventVSState);
                if(EventVS.State.TERMINATED == eventVSState) eventVSStates.add(EventVS.State.CANCELLED)
            }
			FullTextQuery fullTextQuery =  searchHelper.getCombinedQuery(EventVS.class,
				['subject', 'content']  as String[], messageJSON.textQuery?.toString(),
				dateBeginFrom, dateBeginTo, dateFinishFrom, dateFinishTo, eventVSStates)
			if(fullTextQuery) {
				numEventsVSElectionInSystem = fullTextQuery?.getResultSize()
				EventVS.withTransaction {
					fullTextQuery.setSort(new Sort(new SortField("id", SortField.LONG)));
					eventsVSVotacion = fullTextQuery.setFirstResult(params.int('offset')).
						setMaxResults(params.int('max')).list();
					if(eventsVSVotacion) numEventVSInRequest = eventsVSVotacion.size()
				}
			}
		}
		eventsVSMap.numEventVSInRequest = numEventVSInRequest
		eventsVSMap.numEventsVSElection = numEventVSInRequest
		eventsVSMap.numEventsVSElectionInSystem = numEventsVSElectionInSystem
		eventsVSMap.offset = params.int('offset')
		eventsVSVotacion.each {eventoItem ->
			def eventoItemId = eventoItem.id
			def eventoMap = [id: eventoItem.id, dateCreated: eventoItem.dateCreated,
					URL:"${grailsApplication.config.grails.serverURL}/evento/${eventoItem.id}",
					subject:eventoItem.subject, content:eventoItem.content,
					tags:eventoItem.tagVSSet?.collect {tagItem ->
							return [id:tagItem.id, content:tagItem.name]},
					duracion:DateUtils.getElapsedTime(eventoItem.getDateBegin(),
					eventoItem.getDateFinish()),
					state:eventoItem.estado.toString(),
					dateBegin:eventoItem.getDateBegin(),
					dateFinish:eventoItem.getDateFinish()]
			if (eventoItem.userVS)
				eventoMap.userVS = "${eventoItem.userVS?.name} ${eventoItem.userVS?.firstName}"
			def accessControlMap = [serverURL:eventoItem.accessControl.serverURL,
							name:eventoItem.accessControl.name]
			eventoMap.accessControl = accessControlMap
			eventoMap.fieldsEventVS = eventoItem.fieldsEventVS?.collect {opcion ->
							return [id:opcion.id, content:opcion.content]}
			def controlCenterMap = [serverURL:grailsApplication.config.grails.serverURL,
					name:grailsApplication.config.VotingSystem.serverName]
			controlCenterMap.voteVSInfoURL = "${grailsApplication.config.grails.serverURL}/eventVS/votes?eventAccessControlURL=${eventoItem.url}"
			eventoMap.controlCenter = controlCenterMap
			eventoMap.url = eventoItem.url
			eventsVSMap.eventsVS.elections.add(eventoMap)
		}
		render eventsVSMap as JSON
	}
	
	/**
	 * Servicio que busca los eventos que tienen la tagVS que se
	 * pasa como parámetro.
	 * @param tagVS Obligatorio. Texto de la tagVS.
	 * @param max Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @responseContentType [application/json]
	 * @httpMethod [GET]
	 */
	def eventvsByTag () {
        def eventsVSMap = new HashMap()
        if (!params.tag) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR, message(code: 'searchMissingParamTag'))]
        } else {
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

}
