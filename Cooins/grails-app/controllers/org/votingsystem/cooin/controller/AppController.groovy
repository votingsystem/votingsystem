package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.DateUtils

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class AppController {

	def grailsApplication;
    def transactionVSService
    def dashBoardService

    def index() {}

	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de votación con parámetros de utilidad
	 * 		   para una sesión con cliente Android.
	 */
	def androidClient() {
        log.debug("*** Si llega aqui mostrar message app market browserToken: ${params.browserToken}" )
        if(params.boolean('androidClientLoaded')) render(view:"index")
        else {
            String uri = "${params.refererURL}"
            if(!params?.refererURL?.contains("androidClientLoaded=false")) uri = "$uri?androidClientLoaded=false"
            log.debug("uri: ${uri}")
            redirect(uri:uri)
            return
        }
	}

    def tools() {}

    def accounts() {}

    def admin() {}

    def transactions() {}

    def messagevs() {}

    def userVS() {
        Integer numHours = params.int("numHours") ? -params.int("numHours"):-1; //default to 1 hour
        DateUtils.TimePeriod timePeriod = DateUtils.addHours(Calendar.getInstance(), numHours)
        def result = [transactionVSData:dashBoardService.getUserVSInfo(timePeriod)]
        if(request.contentType?.contains("json")) render result as JSON
        else render(view:'user', model:[dataMap:(result as JSON)])
    }

    def contact() {}

    def jsonDocs() {}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}