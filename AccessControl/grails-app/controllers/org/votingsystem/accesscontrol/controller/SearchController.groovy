package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*
import org.votingsystem.util.DateUtils

/**
 * @infoController Búsquedas mediante
 * @descController Servicios de búsqueda sobre los datos generados por la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class SearchController {

   def sessionFactory
   def eventVSService
   def grailsApplication
   def userVSService
   def representativeService

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
                        eq("state", EventVS.State.PENDING)
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

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
