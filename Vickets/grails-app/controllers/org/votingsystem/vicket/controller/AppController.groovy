package org.votingsystem.vicket.controller

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

	def grailsApplication;

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

    def admin() {}

    def user() {}

    def contact() {}

    def jsonDocs() {}

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }
}