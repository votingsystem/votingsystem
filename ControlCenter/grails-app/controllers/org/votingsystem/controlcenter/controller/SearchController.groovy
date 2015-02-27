package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.DateUtils

/**
 * @infoController Búsquedas
 * @descController Servicios de búsqueda sobre los datos generados por la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class SearchController {

    def subscriptionVSService
    def sessionFactory
    def userVSService
    def eventVSElectionService

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
     * Servicio que busca entre las votaciones publicadas una cadena de texto en el rango de fechas solicitado.
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
    def eventVSElection () {
        List<EventVSElection> eventvsList = null
        def resultList = []
        EventVSElection.withTransaction {
            Date dateBeginFrom = null
            Date dateBeginTo = null
            //searchFrom:2014/04/14 00:00:00, max:100, searchTo
            if(params.dateBeginFrom) try {dateBeginFrom = DateUtils.getDateFromString(params.dateBeginFrom)} catch(Exception ex) {}
            if(params.dateBeginTo) try {dateBeginTo = DateUtils.getDateFromString(params.dateBeginTo)} catch(Exception ex) {}

            eventvsList = EventVSElection.createCriteria().list(max: params.max, offset: params.offset,
                    sort:params.sort, order:params.order) {
                or {
                    ilike('subject', "%${params.searchText}%")
                    ilike('content', "%${params.searchText}%")
                }
                and {
                    if(dateBeginFrom && dateBeginTo) {between("dateBegin", dateBeginFrom, dateBeginTo)}
                    else if(dateBeginFrom) {ge("dateBegin", dateBeginFrom)}
                    else if(dateBeginTo) {le("dateBegin", dateBeginTo)}
                }
                or {
                    eq("state", EventVS.State.ACTIVE)
                    eq("state", EventVS.State.PENDING)
                    eq("state", EventVS.State.CANCELLED)
                    eq("state", EventVS.State.TERMINATED)
                }
            }
            eventvsList.each {eventvsItem ->
                resultList.add(eventVSElectionService.getEventVSElectionMap(eventvsItem))
            }
        }
        def resultMap = [eventsVSElection:resultList, max: params.max, offset:params.offset,
                 totalCount:eventvsList.totalCount]
        render resultMap as JSON
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
