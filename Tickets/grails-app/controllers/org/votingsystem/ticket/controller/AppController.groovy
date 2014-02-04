package org.votingsystem.ticket.controller
/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class AppController {

	def grailsApplication;
	
	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de votación con parámetros de utilidad
	 * 		   para una sesión con cliente Android.
	 */
	def androidClient() {
		/*if(params.boolean('androidClientLoaded')) render(view:"index")
		else {
            String uri = "${grailsApplication.config.grails.serverURL}?androidClientLoaded=false"
            if(params.browserToken) uri = "${uri}#${params.browserToken}"
            if(params.eventId) uri = "${uri}&eventId=${params.eventId}"
            if(params.serverURL) uri = "${uri}&serverURL=${params.serverURL}"
            if(params.msg) {
                String msg = URLEncoder.encode("${params.msg}", "UTF-8")
                uri = "${uri}&msg=${msg}"
                log.debug("msg: ${msg}")
            }
        }*/
	}
	
}