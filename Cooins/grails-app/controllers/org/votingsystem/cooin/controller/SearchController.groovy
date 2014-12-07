package org.votingsystem.cooin.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ResponseVS

/**
 * @infoController Búsquedas
 * @descController Servicios de búsqueda sobre los datos generados por la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class SearchController {

    def grailsApplication
    def transactionVSService

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
