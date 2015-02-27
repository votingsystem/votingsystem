package org.votingsystem.controlcenter.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal 
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class AppController {

	def grailsApplication
	
	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de votación con parámetros de utilidad
	 * 		   para una sesión con cliente Android.
	 */
	def androidClient() {
		log.debug("*** Si llega aqui mostrar msg app market browserToken: ${params.browserToken}" )
		if(params.boolean('androidClientLoaded'))
			render(view:"index")
		String uri = "${grailsApplication.config.grails.serverURL}/app/home?androidClientLoaded=false"
		if(params.browserToken) uri = "${uri}#${params.browserToken}"
		if(params.eventId) uri = "${uri}&eventId=${params.eventId}"
		if(params.serverURL) uri = "${uri}&serverURL=${params.serverURL}"
		if(params.msg) {
			String msg = URLEncoder.encode(params.msg, "UTF-8")
			uri = "${uri}&msg=${msg}"
			log.debug("msg: ${msg}")
		} 
		redirect(uri:uri)
		return
	}

    def contact() {}

    def admin() {}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
