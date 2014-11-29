package org.votingsystem.cooin.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS

/**
 * @infoController Cuentas IBAN
 * @descController Servicios relacionados con cuentas IBAN
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class AccountController {

	
	/**
	 * @httpMethod [GET]
	 * @return
	 */
	def index() { }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
	
}